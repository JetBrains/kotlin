/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.hash.GlobalHash
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

internal enum class SlotType {
    // Frame local arena slot can be used.
    ARENA,
    // Return slot can be used.
    RETURN,
    // Return slot, if it is an arena, can be used.
    RETURN_IF_ARENA,
    // Anonymous slot.
    ANONYMOUS,
    // Unknown slot type.
    UNKNOWN
}

// Lifetimes class of reference, computed by escape analysis.
enum class Lifetime(val slotType: SlotType) {
    // If reference is frame-local (only obtained from some call and never leaves).
    LOCAL(SlotType.ARENA),
    // If reference is only returned.
    RETURN_VALUE(SlotType.RETURN),
    // If reference is set as field of references of class RETURN_VALUE or INDIRECT_RETURN_VALUE.
    INDIRECT_RETURN_VALUE(SlotType.RETURN_IF_ARENA),
    // If reference is stored to the field of an incoming parameters.
    PARAMETER_FIELD(SlotType.ANONYMOUS),
    // If reference refers to the global (either global object or global variable).
    GLOBAL(SlotType.ANONYMOUS),
    // If reference used to throw.
    THROW(SlotType.ANONYMOUS),
    // If reference used as an argument of outgoing function. Class can be improved by escape analysis
    // of called function.
    ARGUMENT(SlotType.ANONYMOUS),
    // If reference class is unknown.
    UNKNOWN(SlotType.UNKNOWN),
    // If reference class is irrelevant.
    IRRELEVANT(SlotType.UNKNOWN)
}

/**
 * Provides utility methods to the implementer.
 */
internal interface ContextUtils : RuntimeAware {
    val context: Context

    override val runtime: Runtime
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

    fun isExternal(descriptor: DeclarationDescriptor) = descriptor.module != context.ir.irModule.descriptor

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

            return if (isExternal(this)) {
                context.llvm.externalFunction(this.symbolName, getLlvmFunctionType(this))
            } else {
                context.llvmDeclarations.forFunction(this).llvmFunction
            }
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val FunctionDescriptor.entryPointAddress: ConstValue
        get() {
            val result = LLVMConstBitCast(this.llvmFunction, int8TypePtr)!!
            return constValue(result)
        }

    val ClassDescriptor.typeInfoPtr: ConstPointer
        get() {
            return if (isExternal(this)) {
                constPointer(importGlobal(this.typeInfoSymbolName, runtime.typeInfoType))
            } else {
                context.llvmDeclarations.forClass(this).typeInfo
            }
        }

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

    val String.globalHash: ConstValue
        get() = memScoped {
            val hashBytes = this@globalHash.globalHashBytes
            return Struct(runtime.globalHashType, ConstArray(int8Type, hashBytes.map { Int8(it) }))
        }

    val FqName.globalHash: ConstValue
        get() = this.toString().globalHash

}

/**
 * Converts this string to the sequence of bytes to be used for hashing/storing to binary/etc.
 */
internal fun stringAsBytes(str: String) = str.toByteArray(Charsets.UTF_8)

internal val String.localHash: LocalHash
    get() = LocalHash(localHash(stringAsBytes(this)))

internal val Name.localHash: LocalHash
    get() = this.toString().localHash

internal val FqName.localHash: LocalHash
    get() = this.toString().localHash

internal class Llvm(val context: Context, val llvmModule: LLVMModuleRef) {

    private fun importFunction(name: String, otherModule: LLVMModuleRef): LLVMValueRef {
        if (LLVMGetNamedFunction(llvmModule, name) != null) {
            throw IllegalArgumentException("function $name already exists")
        }

        val externalFunction = LLVMGetNamedFunction(otherModule, name)!!

        val functionType = getFunctionType(externalFunction)
        val function = LLVMAddFunction(llvmModule, name, functionType)!!
        val attributes = LLVMGetFunctionAttr(externalFunction)
        LLVMAddFunctionAttr(function, attributes)
        return function
    }

    private fun importMemset() : LLVMValueRef {
        val parameterTypes = cValuesOf(int8TypePtr, int8Type, int32Type, int32Type, int1Type)
        val functionType = LLVMFunctionType(LLVMVoidType(), parameterTypes, 5, 0)
        return LLVMAddFunction(llvmModule, "llvm.memset.p0i8.i32", functionType)!!
    }

    internal fun externalFunction(name: String, type: LLVMTypeRef): LLVMValueRef {
        val found = LLVMGetNamedFunction(llvmModule, name)
        if (found != null) {
            assert (getFunctionType(found) == type)
            assert (LLVMGetLinkage(found) == LLVMLinkage.LLVMExternalLinkage)
            return found
        } else {
            return LLVMAddFunction(llvmModule, name, type)!!
        }
    }

    private fun externalNounwindFunction(name: String, type: LLVMTypeRef): LLVMValueRef {
        val function = externalFunction(name, type)
        LLVMAddFunctionAttr(function, LLVMNoUnwindAttribute)
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
    val allocArrayFunction = importRtFunction("AllocArrayInstance")
    val initInstanceFunction = importRtFunction("InitInstance")
    val updateReturnRefFunction = importRtFunction("UpdateReturnRef")
    val setRefFunction = importRtFunction("SetRef")
    val updateRefFunction = importRtFunction("UpdateRef")
    val leaveFrameFunction = importRtFunction("LeaveFrame")
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
