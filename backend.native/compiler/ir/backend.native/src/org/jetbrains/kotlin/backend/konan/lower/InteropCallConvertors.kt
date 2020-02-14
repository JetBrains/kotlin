/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.cgen.isCEnumType
import org.jetbrains.kotlin.backend.konan.cgen.isVector
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.isPropertyAccessor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*

/**
 * Check given function is a getter or setter
 * for `value` property of CEnumVar subclass.
 */
private fun isEnumVarValueAccessor(function: IrFunction, symbols: KonanSymbols): Boolean {
    val parent = function.parent as? IrClass ?: return false
    return if (symbols.interopCEnumVar in parent.superClasses && function.isPropertyAccessor) {
        (function.propertyIfAccessor as IrProperty).name.asString() == "value"
    } else {
        false
    }
}

private fun isMemberAtAccessor(function: IrFunction): Boolean =
        function.hasAnnotation(RuntimeNames.cStructMemberAt)

private fun isArrayMemberAtAccessor(function: IrFunction): Boolean =
        function.hasAnnotation(RuntimeNames.cStructArrayMemberAt)

private fun isBitFieldAccessor(function: IrFunction): Boolean =
        function.hasAnnotation(RuntimeNames.cStructBitField)

private class InteropCallContext(
        val symbols: KonanSymbols,
        val builder: IrBuilderWithScope
) {
    fun IrType.isCPointer(): Boolean = this.classOrNull == symbols.interopCPointer

    fun IrType.isNativePointed(): Boolean = isSubtypeOfClass(symbols.nativePointed)

    fun IrType.isStoredInMemoryDirectly(): Boolean =
            isPrimitiveType() || isUnsigned() || isVector()
}

private inline fun <T> generateInteropCall(
        symbols: KonanSymbols,
        builder: IrBuilderWithScope,
        block: InteropCallContext.() -> T
) = InteropCallContext(symbols, builder).block()

/**
 * Search for memory read/write function in [kotlinx.cinterop.nativeMemUtils] of a given [valueType].
 */
private fun InteropCallContext.findMemoryAccessFunction(isRead: Boolean, valueType: IrType): IrFunction {
    val requiredType = if (isRead) {
        IntrinsicType.INTEROP_READ_PRIMITIVE
    } else {
        IntrinsicType.INTEROP_WRITE_PRIMITIVE
    }
    val nativeMemUtilsClass = symbols.nativeMemUtils.owner
    return nativeMemUtilsClass.functions.filter {
        val annotationArgument = it.annotations
                .findAnnotation(RuntimeNames.typedIntrinsicAnnotation)
                ?.getAnnotationStringValue()
        annotationArgument == requiredType.name
    }.first {
        if (isRead) {
            it.returnType.classOrNull == valueType.classOrNull
        } else {
            it.valueParameters.last().type.classOrNull == valueType.classOrNull
        }
    }
}

private fun InteropCallContext.readValueFromMemory(
        nativePtr: IrExpression,
        returnType: IrType
): IrExpression  {
    val memoryValueType = determineInMemoryType(returnType)
    val memReadFn = findMemoryAccessFunction(isRead = true, valueType = memoryValueType)
    val memRead = builder.irCall(memReadFn).also { memRead ->
        memRead.dispatchReceiver = builder.irGetObject(symbols.nativeMemUtils)
        memRead.putValueArgument(0, builder.irCall(symbols.interopInterpretNullablePointed).also {
            it.putValueArgument(0, nativePtr)
        })
    }
    return castPrimitiveIfNeeded(memRead, returnType)
}

private fun InteropCallContext.writeValueToMemory(
        nativePtr: IrExpression,
        value: IrExpression
): IrExpression {
    val memoryValueType = determineInMemoryType(value.type)
    val memWriteFn = findMemoryAccessFunction(isRead = false, valueType = memoryValueType)
    val valueToWrite = castPrimitiveIfNeeded(value, memoryValueType)
    return with(builder) {
        irCall(memWriteFn).also { memWrite ->
            memWrite.dispatchReceiver = irGetObject(symbols.nativeMemUtils)
            memWrite.putValueArgument(0, irCall(symbols.interopInterpretNullablePointed).also {
                it.putValueArgument(0, nativePtr)
            })
            memWrite.putValueArgument(1, valueToWrite)
        }
    }
}

private fun InteropCallContext.determineInMemoryType(type: IrType): IrType {
    val classifier = type.classOrNull!!
    return if (classifier in symbols.unsignedIntegerClasses) {
        symbols.unsignedToSignedOfSameBitWidth.getValue(classifier).owner.defaultType
    } else {
        type
    }
}

