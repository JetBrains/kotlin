/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.phaser.makeIrModulePhase
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

internal val jvmStaticInObjectPhase = makeIrModulePhase(
    ::JvmStaticInObjectLowering,
    name = "JvmStaticInObject",
    description = "Make JvmStatic functions in non-companion objects static and replace all call sites in the module"
)

internal val jvmStaticInCompanionPhase = makeIrFilePhase(
    ::JvmStaticInCompanionLowering,
    name = "JvmStaticInCompanion",
    description = "Synthesize static proxy functions for JvmStatic functions in companion objects"
)

private class JvmStaticInObjectLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        SingletonObjectJvmStaticLowering(context).runOnFilePostfix(irFile)
        irFile.transformChildrenVoid(MakeCallsStatic(context))
    }
}

private class JvmStaticInCompanionLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val companion = irClass.companionObject() ?: return

        companion.declarations
            // In case of companion objects, proxy functions for '$default' methods for @JvmStatic functions with default parameters
            // are not created in the host class.
            .filter {
                it.isJvmStaticDeclaration() &&
                        it.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER &&
                        it.origin != JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_OR_TYPEALIAS_ANNOTATIONS
            }
            .forEach { declaration ->
                val jvmStaticFunction = declaration as IrSimpleFunction
                if (jvmStaticFunction.isExternal) {
                    // We move external functions to the enclosing class and potentially add accessors there.
                    // The JVM backend also adds accessors in the companion object, but these are superfluous.
                    val staticExternal = irClass.addFunction {
                        updateFrom(jvmStaticFunction)
                        name = jvmStaticFunction.name
                        returnType = jvmStaticFunction.returnType
                    }.apply {
                        copyTypeParametersFrom(jvmStaticFunction)
                        copyAnnotationsFrom(jvmStaticFunction)
                        extensionReceiverParameter = jvmStaticFunction.extensionReceiverParameter?.copyTo(this)
                        valueParameters = jvmStaticFunction.valueParameters.map { it.copyTo(this) }
                    }
                    companion.declarations.remove(jvmStaticFunction)
                    companion.addProxy(staticExternal, companion, isStatic = false)
                } else {
                    irClass.addProxy(jvmStaticFunction, companion)
                }
            }
    }

    private fun IrClass.addProxy(target: IrSimpleFunction, companion: IrClass, isStatic: Boolean = true) =
        addFunction {
            returnType = target.returnType
            origin = JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER
            // The proxy needs to have the same name as what it is targeting. If that is a property accessor,
            // we need to make sure that the name is mapped correctly. The static method is not a property accessor,
            // so we do not have a property to link it up to. Therefore, we compute the right name now.
            name = Name.identifier(context.methodSignatureMapper.mapFunctionName(target))
            modality = if (isInterface) Modality.OPEN else target.modality
            // Since we already mangle the name above we need to reset internal visibilities to public in order
            // to avoid mangling the same name twice.
            visibility = if (target.visibility == DescriptorVisibilities.INTERNAL) DescriptorVisibilities.PUBLIC else target.visibility
            isSuspend = target.isSuspend
        }.apply {
            copyTypeParametersFrom(target)
            copyAnnotationsFrom(target)
            if (!isStatic) {
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)
            }
            extensionReceiverParameter = target.extensionReceiverParameter?.copyTo(this)
            valueParameters = target.valueParameters.map { it.copyTo(this) }

            val proxy = this
            val companionInstanceField = context.cachedDeclarations.getFieldForObjectInstance(companion)
            body = context.createIrBuilder(symbol).run {
                irExprBody(irCall(target).apply {
                    passTypeArgumentsFrom(proxy)
                    if (target.dispatchReceiverParameter != null) {
                        dispatchReceiver = irGetField(null, companionInstanceField)
                    }
                    extensionReceiverParameter?.let { extensionReceiver = irGet(it) }
                    for ((i, valueParameter) in valueParameters.withIndex()) {
                        putValueArgument(i, irGet(valueParameter))
                    }
                })
            }
        }
}

private class SingletonObjectJvmStaticLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isNonCompanionObject) return

        for (function in irClass.simpleFunctions()) {
            if (function.isJvmStaticDeclaration()) {
                // dispatch receiver parameter is already null for synthetic property annotation methods
                function.dispatchReceiverParameter?.let { oldDispatchReceiverParameter ->
                    function.dispatchReceiverParameter = null
                    function.replaceThisByStaticReference(context.cachedDeclarations, irClass, oldDispatchReceiverParameter)
                }
            }
        }
    }
}

internal fun IrDeclaration.isJvmStaticInObject(): Boolean =
    isJvmStaticDeclaration() && (parent as? IrClass)?.isNonCompanionObject == true

private class MakeCallsStatic(val context: JvmBackendContext) : IrElementTransformerVoid() {
    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
        val callee = expression.symbol.owner
        if (callee is IrDeclaration && callee.isJvmStaticInObject() && expression.dispatchReceiver != null) {
            return context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).irBlock(expression) {
                // OldReceiver has to be evaluated for its side effects.
                val oldReceiver = super.visitExpression(expression.dispatchReceiver!!)
                // `coerceToUnit()` is private in InsertImplicitCasts, have to reproduce it here
                +IrTypeOperatorCallImpl(
                    oldReceiver.startOffset, oldReceiver.endOffset,
                    context.irBuiltIns.unitType,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                    context.irBuiltIns.unitType,
                    oldReceiver
                )
                expression.dispatchReceiver = null
                +super.visitMemberAccess(expression)
            }
        }
        return super.visitMemberAccess(expression)
    }
}

private fun IrDeclaration.isJvmStaticDeclaration(): Boolean =
    hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) ||
            (this as? IrSimpleFunction)?.correspondingPropertySymbol?.owner?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true ||
            (this as? IrProperty)?.getter?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true
