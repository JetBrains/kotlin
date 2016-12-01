package org.jetbrains.kotlin.backend.konan.llvm

import llvm.*
import org.jetbrains.kotlin.backend.konan.ModuleIndex
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class Context(val irModule: IrModuleFragment, val runtime: Runtime, val llvmModule: LLVMModuleRef) {
    val moduleIndex = ModuleIndex(irModule)

    private fun importFunction(name: String, otherModule: LLVMModuleRef): LLVMValueRef {
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
    val allocArrayFunction = importRtFunction("AllocArrayInstance")
    val setArrayFunction = importRtFunction("Kotlin_Array_set")
    val lookupFieldOffset = importRtFunction("LookupFieldOffset")
    val lookupOpenMethodFunction = importRtFunction("LookupOpenMethod")
    val isInstanceFunction = importRtFunction("IsInstance")
    val checkInstanceFunction = importRtFunction("CheckInstance")
    val throwExceptionFunction = importRtFunction("ThrowException")
    val usedFunctions = mutableListOf<LLVMValueRef>()
}
