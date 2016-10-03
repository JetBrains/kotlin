package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.mallocNativeArrayOf
import llvm.*

/**
 * Represents the value which can be emitted as bitcode const value
 */
internal abstract class CompileTimeValue {

    abstract fun getLlvmValue(): LLVMOpaqueValue?

    fun getLlvmType(): LLVMOpaqueType? {
        return LLVMTypeOf(getLlvmValue())
    }

}

internal class ConstArray(val elemType: LLVMOpaqueType?, val elements: List<CompileTimeValue>) : CompileTimeValue() {

    override fun getLlvmValue(): LLVMOpaqueValue? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()
        val valuesNativeArrayPtr = mallocNativeArrayOf(LLVMOpaqueValue, *values)[0] // FIXME: dispose

        return LLVMConstArray(elemType, valuesNativeArrayPtr, values.size)
    }
}

internal open class Struct(val type: LLVMOpaqueType?, val elements: List<CompileTimeValue>) : CompileTimeValue() {

    constructor(type: LLVMOpaqueType?, vararg elements: CompileTimeValue) : this(type, elements.toList())

    override fun getLlvmValue(): LLVMOpaqueValue? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()
        val valuesNativeArrayPtr = mallocNativeArrayOf(LLVMOpaqueValue, *values)[0] // FIXME: dispose
        return LLVMConstNamedStruct(type, valuesNativeArrayPtr, values.size)
    }
}

internal class Int32(val value: Int) : CompileTimeValue() {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt32Type(), value.toLong(), 1)
}

internal class Int64(val value: Long) : CompileTimeValue() {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt64Type(), value, 1)
}

internal class Zero(val type: LLVMOpaqueType?) : CompileTimeValue() {
    override fun getLlvmValue() = LLVMConstNull(type)
}

internal fun compileTimeValue(value: LLVMOpaqueValue?) = object : CompileTimeValue() {
    override fun getLlvmValue() = value
}

internal val int32Type = LLVMInt32Type()

internal fun pointerType(pointeeType: LLVMOpaqueType?) = LLVMPointerType(pointeeType, 0)