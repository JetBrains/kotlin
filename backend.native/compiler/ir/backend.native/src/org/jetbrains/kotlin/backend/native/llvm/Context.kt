package org.jetbrains.kotlin.backend.native.llvm

import llvm.LLVMCreateBuilder
import llvm.LLVMDisposeBuilder
import llvm.LLVMOpaqueModule
import org.jetbrains.kotlin.backend.native.ModuleIndex
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class Context(val irModule: IrModuleFragment, val runtime: Runtime, val llvmModule: LLVMOpaqueModule) {
    val moduleIndex = ModuleIndex(irModule)

    val llvmBuilder = LLVMCreateBuilder()

    fun dispose() {
        LLVMDisposeBuilder(llvmBuilder)
    }
}