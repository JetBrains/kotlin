package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.IrBuildingTransformer
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.builders.IrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

/**
 * Lowers some interop intrinsic calls.
 */
internal class InteropLowering(val context: Context) : FileLoweringPass {
    override fun lower(irFile: IrFile) {
        val transformer = InteropTransformer(context)
        irFile.transformChildrenVoid(transformer)
    }
}

private class InteropTransformer(val context: Context) : IrBuildingTransformer(context) {

    val interop = context.interopBuiltIns

    private fun MemberScope.getSingleContributedFunction(name: String,
                                                         predicate: (SimpleFunctionDescriptor) -> Boolean) =
            this.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND).single(predicate)

    private fun IrBuilder.irGetObject(descriptor: ClassDescriptor): IrExpression {
        return IrGetObjectValueImpl(startOffset, endOffset, descriptor.defaultType, descriptor)
    }

    private fun IrBuilder.typeOf(descriptor: ClassDescriptor): IrExpression {
        val companionObject = descriptor.companionObjectDescriptor ?:
                error("native variable class $descriptor must have the companion object")
        // TODO: add more checks and produce the compile error instead of exception.

        return irGetObject(companionObject)
    }

    private fun IrBuilder.typeOf(type: KotlinType): IrExpression? {
        val descriptor = TypeUtils.getClassDescriptor(type) ?: return null
        return typeOf(descriptor)
    }

    private fun KotlinType.findOverride(property: PropertyDescriptor): PropertyDescriptor {
        val result = this.memberScope.getContributedVariables(property.name, NoLookupLocation.FROM_BACKEND).single()
        assert (OverridingUtil.overrides(result, property))
        return result
    }

    private fun IrBuilderWithScope.sizeOf(typeObject: IrExpression): IrExpression {
        val sizeProperty = typeObject.type.findOverride(interop.variableTypeSize)
        return irGet(typeObject, sizeProperty)
    }

    private fun IrBuilderWithScope.sizeOf(type: KotlinType): IrExpression? {
        val typeObject = typeOf(type) ?: return null
        return sizeOf(typeObject)
    }

    private fun IrBuilderWithScope.alignOf(typeObject: IrExpression): IrExpression {
        val alignProperty = typeObject.type.findOverride(interop.variableTypeAlign)
        return irGet(typeObject, alignProperty)
    }

    private fun IrBuilderWithScope.alignOf(type: KotlinType): IrExpression? {
        val typeObject = typeOf(type) ?: return null
        return alignOf(typeObject)
    }

    private fun IrBuilderWithScope.arrayGet(array: IrExpression, index: IrExpression): IrExpression? {
        val elementSize = sizeOf(array.type.arguments.single().type) ?: return null

        val offset = times(elementSize, index)

        return irCall(interop.memberAt).apply {
            extensionReceiver = array
            putValueArgument(0, offset)
        }
    }

    private fun IrBuilderWithScope.times(left: IrExpression, right: IrExpression): IrCall {
        val times = left.type.memberScope.getSingleContributedFunction("times") {
            right.type.isSubtypeOf(it.valueParameters.single().type)
        }

        return irCall(times).apply {
            dispatchReceiver = left
            putValueArgument(0, right)
        }
    }

    private fun IrBuilderWithScope.alloc(placement: IrExpression, size: IrExpression, align: IrExpression): IrExpression {
        val alloc = placement.type.memberScope.getSingleContributedFunction("alloc") {
            size.type.isSubtypeOf(it.valueParameters[0]!!.type) &&
                    align.type.isSubtypeOf(it.valueParameters[1]!!.type)
        }

        return irCall(alloc).apply {
            dispatchReceiver = placement
            putValueArgument(0, size)
            putValueArgument(1, align)
        }
    }

    private fun IrBuilderWithScope.allocArray(placement: IrExpression,
                                              elementType: KotlinType,
                                              length: IrExpression
    ): IrExpression? {

        val elementSize = sizeOf(elementType) ?: return null
        val size = times(elementSize, length)
        val align = alignOf(elementType) ?: return null

        return alloc(placement, size, align)
    }

    private fun IrBuilderWithScope.alloc(placement: IrExpression, type: KotlinType): IrExpression? {
        val size = sizeOf(type) ?: return null
        val align = alignOf(type) ?: return null

        return alloc(placement, size, align)
    }

    override fun visitCall(expression: IrCall): IrExpression {

        expression.transformChildrenVoid(this)
        builder.at(expression)
        val descriptor = expression.descriptor.original

        if (descriptor is ClassConstructorDescriptor) {
            val type = descriptor.constructedClass.defaultType
            if (type.isRepresentedAs(ValueType.C_POINTER) || type.isRepresentedAs(ValueType.NATIVE_POINTED)) {
                return expression.getValueArgument(0)!!
            }
        }

        if (descriptor == interop.nativePointedRawPtrGetter ||
                OverridingUtil.overrides(descriptor, interop.nativePointedRawPtrGetter)) {

            return expression.dispatchReceiver!!
        }

        return when (descriptor) {
            interop.cPointerRawValue.getter -> expression.dispatchReceiver!!

            interop.interpretPointed -> expression.getValueArgument(0)!!

            interop.arrayGetByIntIndex, interop.arrayGetByLongIndex -> {
                val array = expression.extensionReceiver!!
                val index = expression.getValueArgument(0)!!
                builder.arrayGet(array, index) ?: expression
            }

            interop.allocUninitializedArrayWithIntLength, interop.allocUninitializedArrayWithLongLength -> {
                val placement = expression.extensionReceiver!!
                val elementType = expression.type.arguments.single().type
                val length = expression.getValueArgument(0)!!
                builder.allocArray(placement, elementType, length) ?: expression
            }

            interop.allocVariable -> {
                val placement = expression.extensionReceiver!!
                val type = expression.getSingleTypeArgument()
                builder.alloc(placement, type) ?: expression
            }

            interop.typeOf -> {
                val type = expression.getSingleTypeArgument()
                builder.typeOf(type) ?: expression
            }

            interop.bitsToFloat -> {
                val argument = expression.getValueArgument(0)
                if (argument is IrConst<*> && argument.kind == IrConstKind.Int) {
                    val floatValue = kotlinx.cinterop.bitsToFloat(argument.value as Int)
                    builder.irFloat(floatValue)
                } else {
                    expression
                }
            }

            interop.bitsToDouble -> {
                val argument = expression.getValueArgument(0)
                if (argument is IrConst<*> && argument.kind == IrConstKind.Long) {
                    val doubleValue = kotlinx.cinterop.bitsToDouble(argument.value as Long)
                    builder.irDouble(doubleValue)
                } else {
                    expression
                }
            }

            else -> expression
        }
    }

    private fun IrCall.getSingleTypeArgument(): KotlinType {
        val typeParameter = descriptor.original.typeParameters.single()
        return getTypeArgument(typeParameter)!!
    }
}

private fun IrBuilder.irFloat(value: Float) =
        IrConstImpl.float(startOffset, endOffset, context.builtIns.floatType, value)

private fun IrBuilder.irDouble(value: Double) =
        IrConstImpl.double(startOffset, endOffset, context.builtIns.doubleType, value)