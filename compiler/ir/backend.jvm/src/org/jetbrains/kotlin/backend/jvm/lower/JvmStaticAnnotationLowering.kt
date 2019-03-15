/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.lower.replaceThisByStaticReference
import org.jetbrains.kotlin.backend.common.phaser.makeIrFilePhase
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
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

        val functionsMadeStatic =
            SingletonObjectJvmStaticLowering(context).apply { runOnFilePostfix(irFile) }.functionsMadeStatic
        irFile.transformChildrenVoid(MakeCallsStatic(context, functionsMadeStatic))
    }
}

private class CompanionObjectJvmStaticLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val companion = irClass.declarations.find {
            it is IrClass && it.isCompanion
        } as IrClass?

        companion?.declarations?.filter(::isJvmStaticFunction)?.forEach {
            val jvmStaticFunction = it as IrSimpleFunction
            val newName = Name.identifier(context.state.typeMapper.mapFunctionName(jvmStaticFunction.symbol.descriptor, null))
            if (!jvmStaticFunction.visibility.isPublicAPI) {
                // TODO: Synthetic accessor creation logic should be supported in SyntheticAccessorLowering in the future.
                val accessorName = Name.identifier("access\$$newName")
                val accessor = createProxy(
                    jvmStaticFunction, companion, companion, accessorName, Visibilities.PUBLIC,
                    isSynthetic = true
                )
                companion.addMember(accessor)
                val proxy = createProxy(
                    accessor, irClass, companion, newName, jvmStaticFunction.visibility, isSynthetic = false
                )
                irClass.addMember(proxy)
            } else {
                val proxy = createProxy(
                    jvmStaticFunction, irClass, companion, newName, jvmStaticFunction.visibility,
                    isSynthetic = false
                )
                irClass.addMember(proxy)
            }
        }

    }

    private fun createProxy(
        target: IrSimpleFunction,
        irClass: IrClass,
        companion: IrClass,
        name: Name,
        visibility: Visibility,
        isSynthetic: Boolean
    ): IrSimpleFunction {
        val origin =
            if (isSynthetic) JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER_SYNTHETIC else JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER
        val descriptor = WrappedSimpleFunctionDescriptor(target.descriptor.annotations)
        return IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            origin,
            IrSimpleFunctionSymbolImpl(descriptor),
            name,
            visibility,
            // FINAL on static interface members makes JVM unhappy, so remove it.
            if (irClass.isInterface) Modality.OPEN else target.modality,
            returnType = target.returnType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = target.isSuspend
        ).apply {
            descriptor.bind(this)
            parent = irClass
            copyTypeParametersFrom(target)
            target.extensionReceiverParameter?.let { extensionReceiverParameter = it.copyTo(this) }
            target.valueParameters.mapTo(valueParameters) { it.copyTo(this) }

            target.annotations.mapTo(annotations) { it.deepCopyWithSymbols() }

            body = createProxyBody(target, this, companion)
        }
    }

    private fun createProxyBody(target: IrFunction, proxy: IrFunction, companion: IrClass): IrBody {
        val companionInstanceField = context.declarationFactory.getFieldForObjectInstance(companion)
        val companionInstanceFieldSymbol = companionInstanceField.symbol
        val call = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target.returnType, target.symbol)

        call.dispatchReceiver = IrGetFieldImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            companionInstanceFieldSymbol,
            companion.defaultType
        )
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
    val functionsMadeStatic: MutableSet<IrFunctionSymbol> = mutableSetOf()

    override fun lower(irClass: IrClass) {
        if (!irClass.isObject || irClass.isCompanion) return

        irClass.declarations.filter(::isJvmStaticFunction).forEach {
            val jvmStaticFunction = it as IrSimpleFunction
            val oldDispatchReceiverParameter = jvmStaticFunction.dispatchReceiverParameter!!
            jvmStaticFunction.dispatchReceiverParameter = null
            modifyBody(jvmStaticFunction, irClass, oldDispatchReceiverParameter)
            functionsMadeStatic.add(jvmStaticFunction.symbol)
        }
    }

    fun modifyBody(irFunction: IrFunction, irClass: IrClass, oldDispatchReceiverParameter: IrValueParameter) {
        irFunction.body = irFunction.body?.replaceThisByStaticReference(context, irClass, oldDispatchReceiverParameter)
    }
}

private class MakeCallsStatic(
    val context: JvmBackendContext,
    val functionsMadeStatic: Set<IrFunctionSymbol>
) : IrElementTransformerVoid() {
    override fun visitCall(expression: IrCall): IrExpression {
        if (functionsMadeStatic.contains(expression.symbol)) {
            return context.createIrBuilder(expression.symbol, expression.startOffset, expression.endOffset).irBlock(expression) {
                // OldReceiver has to be evaluated for its side effects.
                val oldReceiver = super.visitExpression(expression.dispatchReceiver!!)
                // `coerceToUnit()` is private in InsertImplicitCasts, have to reproduce it here
                val oldReceiverVoid = IrTypeOperatorCallImpl(
                    oldReceiver.startOffset, oldReceiver.endOffset,
                    context.irBuiltIns.unitType,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT,
                    context.irBuiltIns.unitType, context.irBuiltIns.unitType.classifierOrFail,
                    oldReceiver
                )

                +super.visitExpression(oldReceiverVoid)
                expression.dispatchReceiver = null
                +super.visitCall(expression)
            }
        }
        return super.visitCall(expression)
    }
}

private fun isJvmStaticFunction(declaration: IrDeclaration): Boolean =
    declaration is IrSimpleFunction &&
            (declaration.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) ||
                    declaration.correspondingProperty?.hasAnnotation(JVM_STATIC_ANNOTATION_FQ_NAME) == true)
