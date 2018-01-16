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

class Runtime(bitcodeFile: String) {
    val llvmModule: LLVMModuleRef = parseBitcodeFile(bitcodeFile)

    internal fun getStructTypeOrNull(name: String) = LLVMGetTypeByName(llvmModule, "struct.$name")
    internal fun getStructType(name: String) = getStructTypeOrNull(name)!!

    val typeInfoType = getStructType("TypeInfo")
    val writableTypeInfoType = getStructTypeOrNull("WritableTypeInfo")
    val fieldTableRecordType = getStructType("FieldTableRecord")
    val methodTableRecordType = getStructType("MethodTableRecord")
    val globalHashType = getStructType("GlobalHash")

    val objHeaderType = getStructType("ObjHeader")
    val objHeaderPtrType = pointerType(objHeaderType)
    val arrayHeaderType = getStructType("ArrayHeader")

    val frameOverlayType = getStructType("FrameOverlay")

    val target = LLVMGetTarget(llvmModule)!!.toKString()

    val dataLayout = LLVMGetDataLayout(llvmModule)!!.toKString()

    val targetData = LLVMCreateTargetData(dataLayout)!!

    val kotlinObjCClassInfo by lazy { getStructType("KotlinObjCClassInfo") }
    val objCMethodDescription by lazy { getStructType("ObjCMethodDescription") }
    val objCTypeAdapter by lazy { getStructType("ObjCTypeAdapter") }
    val objCToKotlinMethodAdapter by lazy { getStructType("ObjCToKotlinMethodAdapter") }
    val kotlinToObjCMethodAdapter by lazy { getStructType("KotlinToObjCMethodAdapter") }
    val typeInfoObjCExportAddition by lazy { getStructType("TypeInfoObjCExportAddition") }


    val pointerSize: Int by lazy {
        LLVMABISizeOfType(targetData, objHeaderPtrType).toInt()
    }

    val pointerAlignment: Int by lazy {
        LLVMABIAlignmentOfType(targetData, objHeaderPtrType)
    }
}