private fun InteropCallContext.castPrimitiveIfNeeded(
        value: IrExpression,
        targetType: IrType
): IrExpression {
    val valueClass = value.type.classOrNull!!
    val targetClass = targetType.classOrNull!!
    return if (valueClass != targetClass) {
        val conversion = symbols.integerConversions.getValue(valueClass to targetClass)
        builder.irCall(conversion.owner).apply {
            if (conversion.owner.dispatchReceiverParameter != null) {
                dispatchReceiver = value
            } else {
                extensionReceiver = value
            }
        }
    } else {
        value
    }
}

private fun InteropCallContext.convertEnumToIntegral(enumValue: IrExpression): IrExpression {
    val enumClass = enumValue.type.getClass()!!
    val valueProperty = enumClass.properties.single { it.name.asString() == "value" }
    return builder.irCall(valueProperty.getter!!).also {
        it.dispatchReceiver = enumValue
    }
}

private fun InteropCallContext.convertIntegralToEnum(value: IrExpression, enumType: IrType): IrExpression {
    val enumClass = enumType.getClass()!!
    val companionClass = enumClass.companionObject()!! as IrClass
    val byValue = companionClass.simpleFunctions().single { it.name.asString() == "byValue" }
    val byValueArg = castPrimitiveIfNeeded(value, byValue.valueParameters.first().type)
    return builder.irCall(byValue).apply {
        dispatchReceiver = builder.irGetObject(companionClass.symbol)
        putValueArgument(0, byValueArg)
    }
}

private fun IrType.getCEnumPrimitiveType(): IrType {
    assert(this.isCEnumType())
    val enumClass = this.getClass()!!
    return enumClass.properties.single { it.name.asString() == "value" }
            .getter!!.returnType
}

private fun InteropCallContext.readEnumValueFromMemory(nativePtr: IrExpression, enumType: IrType): IrExpression {
    val enumPrimitiveType = enumType.getCEnumPrimitiveType()
    val readMemory = readValueFromMemory(nativePtr, enumPrimitiveType)
    return convertIntegralToEnum(readMemory, enumType)
}

private fun InteropCallContext.writeEnumValueToMemory(nativePtr: IrExpression, value: IrExpression): IrExpression {
    val valueToWrite = convertEnumToIntegral(value)
    return writeValueToMemory(nativePtr, valueToWrite)
}

private fun InteropCallContext.convertCPointerToNativePtr(cPointer: IrExpression): IrExpression {
    return builder.irCall(symbols.interopCPointerGetRawValue).also {
        it.extensionReceiver = cPointer
    }
}


private fun InteropCallContext.writePointerToMemory(nativePtr: IrExpression, value: IrExpression): IrExpression {
    val valueToWrite = when {
        value.type.isCPointer() -> convertCPointerToNativePtr(value)
        else -> error("Unsupported pointer type")
    }
    return writeValueToMemory(nativePtr, valueToWrite)
}

private fun InteropCallContext.calculateFieldPointer(receiver: IrExpression, offset: Long): IrExpression {
    val base = builder.irCall(symbols.interopNativePointedRawPtrGetter).also {
        it.dispatchReceiver = receiver
    }
    val nativePtrPlusLong = symbols.nativePtrType.getClass()!!
            .functions.single { it.name.identifier == "plus" }
    return with (builder) {
        irCall(nativePtrPlusLong).also {
            it.dispatchReceiver = base
            it.putValueArgument(0, irLong(offset))
        }
    }
}

private fun InteropCallContext.readPointerFromMemory(nativePtr: IrExpression): IrExpression {
    val readMemory = readValueFromMemory(nativePtr, symbols.nativePtrType)
    return builder.irCall(symbols.interopInterpretCPointer).also {
        it.putValueArgument(0, readMemory)
    }
}

private fun InteropCallContext.readPointed(nativePtr: IrExpression): IrExpression {
    return builder.irCall(symbols.interopInterpretNullablePointed).also {
        it.putValueArgument(0, nativePtr)
    }
}

/** Returns non-null result if [callSite] is accessor to:
 *  1. T.value, T : CEnumVar
 *  2. T.<field-name>, T : CStructVar and accessor is annotated with
 *      [kotlinx.cinterop.internal.CStruct.MemberAt] or [kotlinx.cinterop.internal.CStruct.BitField]
 */
internal fun tryGenerateInteropMemberAccess(
        callSite: IrCall,
        symbols: KonanSymbols,
        builder: IrBuilderWithScope
): IrExpression? = when {
    isEnumVarValueAccessor(callSite.symbol.owner, symbols) ->
        generateInteropCall(symbols, builder) { generateEnumVarValueAccess(callSite) }
    isMemberAtAccessor(callSite.symbol.owner) ->
        generateInteropCall(symbols, builder) { generateMemberAtAccess(callSite) }
    isBitFieldAccessor(callSite.symbol.owner) ->
        generateInteropCall(symbols, builder) { generateBitFieldAccess(callSite) }
    isArrayMemberAtAccessor(callSite.symbol.owner) ->
        generateInteropCall(symbols, builder) { generateArrayMemberAtAccess(callSite) }
    else -> null
}

