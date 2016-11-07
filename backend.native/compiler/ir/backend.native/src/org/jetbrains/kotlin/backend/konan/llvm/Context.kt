package org.jetbrains.kotlin.backend.native.llvm

import llvm.*
import org.jetbrains.kotlin.backend.native.ModuleIndex
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class Context(val irModule: IrModuleFragment, val runtime: Runtime, val llvmModule: LLVMOpaqueModule) {
    val moduleIndex = ModuleIndex(irModule)

    val llvmBuilder = LLVMCreateBuilder()

    private fun importFunction(name: String, otherModule: LLVMOpaqueModule): LLVMOpaqueValue {
        if (LLVMGetNamedFunction(llvmModule, name) != null) {
            throw IllegalArgumentException("function $name already exists")
        }

        val externalFunction = LLVMGetNamedFunction(otherModule, name)!!

        val functionType = getFunctionType(externalFunction)
        return LLVMAddFunction(llvmModule, name, functionType)!!
    }

    val staticData = StaticData(this)

    private fun importRtFunction(name: String) = importFunction(name, runtime.llvmModule)

    val allocInstanceFunction = importRtFunction("AllocInstance")

    fun dispose() {
        LLVMDisposeBuilder(llvmBuilder)
    }
}