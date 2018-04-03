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

package org.jetbrains.kotlin.backend.konan.llvm.objc

import llvm.LLVMTypeRef
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.descriptors.stdlibModule
import org.jetbrains.kotlin.backend.konan.llvm.*

internal open class ObjCCodeGenerator(val codegen: CodeGenerator) {
    val context = codegen.context

    val dataGenerator = codegen.objCDataGenerator!!

    fun FunctionGenerationContext.genSelector(selector: String): LLVMValueRef {
        val selectorRef = dataGenerator.genSelectorRef(selector)
        // TODO: clang emits it with `invariant.load` metadata.
        return load(selectorRef.llvm)
    }

    fun FunctionGenerationContext.genGetSystemClass(name: String): LLVMValueRef {
        val classRef = dataGenerator.genClassRef(name)
        return load(classRef.llvm)
    }

    private val objcMsgSend = constPointer(
            context.llvm.externalFunction(
                    "objc_msgSend",
                    functionType(int8TypePtr, true, int8TypePtr, int8TypePtr),
                    context.stdlibModule.llvmSymbolOrigin
            )
    )

    val objcRelease = context.llvm.externalFunction(
            "objc_release",
            functionType(voidType, false, int8TypePtr),
            context.stdlibModule.llvmSymbolOrigin
    )

    // TODO: this doesn't support stret.
    fun msgSender(functionType: LLVMTypeRef): LLVMValueRef =
            objcMsgSend.bitcast(pointerType(functionType)).llvm
}
