package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.allValueParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

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

internal val int8Type = LLVMInt8Type()!!
internal val int32Type = LLVMInt32Type()!!
internal val int8TypePtr = pointerType(int8Type)

internal val voidType = LLVMVoidType()

internal val ContextUtils.kTypeInfo: LLVMTypeRef
    get() = LLVMGetTypeByName(context.llvmModule, "struct.TypeInfo")!!
internal val ContextUtils.kObjHeader: LLVMTypeRef
    get() = LLVMGetTypeByName(context.llvmModule, "struct.ObjHeader")!!
internal val ContextUtils.kObjHeaderPtr: LLVMTypeRef
    get() = pointerType(kObjHeader)
internal val ContextUtils.kObjHeaderPtrPtr: LLVMTypeRef
    get() = pointerType(kObjHeaderPtr)
internal val ContextUtils.kArrayHeader: LLVMTypeRef
    get() = LLVMGetTypeByName(context.llvmModule, "struct.ArrayHeader")!!
internal val ContextUtils.kArrayHeaderPtr: LLVMTypeRef
    get() = pointerType(kArrayHeader)
internal val ContextUtils.kTypeInfoPtr: LLVMTypeRef
    get() = pointerType(kTypeInfo)
internal val kInt1         = LLVMInt1Type()
internal val kInt8Ptr      = pointerType(LLVMInt8Type())
internal val kInt8PtrPtr   = pointerType(kInt8Ptr)
internal val ContextUtils.kNullObjHeaderPtr: LLVMValueRef
    get() = LLVMConstNull(this.kObjHeaderPtr)!!
internal val ContextUtils.kNullArrayHeaderPtr: LLVMValueRef
    get() = LLVMConstNull(this.kArrayHeaderPtr)!!


internal fun pointerType(pointeeType: LLVMTypeRef?) = LLVMPointerType(pointeeType, 0)!!

internal fun structType(vararg types: LLVMTypeRef?): LLVMTypeRef = memScoped {
    LLVMStructType(allocArrayOf(*types)[0].ptr, types.size, 0)!!
}

internal fun ContextUtils.getLlvmFunctionType(function: FunctionDescriptor): LLVMTypeRef? {
    val returnType = getLLVMType(function.returnType!!)
    val params = function.allValueParameters
    val paramTypes = params.map { getLLVMType(it.type) }

    memScoped {
        val paramTypesPtr = allocArrayOf(paramTypes)[0].ptr
        return LLVMFunctionType(returnType, paramTypesPtr, paramTypes.size, 0)
    }
}

/**
 * Reads [size] bytes contained in this array.
 */
internal fun CArray<CInt8Var>.getBytes(size: Long) =
        (0 .. size-1).map { this[it].value }.toByteArray()

internal fun getFunctionType(ptrToFunction: LLVMValueRef?): LLVMTypeRef {
    return getGlobalType(ptrToFunction)
}

internal fun getGlobalType(ptrToGlobal: LLVMValueRef?): LLVMTypeRef {
    return LLVMGetElementType(LLVMTypeOf(ptrToGlobal))!!
}

internal fun ContextUtils.externalFunction(name: String, type: LLVMTypeRef): LLVMValueRef {
    val found = LLVMGetNamedFunction(context.llvmModule, name)
    if (found != null) {
        assert (getFunctionType(found) == type)
        return found
    } else {
        return LLVMAddFunction(context.llvmModule, name, type)!!
    }
}

internal fun ContextUtils.externalGlobal(name: String, type: LLVMTypeRef): LLVMValueRef {
    val found = LLVMGetNamedGlobal(context.llvmModule, name)
    if (found != null) {
        assert (getGlobalType(found) == type)
        return found
    } else {
        return LLVMAddGlobal(context.llvmModule, type, name)!!
    }
}

internal fun functionType(returnType: LLVMTypeRef?, isVarArg: Boolean = false, vararg paramTypes: LLVMTypeRef?) =
        memScoped {
            val paramTypesPtr = allocArrayOf(*paramTypes)[0].ptr
            LLVMFunctionType(returnType, paramTypesPtr, paramTypes.size, if (isVarArg) 1 else 0)!!
        }