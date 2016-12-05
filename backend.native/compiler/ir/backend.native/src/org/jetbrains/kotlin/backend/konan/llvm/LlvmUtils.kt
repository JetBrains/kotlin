package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.allValueParameters
import org.jetbrains.kotlin.descriptors.FunctionDescriptor

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
    override val llvm = memScoped {
        // TODO: squash multiple GEPs
        val indices = intArrayOf(0, index).map { Int32(it).llvm }
        val indicesArray = allocArrayOf(indices)
        LLVMConstInBoundsGEP(pointer.llvm, indicesArray[0].ptr, indices.size)!!
    }
}

internal fun ConstPointer.bitcast(toType: LLVMTypeRef) = constPointer(LLVMConstBitCast(this.llvm, toType)!!)

internal class ConstArray(val elemType: LLVMTypeRef?, val elements: List<ConstValue>) : ConstValue {

    override val llvm = memScoped {
        val values = elements.map { it.llvm }.toTypedArray()

        val valuesNativeArrayPtr = allocArrayOf(*values)[0].ptr
        LLVMConstArray(elemType, valuesNativeArrayPtr, values.size)!!
    }
}

internal open class Struct(val type: LLVMTypeRef?, val elements: List<ConstValue>) : ConstValue {

    constructor(type: LLVMTypeRef?, vararg elements: ConstValue) : this(type, elements.toList())

    constructor(vararg elements: ConstValue) : this(structType(elements.map { it.llvmType }), *elements)

    override val llvm = memScoped {
        val values = elements.map { it.llvm }.toTypedArray()
        val valuesNativeArrayPtr = allocArrayOf(*values)[0].ptr
        LLVMConstNamedStruct(type, valuesNativeArrayPtr, values.size)!!
    }
}

internal class Int8(val value: Byte) : ConstValue {
    override val llvm = LLVMConstInt(LLVMInt8Type(), value.toLong(), 1)!!
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

internal val int8Type = LLVMInt8Type()!!
internal val int32Type = LLVMInt32Type()!!
internal val int8TypePtr = pointerType(int8Type)

internal val voidType = LLVMVoidType()!!

internal val ContextUtils.kTheAnyTypeInfo: LLVMValueRef
    get() = KonanPlatform.builtIns.any.llvmTypeInfoPtr
internal val ContextUtils.kTheArrayTypeInfo: LLVMValueRef
    get() = KonanPlatform.builtIns.array.llvmTypeInfoPtr
internal val ContextUtils.kTypeInfo: LLVMTypeRef
    get() = LLVMGetTypeByName(context.llvmModule, "struct.TypeInfo")!!
internal val ContextUtils.kObjHeader: LLVMTypeRef
    get() = LLVMGetTypeByName(context.llvmModule, "struct.ObjHeader")!!
internal val ContextUtils.kObjHeaderPtr: LLVMTypeRef
    get() = pointerType(kObjHeader)
internal val ContextUtils.kObjHeaderPtrPtr: LLVMTypeRef
    get() = pointerType(kObjHeaderPtr)
internal val ContextUtils.kTypeInfoPtr: LLVMTypeRef
    get() = pointerType(kTypeInfo)
internal val kInt1         = LLVMInt1Type()!!
internal val kInt8Ptr      = pointerType(int8Type)
internal val kInt8PtrPtr   = pointerType(kInt8Ptr)
internal val kImmInt32One  = Int32(1).llvm
internal val kImmInt64One  = Int64(1).llvm
internal val ContextUtils.kNullObjHeaderPtr: LLVMValueRef
    get() = LLVMConstNull(this.kObjHeaderPtr)!!

internal fun pointerType(pointeeType: LLVMTypeRef) = LLVMPointerType(pointeeType, 0)!!

internal fun structType(vararg types: LLVMTypeRef): LLVMTypeRef = structType(types.toList())

internal fun structType(types: List<LLVMTypeRef>): LLVMTypeRef = memScoped {
    LLVMStructType(allocArrayOf(types)[0].ptr, types.size, 0)!!
}

internal fun ContextUtils.getLlvmFunctionType(function: FunctionDescriptor): LLVMTypeRef {
    val returnType = getLLVMType(function.returnType!!)
    val params = function.allValueParameters
    val paramTypes = params.map { getLLVMType(it.type) }

    memScoped {
        val paramTypesPtr = allocArrayOf(paramTypes)[0].ptr
        return LLVMFunctionType(returnType, paramTypesPtr, paramTypes.size, 0)!!
    }
}

/**
 * Reads [size] bytes contained in this array.
 */
internal fun CArray<CInt8Var>.getBytes(size: Long) =
        (0 .. size-1).map { this[it].value }.toByteArray()

internal fun getFunctionType(ptrToFunction: LLVMValueRef): LLVMTypeRef {
    return getGlobalType(ptrToFunction)
}

internal fun getGlobalType(ptrToGlobal: LLVMValueRef): LLVMTypeRef {
    return LLVMGetElementType(ptrToGlobal.type)!!
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

internal fun functionType(returnType: LLVMTypeRef, isVarArg: Boolean = false, vararg paramTypes: LLVMTypeRef) =
        memScoped {
            val paramTypesPtr = allocArrayOf(*paramTypes)[0].ptr
            LLVMFunctionType(returnType, paramTypesPtr, paramTypes.size, if (isVarArg) 1 else 0)!!
        }