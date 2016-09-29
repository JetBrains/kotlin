package org.jetbrains.kotlin.backend.native.llvm

import llvm.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

fun emitLLVM(module: IrModuleFragment, runtimeFile: String, outFile: String) {
    val llvmModule = LLVMModuleCreateWithName("out")!!
    val runtime = Runtime(runtimeFile)
    LLVMSetDataLayout(llvmModule, runtime.dataLayout)
    LLVMSetTarget(llvmModule, runtime.target)
    module.accept(RTTIGenerator(llvmModule, runtime), null)
    LLVMWriteBitcodeToFile(llvmModule, outFile)
}