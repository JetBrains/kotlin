/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isNothing
import org.jetbrains.kotlin.ir.types.isUnit

private val primitiveToLlvm = PrimitiveBinaryType.values().associate {
    it to when (it) {
        PrimitiveBinaryType.BOOLEAN -> LLVMInt1Type()
        PrimitiveBinaryType.BYTE -> LLVMInt8Type()
        PrimitiveBinaryType.SHORT -> LLVMInt16Type()
        PrimitiveBinaryType.INT -> LLVMInt32Type()
        PrimitiveBinaryType.LONG -> LLVMInt64Type()
        PrimitiveBinaryType.FLOAT -> LLVMFloatType()
        PrimitiveBinaryType.DOUBLE -> LLVMDoubleType()

        PrimitiveBinaryType.POINTER -> int8TypePtr
    }!!
}

private fun RuntimeAware.getLlvmType(primitiveBinaryType: PrimitiveBinaryType?) =
        primitiveBinaryType?.let { primitiveToLlvm[it]!! } ?: this.kObjHeaderPtr

internal fun RuntimeAware.getLLVMType(type: IrType): LLVMTypeRef =
        getLlvmType(type.computePrimitiveBinaryTypeOrNull())

internal fun RuntimeAware.getLLVMType(type: DataFlowIR.Type) =
        getLlvmType(type.primitiveBinaryType)

internal fun RuntimeAware.getLLVMReturnType(type: IrType): LLVMTypeRef {
    return when {
        type.isUnit() || type.isNothing() -> LLVMVoidType()!!
        else -> getLLVMType(type)
    }
}