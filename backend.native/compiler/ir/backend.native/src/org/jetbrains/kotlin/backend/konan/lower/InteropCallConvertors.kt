/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.jvm.ir.propertyIfAccessor
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.backend.konan.llvm.IntrinsicType
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.isPropertyAccessor
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
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

private class InteropCallContext(
        val symbols: KonanSymbols,
        val builder: IrBuilderWithScope
)

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

private fun InteropCallContext.readPrimitiveFromMemory(
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

private fun InteropCallContext.writePrimitiveToMemory(
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

/**
 * Returns null if the given call-site is not an accessor for T.value, T : CEnumVar.
 * Otherwise, generates:
 *
 *  get() => byValue(this.reinterpret().value)
 *
 *  set(value) => this.reinterpret().value = value.value
 */
internal fun tryGenerateEnumVarValueAccess(
        callSite: IrCall,
        symbols: KonanSymbols,
        builder: IrBuilderWithScope
): IrExpression? = if (isEnumVarValueAccessor(callSite.symbol.owner, symbols))
    generateInteropCall(symbols, builder) { generateEnumVarValueAccess(callSite) }
else null

private fun InteropCallContext.generateEnumVarValueAccess(callSite: IrCall): IrExpression {
    val accessor = callSite.symbol.owner
    val nativePtr = builder.irCall(symbols.interopNativePointedRawPtrGetter).also {
        it.dispatchReceiver = callSite.dispatchReceiver!!
    }
    return when {
        accessor.isGetter -> {
            val enumClass = accessor.returnType.getClass()!!
            val enumPrimitiveType = enumClass.properties.single { it.name.asString() == "value" }
                    .getter!!.returnType
            val readMemory = readPrimitiveFromMemory(nativePtr, enumPrimitiveType)
            return convertIntegralToEnum(readMemory, accessor.returnType)
        }
        accessor.isSetter -> {
            val arg = callSite.getValueArgument(0)!!
            val valueToWrite = convertEnumToIntegral(arg)
            writePrimitiveToMemory(nativePtr, valueToWrite)
        }
        else -> error("")
    }
}