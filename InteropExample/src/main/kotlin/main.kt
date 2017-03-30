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
import kotlinx.cinterop.*
import llvm.*

fun main(args: Array<String>) = memScoped {
    val module = LLVMModuleCreateWithName("module")
    println("module=" + module.rawValue)

    val paramTypes = allocArrayOf(LLVMInt32Type(), LLVMInt32Type())
    val retType = LLVMFunctionType(LLVMInt32Type(), paramTypes[0].ptr, 2, 0)

    val sum = LLVMAddFunction(module, "sum", retType)
    val entry = LLVMAppendBasicBlock(sum, "entry")
    val builder = LLVMCreateBuilder()
    LLVMPositionBuilderAtEnd(builder, entry)
    val tmp = LLVMBuildAdd(builder, LLVMGetParam(sum, 0), LLVMGetParam(sum, 1), "tmp")
    LLVMBuildRet(builder, tmp)
    val engineRef = alloc<LLVMExecutionEngineRefVar>()
    val errorRef = allocPointerTo<CInt8Var>()
    LLVMInitializeNativeTarget()
    errorRef.value = null
    if (LLVMCreateExecutionEngineForModule(engineRef.ptr, module, errorRef.ptr) != 0) {
        println("failed to create execution engine")
        return
    }
    val error = errorRef.value
    if (error != null) {
        println(error.toKString())
        return
    }

    println(LLVMGetTypeKind(LLVMInt32Type()))
    val x = 5L
    val y = 6L
    val args = allocArrayOf(
            LLVMCreateGenericValueOfInt(LLVMInt32Type(), x, 0),
            LLVMCreateGenericValueOfInt(LLVMInt32Type(), y, 0))

    val runRes = LLVMRunFunction(engineRef.value, sum, 2, args[0].ptr)
    println(LLVMGenericValueToInt(runRes, 0))
    if (LLVMWriteBitcodeToFile(module, "/tmp/sum.bc") != 0) {
        println("error writing bitcode to file, skipping")
    }
    LLVMDisposeBuilder(builder)
    LLVMDisposeExecutionEngine(engineRef.value)
}
