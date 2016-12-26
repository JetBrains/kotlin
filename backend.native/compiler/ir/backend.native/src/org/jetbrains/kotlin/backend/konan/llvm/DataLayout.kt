package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.descriptors.isUnboundCallableReference
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit

internal fun ContextUtils.getLLVMType(type: KotlinType): LLVMTypeRef {
    return when {
        // Nullable types must be represented as objects for boxing.
        type.isMarkedNullable -> this.kObjHeaderPtr
        type.isUnboundCallableReference() -> int8TypePtr
        KotlinBuiltIns.isBoolean(type) -> LLVMInt1Type()
        KotlinBuiltIns.isByte(type) -> LLVMInt8Type()
        KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isChar(type) -> LLVMInt16Type()
        KotlinBuiltIns.isInt(type) -> LLVMInt32Type()
        KotlinBuiltIns.isLong(type) -> LLVMInt64Type()
        KotlinBuiltIns.isFloat(type) -> LLVMFloatType()
        KotlinBuiltIns.isDouble(type) -> LLVMDoubleType()
        !KotlinBuiltIns.isPrimitiveType(type) -> this.kObjHeaderPtr
        else -> throw NotImplementedError(type.toString() + " is not supported")
    }!!
}

internal fun ContextUtils.getLLVMReturnType(type: KotlinType): LLVMTypeRef {
    return when {
        type.isUnit() -> LLVMVoidType()!!
        // TODO: stdlib have methods taking Nothing, such as kotlin.collections.EmptySet.contains().
        // KotlinBuiltIns.isNothing(type) -> LLVMVoidType()
        else -> getLLVMType(type)
    }
}