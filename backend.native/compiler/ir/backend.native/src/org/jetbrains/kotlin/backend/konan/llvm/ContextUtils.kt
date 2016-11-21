package org.jetbrains.kotlin.backend.konan.llvm

import kotlin_native.interop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.hash.GlobalHash
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

    val llvmTargetData: LLVMOpaqueTargetData
        get() = runtime.targetData

    val staticData: StaticData
        get() = context.staticData

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
    val FunctionDescriptor.llvmFunction: ConstValue
        get() {
            assert (this.kind.isReal)
            val globalName = this.symbolName
            val module = context.llvmModule

            val functionType = getLlvmFunctionType(this)
            val function = LLVMGetNamedFunction(module, globalName) ?: LLVMAddFunction(module, globalName, functionType)
            return constValue(function)
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val FunctionDescriptor.entryPointAddress: ConstValue
        get() {
            val result = LLVMConstBitCast(this.llvmFunction.getLlvmValue(), pointerType(LLVMInt8Type()))
            return constValue(result)
        }

    /**
     * Pointer to type info for given class.
     * It may be declared as pointer to external variable.
     */
    val ClassDescriptor.llvmTypeInfoPtr: ConstPointer
        get() {
            val module = context.llvmModule
            val globalName = this.typeInfoSymbolName
            val globalPtr = LLVMGetNamedGlobal(module, globalName) ?:
                            LLVMAddGlobal(module, runtime.typeInfoType, globalName)

            return constPointer(globalPtr)
        }

    /**
     * Returns contents of this [GlobalHash].
     *
     * It must be declared identically with [Runtime.globalHashType].
     */
    fun GlobalHash.getBytes(): ByteArray {
        val size = GlobalHash.size
        assert(size.toLong() == LLVMStoreSizeOfType(llvmTargetData, runtime.globalHashType))

        return this.bits.getBytes(size)
    }

    /**
     * Returns global hash of this string contents.
     */
    val String.globalHashBytes: ByteArray
        get() = memScoped {
            val hash = globalHash(stringAsBytes(this@globalHashBytes), memScope)
            hash.getBytes()
        }

    /**
     * Return base64 representation for global hash of this string contents.
     */
    val String.globalHashBase64: String
        get() {
            return base64Encode(globalHashBytes)
        }

    /**
     * Converts this string to the sequence of bytes to be used for hashing/storing to binary/etc.
     *
     * TODO: share this implementation
     */
    private fun stringAsBytes(str: String) = str.toByteArray(Charsets.UTF_8)

    val String.localHash: LocalHash
        get() = LocalHash(localHash(stringAsBytes(this)))

    val String.globalHash: ConstValue
        get() = memScoped {
            val hashBytes = this@globalHash.globalHashBytes
            return Struct(runtime.globalHashType, ConstArray(int8Type, hashBytes.map { Int8(it) }))
        }

    val FqName.globalHash: ConstValue
        get() = this.toString().globalHash

    val Name.localHash: LocalHash
        get() = this.toString().localHash

    val FqName.localHash: LocalHash
        get() = this.toString().localHash

    val pointerSize: Int
        get() = LLVMPointerSize(llvmTargetData)
}
