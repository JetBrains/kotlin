package org.jetbrains.kotlin.backend.native.llvm

import llvm.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.types.KotlinType

internal fun getLLVMType(type: KotlinType): LLVMOpaqueType {
    return when {
        KotlinBuiltIns.isBoolean(type) || KotlinBuiltIns.isByte(type) -> LLVMInt8Type()
        KotlinBuiltIns.isShort(type) || KotlinBuiltIns.isChar(type) -> LLVMInt16Type()
        KotlinBuiltIns.isInt(type) -> LLVMInt32Type()
        KotlinBuiltIns.isLong(type) -> LLVMInt64Type()
        !KotlinBuiltIns.isPrimitiveType(type) -> LLVMPointerType(LLVMInt8Type(), 0)
        else -> throw NotImplementedError()
    }!!
}