/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.optimizations.DataFlowIR
import org.jetbrains.kotlin.ir.types.IrType
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
        type.isUnit() -> LLVMVoidType()!!
        // TODO: stdlib have methods taking Nothing, such as kotlin.collections.EmptySet.contains().
        // KotlinBuiltIns.isNothing(type) -> LLVMVoidType()
        else -> getLLVMType(type)
    }
}