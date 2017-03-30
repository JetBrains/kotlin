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

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

interface RuntimeAware {
    val runtime: Runtime
}

class Runtime(private val bitcodeFile: String) {
    val llvmModule: LLVMModuleRef

    init {
        llvmModule = memScoped {

            val bufRef = alloc<LLVMMemoryBufferRefVar>()
            val errorRef = allocPointerTo<CInt8Var>()

            val res = LLVMCreateMemoryBufferWithContentsOfFile(bitcodeFile, bufRef.ptr, errorRef.ptr)
            if (res != 0) {
                throw Error(errorRef.value?.toKString())
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

    val target = LLVMGetTarget(llvmModule)!!.toKString()

    val dataLayout = LLVMGetDataLayout(llvmModule)!!.toKString()

    val targetData = LLVMCreateTargetData(dataLayout)!!

}
