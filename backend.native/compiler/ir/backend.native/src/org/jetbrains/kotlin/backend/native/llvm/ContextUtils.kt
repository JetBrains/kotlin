package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.backend.native.hash.GlobalHash
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

/**
 * Provides utility methods to the implementer.
 */
internal interface ContextUtils {
    val context: Context

    val runtime: Runtime
        get() = context.runtime

    val llvmTargetData: LLVMOpaqueTargetData?
        get() = runtime.targetData

    /**
     * All fields of the class instance.
     * The order respects the class hierarchy, i.e. a class [fields] contains superclass [fields] as a prefix.
     */
    val ClassDescriptor.fields: List<PropertyDescriptor>
        get() {
            val superClass = this.getSuperClassNotAny() // TODO: what if Any has fields?
            val superFields = if (superClass != null) superClass.fields else emptyList()

            return superFields + this.declaredFields
        }

    /**
     * Fields declared in the class.
     */
    val ClassDescriptor.declaredFields: List<PropertyDescriptor>
        get() {
            // TODO: do not use IR to get fields
            val irClass = context.moduleIndex.classes[this.classId] ?:
                          throw IllegalArgumentException("Class ${this.fqNameSafe} is not found in current module")

            return irClass.declarations.mapNotNull { (it as? IrProperty)?.backingField?.descriptor }
        }

    /**
     * LLVM function generated from the Kotlin function.
     * It may be declared as external function prototype.
     */
    val FunctionDescriptor.llvmFunction: CompileTimeValue
        get() {
            assert (this.kind.isReal)
            val globalName = this.symbolName
            val module = context.llvmModule

            val functionType = getLlvmFunctionType(this)
            val function = LLVMGetNamedFunction(module, globalName) ?: LLVMAddFunction(module, globalName, functionType)
            return compileTimeValue(function)
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val FunctionDescriptor.entryPointAddress: CompileTimeValue
        get() {
            val result = LLVMConstBitCast(this.llvmFunction.getLlvmValue(), pointerType(LLVMInt8Type()))
            return compileTimeValue(result)
        }

    /**
     * Pointer to type info for given class.
     * It may be declared as pointer to external variable.
     */
    val ClassDescriptor.llvmTypeInfoPtr: CompileTimeValue
        get() {
            val module = context.llvmModule
            val globalName = this.typeInfoSymbolName
            val globalPtr = LLVMGetNamedGlobal(module, globalName) ?:
                            LLVMAddGlobal(module, runtime.typeInfoType, globalName)

            return compileTimeValue(globalPtr)
        }

    /**
     * Adds global variable with given name and value to [Context.llvmModule] of [context].
     * Returns pointer to this variable.
     */
    fun addGlobalConst(name: String, value: CompileTimeValue): CompileTimeValue {
        val global = LLVMAddGlobal(context.llvmModule, value.getLlvmType(), name)
        LLVMSetInitializer(global, value.getLlvmValue())
        LLVMSetGlobalConstant(global, 1)
        return compileTimeValue(global)
    }

    /**
     * Returns pointer to first element of given array.
     *
     * Note: this function doesn't depend on the context
     *
     * @param arrayPtr pointer to array
     */
    private fun getPtrToFirstElem(arrayPtr: CompileTimeValue): CompileTimeValue {
        val indices = longArrayOf(0, 0).map { LLVMConstInt(LLVMInt32Type(), it, 0) }.toTypedArray()

        memScoped {
            val indicesNativeArrayPtr = allocNativeArrayOf(LLVMOpaqueValue, *indices)[0]

            return compileTimeValue(LLVMConstGEP(arrayPtr.getLlvmValue(), indicesNativeArrayPtr, indices.size))
        }
    }

    /**
     * Adds global array-typed variable with given name and value to [Context.llvmModule] of [context].
     * Returns pointer to the first element of the global array.
     */
    fun addGlobalConstArray(name: String, elemType: LLVMOpaqueType?, elements: List<CompileTimeValue>): CompileTimeValue {
        return if (elements.size > 0) {
            getPtrToFirstElem(addGlobalConst(name, ConstArray(elemType, elements)))
        } else {
            Zero(pointerType(elemType))
        }
    }

    /**
     * Converts this [GlobalHash] to compile-time value of [runtime]-defined `GlobalHash` type.
     *
     * These types must be defined identically.
     */
    private fun GlobalHash.asCompileTimeValue(): Struct {
        val size = GlobalHash.size
        assert(size.toLong() == LLVMStoreSizeOfType(llvmTargetData, runtime.globalhHashType))

        return Struct(runtime.globalhHashType, bits.asCompileTimeValue(size))
        // TODO: implement such transformation more generally using LLVM bitcast
        // (seems to require some investigation)
    }

    /**
     * Converts this string to the sequence of bytes to be used for hashing/storing to binary/etc.
     *
     * TODO: share this implementation
     */
    private fun stringAsBytes(str: String) = str.toByteArray(Charsets.UTF_8)

    val String.localHash: LocalHash
        get() = LocalHash(localHash(stringAsBytes(this)))

    val String.globalHash: CompileTimeValue
        get() = memScoped {
            val hash = globalHash(stringAsBytes(this@globalHash), memScope)
            hash.asCompileTimeValue()
        }

    val FqName.globalHash: CompileTimeValue
        get() = this.toString().globalHash

    val Name.localHash: LocalHash
        get() = this.toString().localHash

    val FqName.localHash: LocalHash
        get() = this.toString().localHash

    val pointerSize: Int
        get() = LLVMPointerSize(llvmTargetData)
}