/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.copyParameterDeclarationsFrom
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.copyCorrespondingPropertyFrom
import org.jetbrains.kotlin.backend.jvm.ir.isInCurrentModule
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME

internal val jvmStaticAnnotationPhase = makeIrFilePhase(
    ::JvmStaticAnnotationLowering,
    name = "JvmStaticAnnotation",
    description = "Handle JvmStatic annotations"
)

/*
 * For @JvmStatic functions within companion objects of classes, we synthesize proxy static functions that redirect
 * to the actual implementation.
 * For @JvmStatic functions within static objects, we make the actual function static and modify all call sites.
 */
private class JvmStaticAnnotationLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
    override fun lower(irFile: IrFile) {
        CompanionObjectJvmStaticLowering(context).runOnFilePostfix(irFile)
        SingletonObjectJvmStaticLowering(context).runOnFilePostfix(irFile)
        irFile.transformChildrenVoid(MakeCallsStatic(context))
    }
}

private class CompanionObjectJvmStaticLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val companion = irClass.declarations.find {
            it is IrClass && it.isCompanion
        } as? IrClass ?: return

        companion.declarations
            // In case of companion objects, proxy functions for '$default' methods for @JvmStatic functions with default parameters
            // are not created in the host class.
            .filter { isJvmStaticFunction(it) && it.origin != IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER }
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
                        extensionReceiverParameter = jvmStaticFunction.extensionReceiverParameter?.copyTo(this)
                        valueParameters = jvmStaticFunction.valueParameters.map { it.copyTo(this) }
                        annotations = jvmStaticFunction.annotations.map { it.deepCopyWithSymbols() }
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
            visibility = if (target.visibility == Visibilities.INTERNAL) Visibilities.PUBLIC else target.visibility
            isSuspend = target.isSuspend
        }.apply {
            copyTypeParametersFrom(target)
            if (!isStatic) {
                dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)
            }
            extensionReceiverParameter = target.extensionReceiverParameter?.copyTo(this)
            valueParameters = target.valueParameters.map { it.copyTo(this) }
            annotations = target.annotations.map { it.deepCopyWithSymbols() }

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

private class SingletonObjectJvmStaticLowering(
    val context: JvmBackendContext
) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isObject || irClass.isCompanion) return

        irClass.declarations.filter(::isJvmStaticFunction).forEach {
            val jvmStaticFunction = it as IrSimpleFunction
            // dispatch receiver parameter is already null for synthetic property annotation methods
            jvmStaticFunction.dispatchReceiverParameter?.let { oldDispatchReceiverParameter ->
                jvmStaticFunction.dispatchReceiverParameter = null
                jvmStaticFunction.body = jvmStaticFunction.body?.replaceThisByStaticReference(
                    context.cachedDeclarations,
                    irClass,
                    oldDispatchReceiverParameter
                )
            }
        }
    }

}

private fun IrFunction.isJvmStaticInSingleton(): Boolean {
    val parentClass = parent as? IrClass ?: return false
    return isJvmStaticFunction(this) && parentClass.isObject && !parentClass.isCompanion
}

private class MakeCallsStatic(
    val context: JvmBackendContext
) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        if (expression.symbol.owner.isJvmStaticInSingleton() && expression.dispatchReceiver != null) {
            // Imported functions do not have their receiver parameter nulled by SingletonObjectJvmStaticLowering,
            // so we have to do it here.
            // TODO: would be better handled by lowering imported declarations.
            val callee = expression.symbol.owner as IrSimpleFunction
            val newCallee = if (!callee.isInCurrentModule()) {
                callee.copyRemovingDispatchReceiver()       // TODO: cache these
            } else callee

            return context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).irBlock(expression) {
                // OldReceiver has to be evaluated for its side effects.
                val oldReceiver = super.visitExpression(expression.dispatchReceiver!!)
                // `coerceToUnit()` is private in InsertImplicitCasts, have to reproduce it here
                val oldReceiverVoid = IrTypeOperatorCallImpl(
                    oldReceiver.startOffset, oldReceiver.endOffset,
                    context.irBuiltIns.unitType,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                    context.irBuiltIns.unitType,
                    oldReceiver
                )

                +super.visitExpression(oldReceiverVoid)
                +super.visitCall(
                    irCall(expression, newFunction = newCallee).apply { dispatchReceiver = null }
                )
            }
        }
        return super.visitCall(expression)
    }

    private fun IrSimpleFunction.copyRemovingDispatchReceiver(): IrSimpleFunction =
        factory.buildFun(descriptor) {
            updateFrom(this@copyRemovingDispatchReceiver)
            name = this@copyRemovingDispatchReceiver.name
            returnType = this@copyRemovingDispatchReceiver.returnType
        }.also {
            it.parent = parent
            it.copyCorrespondingPropertyFrom(this)
            it.annotations += annotations
            it.copyParameterDeclarationsFrom(this)
            it.dispatchReceiverParameter = null
        }
}

private fun isJvmStaticFunction(declaration: IrDeclaration): Boolean =
    declaration is IrSimpleFunction &&
            (declaration.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) ||
                    declaration.correspondingPropertySymbol?.owner?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true) &&
            declaration.origin != JvmLoweredDeclarationOrigin.SYNTHETIC_METHOD_FOR_PROPERTY_ANNOTATIONS
