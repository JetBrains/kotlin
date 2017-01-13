package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.Distribution
import org.jetbrains.kotlin.backend.konan.hash.GlobalHash
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.descriptors.backingField
import org.jetbrains.kotlin.backend.konan.descriptors.vtableSize
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

/**
 * Provides utility methods to the implementer.
 */
internal interface ContextUtils {
    val context: Context

    val runtime: Runtime
        get() = context.llvm.runtime

    /**
     * Describes the target platform.
     *
     * TODO: using [llvmTargetData] usually results in generating non-portable bitcode.
     */
    val llvmTargetData: LLVMTargetDataRef
        get() = runtime.targetData

    val staticData: StaticData
        get() = context.llvm.staticData

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
            // TODO: Here's what is going on here:
            // The existence of a backing field for a property is only described in the IR, 
            // but not in the property descriptor.
            // That works, while we process the IR, but not for deserialized descriptors.
            //
            // So to have something in deserialized descriptors, 
            // while we still see the IR, we mark the property with an annotation.
            //
            // We could apply the annotation during IR rewite, but we still are not
            // that far in the rewriting infrastructure. So we postpone
            // the annotation until the serializer.
            //
            // In this function we check the presence of the backing filed
            // two ways: first we check IR, then we check the annotation.

            val irClass = context.ir.moduleIndexForCodegen.classes[this.classId]
            if (irClass != null) {
                val declarations = irClass.declarations

                return declarations.mapNotNull {
                    when (it) {
                        is IrProperty -> it.backingField?.descriptor
                        is IrField -> it.descriptor
                        else -> null
                    }
                }
            } else {
                val properties = this.unsubstitutedMemberScope.
                    getContributedDescriptors().
                    filterIsInstance<PropertyDescriptor>()

                return properties.mapNotNull{ it.backingField }
            }
        }

    /**
     * LLVM function generated from the Kotlin function.
     * It may be declared as external function prototype.
     */
    val FunctionDescriptor.llvmFunction: LLVMValueRef
        get() {
            assert (this.kind.isReal)
            if (this is TypeAliasConstructorDescriptor) {
                return this.underlyingConstructorDescriptor.llvmFunction
            }
            val globalName = this.symbolName
            val module = context.llvmModule

            val functionType = getLlvmFunctionType(this)

            return LLVMGetNamedFunction(module, globalName) ?:
                    LLVMAddFunction(module, globalName, functionType)!!
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val FunctionDescriptor.entryPointAddress: ConstValue
        get() {
            val result = LLVMConstBitCast(this.llvmFunction, int8TypePtr)!!
            return constValue(result)
        }

    /**
     * Pointer to struct { TypeInfo, vtable }.
     */
    val ClassDescriptor.typeInfoWithVtable: ConstPointer
        get() {
            val type = structType(runtime.typeInfoType, LLVMArrayType(int8TypePtr, this.vtableSize)!!)
            return constPointer(externalGlobal(this.typeInfoSymbolName, type))
        }

    val ClassDescriptor.typeInfoPtr: ConstPointer
        get() = typeInfoWithVtable.getElementPtr(0)

    /**
     * Pointer to type info for given class.
     * It may be declared as pointer to external variable.
     */
    val ClassDescriptor.llvmTypeInfoPtr: LLVMValueRef
        get() = typeInfoPtr.llvm

    /**
     * Pointer to type info for this type, or `null` if the type doesn't have corresponding type info.
     */
    val KotlinType.typeInfoPtr: ConstPointer?
        get() = TypeUtils.getClassDescriptor(this)?.typeInfoPtr

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
}

internal class Llvm(val context: Context, val llvmModule: LLVMModuleRef) {

    private fun importFunction(name: String, otherModule: LLVMModuleRef): LLVMValueRef {
        if (LLVMGetNamedFunction(llvmModule, name) != null) {
            throw IllegalArgumentException("function $name already exists")
        }

        val externalFunction = LLVMGetNamedFunction(otherModule, name)!!

        val functionType = getFunctionType(externalFunction)
        val function = LLVMAddFunction(llvmModule, name, functionType)!!
        val attributes = LLVMGetFunctionAttrSet(externalFunction)
        for (attribute in LLVMAttribute.values()) {
            if (attribute in attributes) {
                LLVMAddFunctionAttr(function, attribute)
            }
        }
        return function
    }

    private fun importMemset() : LLVMValueRef {
        memScoped {
            val parameterTypes = allocArrayOf(int8TypePtr, int8Type, int32Type, int32Type, int1Type)
            val functionType = LLVMFunctionType(LLVMVoidType(), parameterTypes[0].ptr, 5, 0)
            return LLVMAddFunction(llvmModule, "llvm.memset.p0i8.i32", functionType)!!
        }
    }

    private fun externalFunction(name: String, type: LLVMTypeRef): LLVMValueRef {
        val found = LLVMGetNamedFunction(context.llvmModule, name)
        if (found != null) {
            assert (getFunctionType(found) == type)
            return found
        } else {
            return LLVMAddFunction(context.llvmModule, name, type)!!
        }
    }

    private fun externalNounwindFunction(name: String, type: LLVMTypeRef): LLVMValueRef {
        val function = externalFunction(name, type)
        LLVMAddFunctionAttr(function, LLVMAttribute.LLVMNoUnwindAttribute)
        return function
    }

    val staticData = StaticData(context)

    val runtimeFile = context.config.distribution.runtime
    val runtime = Runtime(runtimeFile) // TODO: dispose

    init {
        LLVMSetDataLayout(llvmModule, runtime.dataLayout)
        LLVMSetTarget(llvmModule, runtime.target)
    }

    private fun importRtFunction(name: String) = importFunction(name, runtime.llvmModule)

    var globalInitIndex:Int = 0

    val allocInstanceFunction = importRtFunction("AllocInstance")
    val initInstanceFunction = importRtFunction("InitInstance")
    val allocArrayFunction = importRtFunction("AllocArrayInstance")
    val setLocalRefFunction = importRtFunction("SetLocalRef")
    val setGlobalRefFunction = importRtFunction("SetGlobalRef")
    val updateLocalRefFunction = importRtFunction("UpdateLocalRef")
    val updateGlobalRefFunction = importRtFunction("UpdateGlobalRef")
    val releaseLocalRefsFunction = importRtFunction("ReleaseLocalRefs")
    val setArrayFunction = importRtFunction("Kotlin_Array_set")
    val copyImplArrayFunction = importRtFunction("Kotlin_Array_copyImpl")
    val lookupFieldOffset = importRtFunction("LookupFieldOffset")
    val lookupOpenMethodFunction = importRtFunction("LookupOpenMethod")
    val isInstanceFunction = importRtFunction("IsInstance")
    val checkInstanceFunction = importRtFunction("CheckInstance")
    val throwExceptionFunction = importRtFunction("ThrowException")
    val appendToInitalizersTail = importRtFunction("AppendToInitializersTail")

    val gxxPersonalityFunction = externalNounwindFunction("__gxx_personality_v0", functionType(int32Type, true))
    val cxaBeginCatchFunction = externalNounwindFunction("__cxa_begin_catch", functionType(int8TypePtr, false, int8TypePtr))
    val cxaEndCatchFunction = externalNounwindFunction("__cxa_end_catch", functionType(voidType, false))

    val memsetFunction = importMemset()

    val usedFunctions = mutableListOf<LLVMValueRef>()
    val staticInitializers = mutableListOf<LLVMValueRef>()
    val fileInitializers = mutableListOf<IrElement>()
}
