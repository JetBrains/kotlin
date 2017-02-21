package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.KotlinType

internal class DirectBridgesCallsLowering(val context: Context) : BodyLoweringPass {
    override fun lower(irBody: IrBody) {
        irBody.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                expression.transformChildrenVoid(this)
                val descriptor = expression.descriptor as? FunctionDescriptor ?: return expression
                if (descriptor.modality == Modality.ABSTRACT
                        || (expression.superQualifier == null && descriptor.isOverridable)) {
                    // A virtual call. box/unbox will be in the corresponding bridge.
                    return expression
                }

                val target = descriptor.target
                if (descriptor.kind != CallableMemberDescriptor.Kind.DELEGATION && !descriptor.original.needBridgeTo(target))
                    return expression

                return IrCallImpl(expression.startOffset, expression.endOffset,
                        target, remapTypeArguments(expression, target)).apply {
                    dispatchReceiver = expression.dispatchReceiver
                    extensionReceiver = expression.extensionReceiver
                    mapValueParameters { expression.getValueArgument(it)!! }
                }
            }

            private fun remapTypeArguments(oldExpression: IrMemberAccessExpression, newCallee: CallableDescriptor)
                    : Map<TypeParameterDescriptor, KotlinType>? {
                val oldCallee = oldExpression.descriptor

                return if (oldCallee.typeParameters.isEmpty())
                    null
                else oldCallee.typeParameters.associateBy(
                        { newCallee.typeParameters[it.index] },
                        { oldExpression.getTypeArgument(it)!! }
                )
            }
        })
    }
}

internal class BridgesBuilding(val context: Context) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        val functions = mutableSetOf<FunctionDescriptor?>()
        irClass.declarations.forEach {
            when (it) {
                is IrFunction -> functions.add(it.descriptor)
                is IrProperty -> {
                    functions.add(it.getter?.descriptor)
                    functions.add(it.setter?.descriptor)
                }
            }
        }

        irClass.descriptor.contributedMethods.forEach { functions.add(it) }

        functions.forEach {
            it?.let { function ->
                function.allOverriddenDescriptors
                        .map { OverriddenFunctionDescriptor(function, it) }
                        .filter { it.needBridge }
                        .filter { it.canBeCalledVirtually }
                        .distinctBy { it.bridgeDirections }
                        .forEach { buildBridge(it, irClass)
                }
            }
        }
    }

    private object DECLARATION_ORIGIN_BRIDGE_METHOD :
            IrDeclarationOriginImpl("BRIDGE_METHOD")

    private fun buildBridge(descriptor: OverriddenFunctionDescriptor, irClass: IrClass) {

        println("BUILD_BRIDGE fun: ${descriptor.descriptor}")
        println("BUILD_BRIDGE kind: ${descriptor.descriptor.kind}")
        println("BUILD_BRIDGE target: ${descriptor.descriptor.target}")
        println("BUILD_BRIDGE overridden: ${descriptor.overriddenDescriptor}")
        println()

        val bridgeDescriptor = context.specialDescriptorsFactory.getBridgeDescriptor(descriptor)
        val target = descriptor.descriptor.target

        val delegatingCall = IrCallImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, target,
                superQualifier = target.containingDeclaration as ClassDescriptor /* Call non-virtually */).apply {
            val dispatchReceiverParameter = bridgeDescriptor.dispatchReceiverParameter
            if (dispatchReceiverParameter != null)
                dispatchReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, dispatchReceiverParameter)
            val extensionReceiverParameter = bridgeDescriptor.extensionReceiverParameter
            if (extensionReceiverParameter != null)
                extensionReceiver = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, extensionReceiverParameter)
            bridgeDescriptor.valueParameters.forEach {
                this.putValueArgument(it.index, IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, it))
            }
        }

        val bridgeBody = if (bridgeDescriptor.returnType.let { it != null && !KotlinBuiltIns.isUnitOrNullableUnit(it) })
            IrReturnImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, bridgeDescriptor, delegatingCall)
        else
            delegatingCall
        irClass.declarations.add(IrFunctionImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, DECLARATION_ORIGIN_BRIDGE_METHOD,
                bridgeDescriptor, IrBlockBodyImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(bridgeBody))))
    }
}
