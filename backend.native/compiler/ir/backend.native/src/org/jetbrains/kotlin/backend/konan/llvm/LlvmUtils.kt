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

internal val LLVMValueRef.type: LLVMTypeRef
    get() = LLVMTypeOf(this)!!

/**
 * Represents the value which can be emitted as bitcode const value
 */
internal interface ConstValue {

    val llvm: LLVMValueRef
}

internal val ConstValue.llvmType: LLVMTypeRef
    get() = this.llvm.type

internal interface ConstPointer : ConstValue {
    fun getElementPtr(index: Int): ConstPointer = ConstGetElementPtr(this, index)
}

internal fun constPointer(value: LLVMValueRef) = object : ConstPointer {

    init {
        assert (LLVMIsConstant(value) == 1)
    }

    override val llvm = value
}

private class ConstGetElementPtr(val pointer: ConstPointer, val index: Int) : ConstPointer {
    override val llvm = LLVMConstInBoundsGEP(pointer.llvm, cValuesOf(Int32(0).llvm, Int32(index).llvm), 2)!!
    // TODO: squash multiple GEPs
}

internal fun ConstPointer.bitcast(toType: LLVMTypeRef) = constPointer(LLVMConstBitCast(this.llvm, toType)!!)

internal class ConstArray(val elemType: LLVMTypeRef?, val elements: List<ConstValue>) : ConstValue {

    override val llvm = LLVMConstArray(elemType, elements.map { it.llvm }.toCValues(), elements.size)!!
}

internal open class Struct(val type: LLVMTypeRef?, val elements: List<ConstValue>) : ConstValue {

    constructor(type: LLVMTypeRef?, vararg elements: ConstValue) : this(type, elements.toList())

    constructor(vararg elements: ConstValue) : this(structType(elements.map { it.llvmType }), *elements)

    override val llvm = LLVMConstNamedStruct(type, elements.map { it.llvm }.toCValues(), elements.size)!!
}

internal class Int1(val value: Byte) : ConstValue {
    override val llvm = LLVMConstInt(LLVMInt1Type(), value.toLong(), 1)!!
}

internal class Int8(val value: Byte) : ConstValue {
    override val llvm = LLVMConstInt(LLVMInt8Type(), value.toLong(), 1)!!
}

internal class Char16(val value: Char) : ConstValue {
    override val llvm = LLVMConstInt(LLVMInt16Type(), value.toLong(), 1)!!
}

internal class Int32(val value: Int) : ConstValue {
    override val llvm = LLVMConstInt(LLVMInt32Type(), value.toLong(), 1)!!
}

internal class Int64(val value: Long) : ConstValue {
    override val llvm = LLVMConstInt(LLVMInt64Type(), value, 1)!!
}

internal class Zero(val type: LLVMTypeRef) : ConstValue {
    override val llvm = LLVMConstNull(type)!!
}

internal class NullPointer(val pointeeType: LLVMTypeRef): ConstPointer {
    override val llvm = LLVMConstNull(pointerType(pointeeType))!!
}

internal fun constValue(value: LLVMValueRef) = object : ConstValue {
    init {
        assert (LLVMIsConstant(value) == 1)
    }

    override val llvm = value
}

internal val int1Type = LLVMInt1Type()!!
internal val int8Type = LLVMInt8Type()!!
internal val int32Type = LLVMInt32Type()!!
internal val int8TypePtr = pointerType(int8Type)

internal val voidType = LLVMVoidType()!!

internal val RuntimeAware.kTypeInfo: LLVMTypeRef
    get() = runtime.typeInfoType
internal val RuntimeAware.kObjHeader: LLVMTypeRef
    get() = runtime.objHeaderType
internal val RuntimeAware.kContainerHeader: LLVMTypeRef
    get() = runtime.containerHeaderType
internal val RuntimeAware.kObjHeaderPtr: LLVMTypeRef
    get() = pointerType(kObjHeader)
internal val RuntimeAware.kObjHeaderPtrPtr: LLVMTypeRef
    get() = pointerType(kObjHeaderPtr)
internal val RuntimeAware.kArrayHeader: LLVMTypeRef
    get() = runtime.arrayHeaderType
internal val RuntimeAware.kArrayHeaderPtr: LLVMTypeRef
    get() = pointerType(kArrayHeader)
internal val RuntimeAware.kTypeInfoPtr: LLVMTypeRef
    get() = pointerType(kTypeInfo)
internal val kInt1         = LLVMInt1Type()!!
internal val kBoolean      = kInt1
internal val kInt8Ptr      = pointerType(int8Type)
internal val kInt8PtrPtr   = pointerType(kInt8Ptr)
internal val kNullInt8Ptr  = LLVMConstNull(kInt8Ptr)!!
internal val kImmInt32One  = Int32(1).llvm
internal val kImmInt64One  = Int64(1).llvm
internal val ContextUtils.kNullObjHeaderPtr: LLVMValueRef
    get() = LLVMConstNull(this.kObjHeaderPtr)!!
internal val ContextUtils.kNullObjHeaderPtrPtr: LLVMValueRef
    get() = LLVMConstNull(this.kObjHeaderPtrPtr)!!

