/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhaseDescription
import org.jetbrains.kotlin.backend.jvm.CachedFieldsForObjectInstances
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isEffectivelyInlineOnly
import org.jetbrains.kotlin.backend.jvm.ir.isInlineFunctionCall
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

/**
 * Makes `@JvmStatic` functions in non-companion objects static and replaces all call sites in the module.
 */
@PhaseDescription(name = "JvmStaticInObject")
internal class JvmStaticInObjectLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) =
        irFile.transformChildrenVoid(
            SingletonObjectJvmStaticTransformer(context.irBuiltIns, context.cachedDeclarations.fieldsForObjectInstances)
        )
}

/**
 * Synthesizes static proxy functions for `@JvmStatic` functions in companion objects.
 */
@PhaseDescription(name = "JvmStaticInCompanion")
internal class JvmStaticInCompanionLowering(val context: JvmBackendContext) : FileLoweringPass {
    override fun lower(irFile: IrFile) =
        irFile.transformChildrenVoid(CompanionObjectJvmStaticTransformer(context))
}

private fun IrDeclaration.isJvmStaticDeclaration(): Boolean =
    hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) ||
            (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true ||
            (this as? IrProperty)?.getter?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true

private fun IrDeclaration.isJvmStaticInCompanion(): Boolean =
    isJvmStaticDeclaration() && (parent as? IrClass)?.isCompanion == true

internal fun IrDeclaration.isJvmStaticInObject(): Boolean =
    isJvmStaticDeclaration() && (parent as? IrClass)?.isNonCompanionObject == true

// `coerceToUnit()` is private in InsertImplicitCasts, have to reproduce it here
private fun IrExpression.coerceToUnit(irBuiltIns: IrBuiltIns) =
    IrTypeOperatorCallImpl(startOffset, endOffset, irBuiltIns.unitType, IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, irBuiltIns.unitType, this)

private fun IrMemberAccessExpression<*>.makeStatic(irBuiltIns: IrBuiltIns, replaceCallee: IrSimpleFunction?): IrExpression {
    val receiver = dispatchReceiver ?: return this
    removeDispatchReceiver()
    if (replaceCallee != null) {
        (this as IrCall).symbol = replaceCallee.symbol
    }
    if (receiver.isTrivial()) {
        // Receiver has no side effects (aside from maybe class initialization) so discard it.
        return this
    }
    return IrBlockImpl(startOffset, endOffset, type).apply {
        statements += receiver.coerceToUnit(irBuiltIns) // evaluate for side effects
        statements += this@makeStatic
    }
}

class SingletonObjectJvmStaticTransformer(
    private val irBuiltIns: IrBuiltIns,
    private val cachedFields: CachedFieldsForObjectInstances
) : IrElementTransformerVoid() {
    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        transformFunction(declaration)
        return super.visitSimpleFunction(declaration)
    }

    private fun transformFunction(function: IrFunction) {
        if (function.isJvmStaticInObject()) {
            // dispatch receiver parameter is already null for synthetic property annotation methods
            function.dispatchReceiverParameter?.let { oldDispatchReceiverParameter ->
                function.dispatchReceiverParameter = null

                if (function !is IrLazyFunctionBase) {
                    function.replaceThisByStaticReference(cachedFields, function.parentAsClass, oldDispatchReceiverParameter)
                }
            }
        }
    }

    // This lowering runs before functions references are handled, and should transform them too.
    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
        expression.transformChildrenVoid(this)

        val callee = expression.symbol.owner
        if (callee is IrFunction) {
            transformFunction(callee)
        }
        if (callee is IrProperty) {
            callee.getter?.let {
                transformFunction(it)
            }
            callee.setter?.let {
                transformFunction(it)
            }
        }

        if (callee is IrDeclaration && callee.isJvmStaticInObject()) {
            return expression.makeStatic(irBuiltIns, replaceCallee = null)
        }
        return expression
    }
}

private class CompanionObjectJvmStaticTransformer(val context: JvmBackendContext) : IrElementTransformerVoid() {
    // TODO: would be nice to add a mode that *only* leaves static versions for all annotated methods, with nothing
    //  in companions - this would reduce the number of classes if the companion only has `@JvmStatic` declarations.
    private fun IrSimpleFunction.needsStaticProxy(): Boolean = when {
        // Case 1: `external` static methods are moved to the outer class. JNI code does not care about visibility.
        isExternal -> true
        // Case 2: `JvmStatic` is useless on inline-only methods because they're not visible to any Java code anyway.
        origin == JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS -> false
        isEffectivelyInlineOnly() -> false
        // Case 3: protected non-inline needs a static proxy in the parent to be callable from subclasses
        // of said parent in different packages even in pure Kotlin due to JVM visibility rules.
        visibility == DescriptorVisibilities.PROTECTED && !isInline -> true
        // Case 4: public or protected inline needs a static proxy if not synthetic to be callable
        // on the parent class from Java code (the original point of this annotation).
        else -> !origin.isSynthetic
    }

    override fun visitClass(declaration: IrClass): IrStatement =
        super.visitClass(declaration).also {
            declaration.companionObject()?.declarations?.transformInPlace {
                if (it is IrSimpleFunction && it.isJvmStaticDeclaration() && it.needsStaticProxy()) {
                    val (static, companionFun) = context.cachedDeclarations.getStaticAndCompanionDeclaration(it)
                    declaration.declarations.add(static)
                    companionFun
                } else it
            }
        }

    // By this point all callable references have already been lowered (except ones for signature-generating
    // intrinsics, which do not care about accessibility rules), so only calls remain.
    override fun visitCall(expression: IrCall): IrExpression {
        expression.transformChildrenVoid(this)
        val callee = expression.symbol.owner
        return when {
            shouldReplaceWithStaticCall(callee) -> {
                val (staticProxy, _) = context.cachedDeclarations.getStaticAndCompanionDeclaration(callee)
                expression.makeStatic(context.irBuiltIns, staticProxy)
            }
            callee.symbol == context.ir.symbols.indyLambdaMetafactoryIntrinsic -> {
                val implFunRef = expression.getValueArgument(1) as? IrFunctionReference
                    ?: throw AssertionError("'implMethodReference' is expected to be 'IrFunctionReference': ${expression.dump()}")
                val implFun = implFunRef.symbol.owner
                if (implFunRef.dispatchReceiver != null && implFun is IrSimpleFunction && shouldReplaceWithStaticCall(implFun)) {
                    val (staticProxy, _) = context.cachedDeclarations.getStaticAndCompanionDeclaration(implFun)
                    expression.putValueArgument(
                        1,
                        IrFunctionReferenceImpl(
                            implFunRef.startOffset, implFunRef.endOffset, implFunRef.type,
                            staticProxy.symbol,
                            staticProxy.typeParameters.size,
                            implFunRef.reflectionTarget, implFunRef.origin
                        )
                    )
                }
                expression
            }
            else ->
                expression
        }
    }

    private fun shouldReplaceWithStaticCall(callee: IrSimpleFunction) =
        callee.isJvmStaticInCompanion() &&
                callee.visibility == DescriptorVisibilities.PROTECTED &&
                !callee.isInlineFunctionCall(context)
}
