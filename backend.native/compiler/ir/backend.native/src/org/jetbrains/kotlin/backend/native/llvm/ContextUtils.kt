package org.jetbrains.kotlin.backend.native.llvm

import kotlin_native.interop.mallocNativeArrayOf
import llvm.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny

/**
 * Provides utility methods to the implementer.
 */
internal interface ContextUtils {
    val context: Context

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
                            LLVMAddGlobal(module, context.runtime.typeInfoType, globalName)

            return compileTimeValue(globalPtr)
        }

    /**
     * Adds global variable with given name and value to [Context.llvmModule] of [context].
     * Returns pointer to this variable.
     */
    fun addGlobalVar(name: String, value: CompileTimeValue): CompileTimeValue {
        val global = LLVMAddGlobal(context.llvmModule, value.getLlvmType(), name)
        LLVMSetInitializer(global, value.getLlvmValue())
        return compileTimeValue(global)
    }

    /**
     * Returns pointer to first element of given array.
     *
     * @param arrayPtr pointer to array
     */
    private fun getPtrToFirstElem(arrayPtr: CompileTimeValue): CompileTimeValue {
        val indices = longArrayOf(0, 0).map { LLVMConstInt(LLVMInt32Type(), it, 0) }.toTypedArray()
        val indicesNativeArrayPtr = mallocNativeArrayOf(LLVMOpaqueValue, *indices)[0] // TODO: dispose

        return compileTimeValue(LLVMBuildGEP(context.llvmBuilder, arrayPtr.getLlvmValue(), indicesNativeArrayPtr, indices.size, ""))
    }

    /**
     * Adds global array-typed variable with given name and value to [Context.llvmModule] of [context].
     * Returns pointer to the first element of the global array.
     */
    fun addGlobalArray(name: String, elemType: LLVMOpaqueType?, elements: List<CompileTimeValue>): CompileTimeValue {
        return if (elements.size > 0) {
            getPtrToFirstElem(addGlobalVar(name, ConstArray(elemType, elements)))
        } else {
            Zero(pointerType(elemType))
        }
    }
}