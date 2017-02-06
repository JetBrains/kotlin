package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.ValueType
import org.jetbrains.kotlin.backend.konan.isRepresentedAs
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit

private val valueTypes = ValueType.values().associate {
    it to when (it) {
        ValueType.BOOLEAN -> LLVMInt1Type()
        ValueType.BYTE -> LLVMInt8Type()
        ValueType.SHORT, ValueType.CHAR -> LLVMInt16Type()
        ValueType.INT -> LLVMInt32Type()
        ValueType.LONG -> LLVMInt64Type()
        ValueType.FLOAT -> LLVMFloatType()
        ValueType.DOUBLE -> LLVMDoubleType()

        ValueType.UNBOUND_CALLABLE_REFERENCE,
        ValueType.NATIVE_PTR, ValueType.NATIVE_POINTED, ValueType.C_POINTER -> int8TypePtr
    }!!
}

internal fun RuntimeAware.getLLVMType(type: KotlinType): LLVMTypeRef {
    for ((valueType, llvmType) in valueTypes) {
        if (type.isRepresentedAs(valueType)) {
            return llvmType
        }
    }

    return this.kObjHeaderPtr
}

internal fun RuntimeAware.getLLVMReturnType(type: KotlinType): LLVMTypeRef {
    return when {
        type.isUnit() -> LLVMVoidType()!!
        // TODO: stdlib have methods taking Nothing, such as kotlin.collections.EmptySet.contains().
        // KotlinBuiltIns.isNothing(type) -> LLVMVoidType()
        else -> getLLVMType(type)
    }
}