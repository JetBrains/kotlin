package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*

class Runtime(private val bitcodeFile: String) {
    val llvmModule: LLVMModuleRef

    init {
        llvmModule = memScoped {

            val bufRef = alloc<LLVMMemoryBufferRefVar>()
            val errorRef = allocPointerTo<CInt8Var>()
            val res = LLVMCreateMemoryBufferWithContentsOfFile(bitcodeFile, bufRef.ptr, errorRef.ptr)
            if (res != 0) {
                throw Error(errorRef.value?.asCString()?.toString())
            }

            val moduleRef = alloc<LLVMModuleRefVar>()
            val parseRes = LLVMParseBitcode2(bufRef.value, moduleRef.ptr)
            if (parseRes != 0) {
                throw Error(parseRes.toString())
            }

            moduleRef.value!!
        }
    }

    private fun getStructType(name: String) = LLVMGetTypeByName(llvmModule, "struct.$name")!!

    val typeInfoType = getStructType("TypeInfo")
    val fieldTableRecordType = getStructType("FieldTableRecord")
    val methodTableRecordType = getStructType("MethodTableRecord")
    val globalHashType = getStructType("GlobalHash")

    val containerHeaderType = getStructType("ContainerHeader")
    val objHeaderType = getStructType("ObjHeader")
    val arrayHeaderType = getStructType("ArrayHeader")

    val target = LLVMGetTarget(llvmModule)!!.asCString().toString()

    val dataLayout = LLVMGetDataLayout(llvmModule)!!.asCString().toString()

    val targetData = LLVMCreateTargetData(dataLayout)!!

}
