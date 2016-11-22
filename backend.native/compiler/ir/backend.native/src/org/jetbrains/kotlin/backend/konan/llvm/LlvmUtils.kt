package org.jetbrains.kotlin.backend.konan.llvm

import kotlin_.cinterop.*
import llvm.*
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.utils.singletonOrEmptyList

/**
 * Represents the value which can be emitted as bitcode const value
 */
internal interface ConstValue {

    fun getLlvmValue(): LLVMValueRef?

    fun getLlvmType(): LLVMTypeRef {
        return LLVMTypeOf(getLlvmValue())!!
    }

}

internal interface ConstPointer : ConstValue {
    fun getElementPtr(index: Int): ConstPointer = ConstGetElementPtr(this, index)
}

internal fun constPointer(value: LLVMValueRef?) = object : ConstPointer {
    override fun getLlvmValue() = value
}

private class ConstGetElementPtr(val pointer: ConstPointer, val index: Int) : ConstPointer {
    override fun getLlvmValue(): LLVMValueRef? {
        // TODO: probably it should be computed once when initialized?
        // TODO: squash multiple GEPs
        val indices = intArrayOf(0, index).map { Int32(it).getLlvmValue() }
        memScoped {
            val indicesArray = allocArrayOf(indices)
            return LLVMConstInBoundsGEP(pointer.getLlvmValue(), indicesArray[0].ptr, indices.size)
        }
    }
}

internal fun ConstPointer.bitcast(toType: LLVMTypeRef) = constPointer(LLVMConstBitCast(this.getLlvmValue(), toType))

internal class ConstArray(val elemType: LLVMTypeRef?, val elements: List<ConstValue>) : ConstValue {

    override fun getLlvmValue(): LLVMValueRef? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()

        memScoped {
            val valuesNativeArrayPtr = allocArrayOf(*values)[0].ptr

            return LLVMConstArray(elemType, valuesNativeArrayPtr, values.size)
        }
    }
}

internal open class Struct(val type: LLVMTypeRef?, val elements: List<ConstValue>) : ConstValue {

    constructor(type: LLVMTypeRef?, vararg elements: ConstValue) : this(type, elements.toList())

    override fun getLlvmValue(): LLVMValueRef? {
        val values = elements.map { it.getLlvmValue() }.toTypedArray()
        memScoped {
            val valuesNativeArrayPtr = allocArrayOf(*values)[0].ptr
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

internal class Zero(val type: LLVMTypeRef?) : ConstValue {
    override fun getLlvmValue() = LLVMConstNull(type)
}

internal class NullPointer(val pointeeType: LLVMTypeRef?): ConstPointer {
    override fun getLlvmValue() = LLVMConstNull(pointerType(pointeeType))
}

internal fun constValue(value: LLVMValueRef?) = object : ConstValue {
    override fun getLlvmValue() = value
}

internal val int8Type = LLVMInt8Type()
internal val int32Type = LLVMInt32Type()

internal fun pointerType(pointeeType: LLVMTypeRef?) = LLVMPointerType(pointeeType, 0)

internal fun structType(vararg types: LLVMTypeRef?): LLVMTypeRef = memScoped {
    LLVMStructType(allocArrayOf(*types)[0].ptr, types.size, 0)!!
}

internal fun getLlvmFunctionType(function: FunctionDescriptor): LLVMTypeRef? {
    val returnType = getLLVMType(function.returnType!!)
    val params = function.dispatchReceiverParameter.singletonOrEmptyList() +
            function.extensionReceiverParameter.singletonOrEmptyList() +
            function.valueParameters

    var extraParam = listOf<LLVMTypeRef?>()
    if (function is ClassConstructorDescriptor) {
        extraParam += pointerType(LLVMInt8Type())
    }
    val paramTypes:List<LLVMTypeRef?> = params.map { getLLVMType(it.type) }
    extraParam += paramTypes

    if (extraParam.size == 0) return LLVMFunctionType(returnType, null, 0, 0)
    memScoped {
        val paramTypesPtr = allocArrayOf(extraParam)[0].ptr
        return LLVMFunctionType(returnType, paramTypesPtr, extraParam.size, 0)
    }
}

/**
 * Reads [size] bytes contained in this array.
 */
internal fun CArray<CInt8Var>.getBytes(size: Long) =
        (0 .. size-1).map { this[it].value }.toByteArray()

internal fun getFunctionType(ptrToFunction: LLVMValueRef?): LLVMTypeRef {
    val typeOfPtrToFunction = LLVMTypeOf(ptrToFunction)
    return LLVMGetElementType(typeOfPtrToFunction)!!
}
