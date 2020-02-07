/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.ir.*
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isInCurrentModule
import org.jetbrains.kotlin.backend.jvm.ir.replaceThisByStaticReference
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildFunWithDescriptorForInlining
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin

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
        } as IrClass?

        companion?.declarations?.filter(::isJvmStaticFunction)?.forEach { declaration ->
            val jvmStaticFunction = declaration as IrSimpleFunction
            val proxy = createProxy(jvmStaticFunction, irClass, companion)
            irClass.addMember(proxy)
        }

    }

    private fun createProxy(target: IrSimpleFunction, irClass: IrClass, companion: IrClass): IrSimpleFunction {
        return buildFun {
            updateFrom(target)
            returnType = target.returnType
            origin = JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER
            name = target.name
            modality = if (irClass.isInterface) Modality.OPEN else target.modality
            isInline = false
            isExternal = false
            isTailrec = false
            isExpect = false
        }.apply {
            parent = irClass
            copyTypeParametersFrom(target)
            target.extensionReceiverParameter?.let { extensionReceiverParameter = it.copyTo(this) }
            valueParameters = target.valueParameters.map { it.copyTo(this) }

            annotations = target.annotations.map { it.deepCopyWithSymbols() }

            body = createProxyBody(target, this, companion)
        }
    }

    private fun createProxyBody(target: IrFunction, proxy: IrFunction, companion: IrClass): IrBody {
        val companionInstanceField = context.declarationFactory.getFieldForObjectInstance(companion)
        val companionInstanceFieldSymbol = companionInstanceField.symbol
        val call = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol)

        call.passTypeArgumentsFrom(proxy)

        target.dispatchReceiverParameter?.let { _ ->
            call.dispatchReceiver = IrGetFieldImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                companionInstanceFieldSymbol,
                companion.defaultType
            )
        }
        proxy.extensionReceiverParameter?.let { extensionReceiver ->
            call.extensionReceiver = IrGetValueImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                extensionReceiver.symbol
            )
        }
        proxy.valueParameters.mapIndexed { i, valueParameter ->
            call.putValueArgument(
                i,
                IrGetValueImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    valueParameter.symbol
                )
            )
        }

        return IrExpressionBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, call)
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
                modifyBody(jvmStaticFunction, irClass, oldDispatchReceiverParameter)
            }
        }
    }

    fun modifyBody(irFunction: IrFunction, irClass: IrClass, oldDispatchReceiverParameter: IrValueParameter) {
        irFunction.body = irFunction.body?.replaceThisByStaticReference(context.declarationFactory, irClass, oldDispatchReceiverParameter)
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
        buildFunWithDescriptorForInlining(descriptor) {
            updateFrom(this@copyRemovingDispatchReceiver)
            name = this@copyRemovingDispatchReceiver.name
            returnType = this@copyRemovingDispatchReceiver.returnType
        }.also {
            it.parent = parent
            it.correspondingPropertySymbol = correspondingPropertySymbol
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