private fun InteropCallContext.generateEnumVarValueAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val nativePtr = builder.irCall(symbols.interopNativePointedRawPtrGetter).also {
        it.dispatchReceiver = callSite.dispatchReceiver!!
    }
    return when {
        accessor.isGetter -> readEnumValueFromMemory(nativePtr, accessor.returnType)
        accessor.isSetter -> writeEnumValueToMemory(nativePtr, callSite.getValueArgument(0)!!)
        else -> error("")
    }
}

private fun InteropCallContext.generateMemberAtAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val memberAt = accessor.getAnnotation(RuntimeNames.cStructMemberAt)!!
    val offset = (memberAt.getValueArgument(0) as IrConst<Long>).value
    val fieldPointer = calculateFieldPointer(callSite.dispatchReceiver!!, offset)
    return when {
        accessor.isGetter -> {
            val type = accessor.returnType
            when {
                type.isCEnumType() -> readEnumValueFromMemory(fieldPointer, type)
                type.isStoredInMemoryDirectly() -> readValueFromMemory(fieldPointer, type)
                type.isCPointer() -> readPointerFromMemory(fieldPointer)
                type.isNativePointed() -> readPointed(fieldPointer)
                else -> error("Cannot get field type: ${type.getClass()?.name}")
            }
        }
        accessor.isSetter -> {
            val value = callSite.getValueArgument(0)!!
            val type = accessor.valueParameters[0].type
            when {
                type.isCEnumType() -> writeEnumValueToMemory(fieldPointer, value)
                type.isStoredInMemoryDirectly() -> writeValueToMemory(fieldPointer, value)
                type.isCPointer() -> writePointerToMemory(fieldPointer, value)
                else -> error("Cannot set field of type ${type.getClass()?.name}")
            }
        }
        else -> error("")
    }
}

private fun InteropCallContext.generateArrayMemberAtAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val memberAt = accessor.getAnnotation(RuntimeNames.cStructArrayMemberAt)!!
    val offset = (memberAt.getValueArgument(0) as IrConst<Long>).value
    val fieldPointer = calculateFieldPointer(callSite.dispatchReceiver!!, offset)
    return builder.irCall(symbols.interopInterpretCPointer).also {
        it.putValueArgument(0, fieldPointer)
    }
}

private fun InteropCallContext.writeBits(
        base: IrExpression,
        offset: Long,
        size: Int,
        value: IrExpression
): IrExpression {
    val integralValue = when {
        value.type.isCEnumType() -> convertEnumToIntegral(value)
        else -> value
    }
    val targetType = symbols.writeBits.owner.valueParameters.last().type
    val valueToWrite = castPrimitiveIfNeeded(integralValue, targetType)
    return with(builder) {
        irCall(symbols.writeBits).also {
            it.putValueArgument(0, base)
            it.putValueArgument(1, irLong(offset))
            it.putValueArgument(2, irInt(size))
            it.putValueArgument(3, valueToWrite)
        }
    }
}

private fun InteropCallContext.readBits(
        base: IrExpression,
        offset: Long,
        size: Int,
        type: IrType
): IrExpression {
    val isSigned = when {
        type.isCEnumType() ->
            !type.getCEnumPrimitiveType().isUnsigned()
        else ->
            !type.isUnsigned()
    }
    val integralValue = with (builder) {
        irCall(symbols.readBits).also {
            it.putValueArgument(0, base)
            it.putValueArgument(1, irLong(offset))
            it.putValueArgument(2, irInt(size))
            it.putValueArgument(3, irBoolean(isSigned))
        }
    }
    return when {
        type.isCEnumType() -> convertIntegralToEnum(integralValue, type)
        else -> castPrimitiveIfNeeded(integralValue, type)
    }
}

private fun InteropCallContext.generateBitFieldAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val bitField = accessor.getAnnotation(RuntimeNames.cStructBitField)!!
    val offset = (bitField.getValueArgument(0) as IrConst<Long>).value
    val size = (bitField.getValueArgument(1) as IrConst<Int>).value
    val base = builder.irCall(symbols.interopNativePointedRawPtrGetter).also {
        it.dispatchReceiver = callSite.dispatchReceiver!!
    }
    return when {
        accessor.isSetter -> {
            val argument = callSite.getValueArgument(0)!!
            writeBits(base, offset, size, argument)
        }
        accessor.isGetter -> {
            val type = accessor.returnType
            readBits(base, offset, size, type)
        }
        else -> error("Unexpected function: ${accessor.name}")
    }
}