// Nothing type has no values, but we do generate unreachable code and thus need some fake value:
internal val ContextUtils.kNothingFakeValue: LLVMValueRef
    get() = LLVMGetUndef(kObjHeaderPtr)!!

internal fun pointerType(pointeeType: LLVMTypeRef) = LLVMPointerType(pointeeType, 0)!!

internal fun structType(vararg types: LLVMTypeRef): LLVMTypeRef = structType(types.toList())

internal fun structType(types: List<LLVMTypeRef>): LLVMTypeRef =
    LLVMStructType(types.toCValues(), types.size, 0)!!

internal fun ContextUtils.numParameters(functionType: LLVMTypeRef) : Int {
    // Note that type is usually function pointer, so we have to dereference it.
    return LLVMCountParamTypes(LLVMGetElementType(functionType))!!
}

internal fun ContextUtils.isObjectReturn(functionType: LLVMTypeRef) : Boolean {
    // Note that type is usually function pointer, so we have to dereference it.
    val returnType = LLVMGetReturnType(LLVMGetElementType(functionType))!!
    return isObjectType(returnType)
}

internal fun ContextUtils.isObjectRef(value: LLVMValueRef): Boolean {
    return isObjectType(value.type)
}

internal fun RuntimeAware.isObjectType(type: LLVMTypeRef): Boolean {
    return type == kObjHeaderPtr || type == kArrayHeaderPtr
}

/**
 * Reads [size] bytes contained in this array.
 */
internal fun CArrayPointer<ByteVar>.getBytes(size: Long) =
        (0 .. size-1).map { this[it] }.toByteArray()

internal fun getFunctionType(ptrToFunction: LLVMValueRef): LLVMTypeRef {
    return getGlobalType(ptrToFunction)
}

internal fun getGlobalType(ptrToGlobal: LLVMValueRef): LLVMTypeRef {
    return LLVMGetElementType(ptrToGlobal.type)!!
}

internal fun ContextUtils.addGlobal(name: String, type: LLVMTypeRef,
                                    threadLocal: Boolean = false): LLVMValueRef {
    assert(LLVMGetNamedGlobal(context.llvmModule, name) == null)
    val result = LLVMAddGlobal(context.llvmModule, type, name)!!
    if (threadLocal)
        LLVMSetThreadLocalMode(result, LLVMThreadLocalMode.LLVMLocalExecTLSModel)
    return result
}

internal fun ContextUtils.importGlobal(name: String, type: LLVMTypeRef,
                                       threadLocal: Boolean = false): LLVMValueRef {
    val found = LLVMGetNamedGlobal(context.llvmModule, name)
    if (found != null) {
        assert (getGlobalType(found) == type)
        assert (LLVMGetInitializer(found) == null)
        if (threadLocal)
            assert(LLVMGetThreadLocalMode(found) == LLVMThreadLocalMode.LLVMLocalExecTLSModel)
        return found
    } else {
        val result = LLVMAddGlobal(context.llvmModule, type, name)!!
        if (threadLocal)
            LLVMSetThreadLocalMode(result, LLVMThreadLocalMode.LLVMLocalExecTLSModel)
        return result
    }
}

internal fun functionType(returnType: LLVMTypeRef, isVarArg: Boolean = false, vararg paramTypes: LLVMTypeRef) =
        LLVMFunctionType(
                returnType,
                cValuesOf(*paramTypes), paramTypes.size,
                if (isVarArg) 1 else 0
        )!!


fun llvm2string(value: LLVMValueRef?): String {
  if (value == null) return "<null>"
  return LLVMPrintValueToString(value)!!.toKString()
}

fun llvmtype2string(type: LLVMTypeRef?): String {
    if (type == null) return "<null type>"
    return LLVMPrintTypeToString(type)!!.toKString()
}

fun getStructElements(type: LLVMTypeRef): List<LLVMTypeRef> {
    val count = LLVMCountStructElementTypes(type)
    return (0 until count).map {
        LLVMStructGetTypeAtIndex(type, it)!!
    }
}

fun parseBitcodeFile(path: String): LLVMModuleRef = memScoped {
    val bufRef = alloc<LLVMMemoryBufferRefVar>()
    val errorRef = allocPointerTo<ByteVar>()

    val res = LLVMCreateMemoryBufferWithContentsOfFile(path, bufRef.ptr, errorRef.ptr)
    if (res != 0) {
        throw Error(errorRef.value?.toKString())
    }

    val memoryBuffer = bufRef.value
    try {

        val moduleRef = alloc<LLVMModuleRefVar>()
        val parseRes = LLVMParseBitcode2(memoryBuffer, moduleRef.ptr)
        if (parseRes != 0) {
            throw Error(parseRes.toString())
        }

        moduleRef.value!!
    } finally {
        LLVMDisposeMemoryBuffer(memoryBuffer)
    }
}

internal fun String.mdString() = LLVMMDString(this, this.length)!!
internal fun node(vararg it:LLVMValueRef) = LLVMMDNode(it.toList().toCValues(), it.size)
