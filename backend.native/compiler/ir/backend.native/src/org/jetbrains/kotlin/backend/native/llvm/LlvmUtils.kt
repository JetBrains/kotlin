package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.utils.singletonOrEmptyList

/**
 * Represents the value which can be emitted as bitcode const value
 */
internal interface CompileTimeValue {

    fun getLlvmValue(): LLVMOpaqueValue?

    fun getLlvmType(): LLVMOpaqueType? {
        return LLVMTypeOf(getLlvmValue())
    }

}

internal class ConstArray(val elemType: LLVMOpaqueType?, val elements: List<CompileTimeValue>) : CompileTimeValue {

    override fun getLlvmValue(): LLVMOpaqueValue? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()

        memScoped {
            val valuesNativeArrayPtr = allocNativeArrayOf(LLVMOpaqueValue, *values)[0]

            return LLVMConstArray(elemType, valuesNativeArrayPtr, values.size)
        }
    }
}

internal open class Struct(val type: LLVMOpaqueType?, val elements: List<CompileTimeValue>) : CompileTimeValue {

    constructor(type: LLVMOpaqueType?, vararg elements: CompileTimeValue) : this(type, elements.toList())

    override fun getLlvmValue(): LLVMOpaqueValue? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()
        memScoped {
            val valuesNativeArrayPtr = allocNativeArrayOf(LLVMOpaqueValue, *values)[0]
            return LLVMConstNamedStruct(type, valuesNativeArrayPtr, values.size)
        }
    }
}

internal class Int8(val value: Byte) : CompileTimeValue {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt8Type(), value.toLong(), 1)
}

internal class Int32(val value: Int) : CompileTimeValue {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt32Type(), value.toLong(), 1)
}

internal class Int64(val value: Long) : CompileTimeValue {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt64Type(), value, 1)
}

internal class Zero(val type: LLVMOpaqueType?) : CompileTimeValue {
    override fun getLlvmValue() = LLVMConstNull(type)
}

internal fun compileTimeValue(value: LLVMOpaqueValue?) = object : CompileTimeValue {
    override fun getLlvmValue() = value
}

internal val int8Type = LLVMInt8Type()
internal val int32Type = LLVMInt32Type()

internal fun pointerType(pointeeType: LLVMOpaqueType?) = LLVMPointerType(pointeeType, 0)

internal fun getLlvmFunctionType(function: FunctionDescriptor): LLVMOpaqueType? {
    val returnType = getLLVMType(function.returnType!!)
    val params = function.dispatchReceiverParameter.singletonOrEmptyList() +
            function.extensionReceiverParameter.singletonOrEmptyList() +
            function.valueParameters

    var extraParam = listOf<LLVMOpaqueType?>()
    if (function is ClassConstructorDescriptor) {
        extraParam += pointerType(LLVMInt8Type())
    }
    val paramTypes:List<LLVMOpaqueType?> = params.map { getLLVMType(it.type) }
    extraParam += paramTypes

    if (extraParam.size == 0) return LLVMFunctionType(returnType, null, 0, 0)
    memScoped {
        val paramTypesPtr = allocNativeArrayOf(LLVMOpaqueType, *extraParam.toTypedArray())[0] // TODO: dispose
        return LLVMFunctionType(returnType, paramTypesPtr, extraParam.size, 0)
    }
}

/**
 * Represents [size] bytes contained in this array as [ConstArray].
 */
internal fun NativeArray<Int8Box>.asCompileTimeValue(size: Int): ConstArray {
    return ConstArray(int8Type, (0 .. size-1).map {
        Int8(this[it].value)
    })
}

internal fun getFunctionType(ptrToFunction: LLVMOpaqueValue?): LLVMOpaqueType {
    val typeOfPtrToFunction = LLVMTypeOf(ptrToFunction)
    return LLVMGetElementType(typeOfPtrToFunction)!!
}