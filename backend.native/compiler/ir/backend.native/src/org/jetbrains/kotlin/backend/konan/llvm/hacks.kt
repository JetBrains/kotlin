package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVarWithValueMappedTo

// TODO: the definitions below are required to perform smooth migration to new interop;
// remove after replacing with LLVM*Ref typedefs

typealias LLVMOpaqueValue = CPointer<llvm.LLVMOpaqueValue>
typealias LLVMOpaqueType = CPointer<llvm.LLVMOpaqueType>
typealias LLVMOpaqueBasicBlock = CPointer<llvm.LLVMOpaqueBasicBlock>
