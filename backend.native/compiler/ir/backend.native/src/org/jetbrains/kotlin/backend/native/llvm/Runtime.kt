package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.*
import llvm.*

class Runtime(private val bitcodeFile: String) {
    val llvmModule: LLVMOpaqueModule

    init {
        val arena = Arena()
        try {

            val bufRef = arena.alloc(LLVMOpaqueMemoryBuffer.ref)
            val errorRef = arena.alloc(Int8Box.ref)
            val res = LLVMCreateMemoryBufferWithContentsOfFile(bitcodeFile, bufRef, errorRef)
            if (res != 0) {
                throw Error(errorRef.value?.asCString()?.toString())
            }

            val moduleRef = arena.alloc(LLVMOpaqueModule.ref)
            val parseRes = LLVMParseBitcode2(bufRef.value, moduleRef)
            if (parseRes != 0) {
                throw Error(parseRes.toString())
            }

            llvmModule = moduleRef.value!!

        } finally {
            arena.clear()
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
