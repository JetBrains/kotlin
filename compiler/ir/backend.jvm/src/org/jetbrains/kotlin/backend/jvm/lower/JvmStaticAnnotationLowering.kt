/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlock
import org.jetbrains.kotlin.backend.common.runOnFilePostfix
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.descriptors.JvmFunctionDescriptorImpl
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.annotations.JVM_STATIC_ANNOTATION_FQ_NAME
import org.jetbrains.org.objectweb.asm.Opcodes

/*
 * For @JvmStatic functions within companion objects of classes, we synthesize proxy static functions that redirect
 * to the actual implementation.
 * For @JvmStatic functions within static objects, we make the actual function static and modify all call sites.
 */
class JvmStaticAnnotationLowering(val context: JvmBackendContext) : IrElementTransformerVoid(), FileLoweringPass {
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
            if (AsmUtil.getVisibilityAccessFlag(jvmStaticFunction.descriptor) != Opcodes.ACC_PUBLIC) {
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
        target: IrFunction,
        irClass: IrClass,
        companion: IrClass,
        name: Name,
        visibility: Visibility,
        isSynthetic: Boolean
    ): IrFunction {
        val proxyFunctionSymbol = makeJvmStaticFunctionSymbol(irClass, target.symbol, name, visibility, isSynthetic)

        val proxyIrFunction = IrFunctionImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            JvmLoweredDeclarationOrigin.JVM_STATIC_WRAPPER,
            proxyFunctionSymbol
        )
        proxyIrFunction.returnType = target.returnType
        proxyIrFunction.createParameterDeclarations()

        proxyIrFunction.body = createProxyBody(target, proxyIrFunction, companion)
        target.annotations.mapTo(proxyIrFunction.annotations) { it.deepCopyWithSymbols() }
        return proxyIrFunction
    }

    private fun createProxyBody(target: IrFunction, proxy: IrFunction, companion: IrClass): IrBody {
        val companionInstanceFieldSymbol = context.descriptorsFactory.getSymbolForObjectInstance(companion.symbol)
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
            val oldDispatchReceiverParemeter = jvmStaticFunction.dispatchReceiverParameter!!
            jvmStaticFunction.dispatchReceiverParameter = null
            modifyBody(jvmStaticFunction, irClass, oldDispatchReceiverParemeter)
            functionsMadeStatic.add(jvmStaticFunction.symbol)
        }
    }

    fun modifyBody(irFunction: IrFunction, irClass: IrClass, oldDispatchReceiverParameter: IrValueParameter) {
        irFunction.body = irFunction.body?.transform(ReplaceThisByStaticReference(context, irClass, oldDispatchReceiverParameter), null)
    }
}

private class ReplaceThisByStaticReference(
    val context: JvmBackendContext,
    val irClass: IrClass,
    val oldThisReceiverParameter: IrValueParameter
) : IrElementTransformer<Nothing?> {
    override fun visitGetValue(expression: IrGetValue, data: Nothing?): IrExpression {
        val irGetValue = expression
        if (irGetValue.symbol == oldThisReceiverParameter.symbol) {
            val instanceSymbol = context.descriptorsFactory.getSymbolForObjectInstance(irClass.symbol)
            return IrGetFieldImpl(
                expression.startOffset,
                expression.endOffset,
                instanceSymbol,
                irClass.defaultType
            )
        }
        return super.visitGetValue(irGetValue, data)
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

private fun makeJvmStaticFunctionSymbol(
    ownerClass: IrClass,
    oldFunctionSymbol: IrFunctionSymbol,
    newName: Name,
    visibility: Visibility,
    isSynthetic: Boolean
): IrSimpleFunctionSymbol {
    val proxyDescriptorForIrFunction = JvmFunctionDescriptorImpl(
        ownerClass.descriptor,
        null,
        oldFunctionSymbol.descriptor.annotations,
        newName,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        oldFunctionSymbol.descriptor.source,
        extraFlags = if (isSynthetic) Opcodes.ACC_SYNTHETIC else 0
    )

    proxyDescriptorForIrFunction.initialize(
        oldFunctionSymbol.descriptor.extensionReceiverParameter?.copy(proxyDescriptorForIrFunction),
        null,
        oldFunctionSymbol.descriptor.typeParameters,
        oldFunctionSymbol.descriptor.valueParameters.map { it.copy(proxyDescriptorForIrFunction, it.name, it.index) },
        oldFunctionSymbol.descriptor.returnType,
        // FINAL on static interface members makes JVM unhappy, so remove it.
        if (ownerClass.isInterface) Modality.OPEN else oldFunctionSymbol.descriptor.modality,
        visibility
    )

    return IrSimpleFunctionSymbolImpl(proxyDescriptorForIrFunction)
}
