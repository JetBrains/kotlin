package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.utils.singletonOrEmptyList

/**
 * Represents the value which can be emitted as bitcode const value
 */
internal interface ConstValue {

    fun getLlvmValue(): LLVMOpaqueValue?

    fun getLlvmType(): LLVMOpaqueType {
        return LLVMTypeOf(getLlvmValue())!!
    }

}

internal interface ConstPointer : ConstValue {
    fun getElementPtr(index: Int): ConstPointer = ConstGetElementPtr(this, index)
}

internal fun constPointer(value: LLVMOpaqueValue?) = object : ConstPointer {
    override fun getLlvmValue() = value
}

private class ConstGetElementPtr(val pointer: ConstPointer, val index: Int) : ConstPointer {
    override fun getLlvmValue(): LLVMOpaqueValue? {
        // TODO: probably it should be computed once when initialized?
        // TODO: squash multiple GEPs
        val indices = intArrayOf(0, index).map { Int32(it).getLlvmValue() }
        memScoped {
            val indicesArray = allocNativeArrayOf(LLVMOpaqueValue, indices)
            return LLVMConstInBoundsGEP(pointer.getLlvmValue(), indicesArray[0], indices.size)
        }
    }
}

internal fun ConstPointer.bitcast(toType: LLVMOpaqueType) = constPointer(LLVMConstBitCast(this.getLlvmValue(), toType))

internal class ConstArray(val elemType: LLVMOpaqueType?, val elements: List<ConstValue>) : ConstValue {

    override fun getLlvmValue(): LLVMOpaqueValue? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()

        memScoped {
            val valuesNativeArrayPtr = allocNativeArrayOf(LLVMOpaqueValue, *values)[0]

            return LLVMConstArray(elemType, valuesNativeArrayPtr, values.size)
        }
    }
}

internal open class Struct(val type: LLVMOpaqueType?, val elements: List<ConstValue>) : ConstValue {

    constructor(type: LLVMOpaqueType?, vararg elements: ConstValue) : this(type, elements.toList())

    override fun getLlvmValue(): LLVMOpaqueValue? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()
        memScoped {
            val valuesNativeArrayPtr = allocNativeArrayOf(LLVMOpaqueValue, *values)[0]
            return LLVMConstNamedStruct(type, valuesNativeArrayPtr, values.size)
        }
    }
}

internal class Int8(val value: Byte) : ConstValue {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt8Type(), value.toLong(), 1)
}

internal class Int32(val value: Int) : ConstValue {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt32Type(), value.toLong(), 1)
}

internal class Int64(val value: Long) : ConstValue {
    override fun getLlvmValue() = LLVMConstInt(LLVMInt64Type(), value, 1)
}

internal class Zero(val type: LLVMOpaqueType?) : ConstValue {
    override fun getLlvmValue() = LLVMConstNull(type)
}

internal class NullPointer(val pointeeType: LLVMOpaqueType?): ConstPointer {
    override fun getLlvmValue() = LLVMConstNull(pointerType(pointeeType))
}

internal fun constValue(value: LLVMOpaqueValue?) = object : ConstValue {
    override fun getLlvmValue() = value
}

internal val int8Type = LLVMInt8Type()
internal val int32Type = LLVMInt32Type()

internal fun pointerType(pointeeType: LLVMOpaqueType?) = LLVMPointerType(pointeeType, 0)

internal fun structType(vararg types: LLVMOpaqueType?): LLVMOpaqueType = memScoped {
    LLVMStructType(allocNativeArrayOf(LLVMOpaqueType, *types)[0], types.size, 0)!!
}

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
 * Reads [size] bytes contained in this array.
 */
internal fun NativeArray<Int8Box>.getBytes(size: Int) =
        (0 .. size-1).map { this[it].value }.toByteArray()

internal fun getFunctionType(ptrToFunction: LLVMOpaqueValue?): LLVMOpaqueType {
    val typeOfPtrToFunction = LLVMTypeOf(ptrToFunction)
    return LLVMGetElementType(typeOfPtrToFunction)!!
}