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
import org.jetbrains.kotlin.backend.konan.descriptors.CurrentKonanModule
import org.jetbrains.kotlin.backend.konan.descriptors.DeserializedKonanModule
import org.jetbrains.kotlin.backend.konan.descriptors.LlvmSymbolOrigin
import org.jetbrains.kotlin.backend.konan.descriptors.findPackage
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.backend.konan.hash.GlobalHash
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.backend.konan.isNativeBinary
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.backend.konan.library.withResolvedDependencies
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal sealed class SlotType {
    // Frame local arena slot can be used.
    class ARENA: SlotType()
    // Return slot can be used.
    class RETURN: SlotType()
    // Return slot, if it is an arena, can be used.
    class RETURN_IF_ARENA: SlotType()
    // Return slot, if it is an arena, can be used.
    class PARAM_IF_ARENA(val parameter: Int): SlotType()
    // Anonymous slot.
    class ANONYMOUS: SlotType()
    // Unknown slot type.
    class UNKNOWN: SlotType()

    companion object {
        val ARENA = ARENA()
        val RETURN = RETURN()
        val RETURN_IF_ARENA = RETURN_IF_ARENA()
        val ANONYMOUS = ANONYMOUS()
        val UNKNOWN = UNKNOWN()
    }
}

// Lifetimes class of reference, computed by escape analysis.
internal sealed class Lifetime(val slotType: SlotType) {
    // If reference is frame-local (only obtained from some call and never leaves).
    class LOCAL: Lifetime(SlotType.ARENA) {
        override fun toString(): String {
            return "LOCAL"
        }
    }

    // If reference is only returned.
    class RETURN_VALUE: Lifetime(SlotType.RETURN) {
        override fun toString(): String {
            return "RETURN_VALUE"
        }
    }

    // If reference is set as field of references of class RETURN_VALUE or INDIRECT_RETURN_VALUE.
    class INDIRECT_RETURN_VALUE: Lifetime(SlotType.RETURN_IF_ARENA) {
        override fun toString(): String {
            return "INDIRECT_RETURN_VALUE"
        }
    }

    // If reference is stored to the field of an incoming parameters.
    class PARAMETER_FIELD(val parameter: Int): Lifetime(SlotType.PARAM_IF_ARENA(parameter)) {
        override fun toString(): String {
            return "PARAMETER_FIELD($parameter)"
        }
    }

    // If reference refers to the global (either global object or global variable).
    class GLOBAL: Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "GLOBAL"
        }
    }

    // If reference used to throw.
    class THROW: Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "THROW"
        }
    }

    // If reference used as an argument of outgoing function. Class can be improved by escape analysis
    // of called function.
    class ARGUMENT: Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "ARGUMENT"
        }
    }

    // If reference class is unknown.
    class UNKNOWN: Lifetime(SlotType.UNKNOWN) {
        override fun toString(): String {
            return "UNKNOWN"
        }
    }

    // If reference class is irrelevant.
    class IRRELEVANT: Lifetime(SlotType.UNKNOWN) {
        override fun toString(): String {
            return "IRRELEVANT"
        }
    }

    companion object {
        val LOCAL = LOCAL()
        val RETURN_VALUE = RETURN_VALUE()
        val INDIRECT_RETURN_VALUE = INDIRECT_RETURN_VALUE()
        val GLOBAL = GLOBAL()
        val THROW = THROW()
        val ARGUMENT = ARGUMENT()
        val UNKNOWN = UNKNOWN()
        val IRRELEVANT = IRRELEVANT()
    }
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

    fun isExternal(descriptor: DeclarationDescriptor): Boolean {
        val pkg = descriptor.findPackage()
        return when (pkg) {
            is IrFile -> false
            is IrExternalPackageFragment -> true
            else -> error(pkg)
        }
    }

    /**
     * LLVM function generated from the Kotlin function.
     * It may be declared as external function prototype.
     */
    val FunctionDescriptor.llvmFunction: LLVMValueRef
        get() {
            assert (this.isReal)

            return if (isExternal(this)) {
                context.llvm.externalFunction(this.symbolName, getLlvmFunctionType(this),
                        origin = this.llvmSymbolOrigin)
            } else {
                context.llvmDeclarations.forFunction(this).llvmFunction
            }
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val FunctionDescriptor.entryPointAddress: ConstPointer
        get() {
            val result = LLVMConstBitCast(this.llvmFunction, int8TypePtr)!!
            return constPointer(result)
        }

    val ClassDescriptor.typeInfoPtr: ConstPointer
        get() {
            return if (isExternal(this)) {
                constPointer(importGlobal(this.typeInfoSymbolName, runtime.typeInfoType,
                        origin = this.llvmSymbolOrigin))
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

        copyFunctionAttributes(externalFunction, function)

        return function
    }

    private fun copyFunctionAttributes(source: LLVMValueRef, destination: LLVMValueRef) {
        // TODO: consider parameter attributes
        val attributeIndex = LLVMAttributeFunctionIndex
        val count = LLVMGetAttributeCountAtIndex(source, attributeIndex)
        memScoped {
            val attributes = allocArray<LLVMAttributeRefVar>(count)
            LLVMGetAttributesAtIndex(source, attributeIndex, attributes)
            (0 until count).forEach {
                LLVMAddAttributeAtIndex(destination, attributeIndex, attributes[it])
            }
        }
    }

    private fun importMemset() : LLVMValueRef {
        val parameterTypes = cValuesOf(int8TypePtr, int8Type, int32Type, int32Type, int1Type)
        val functionType = LLVMFunctionType(LLVMVoidType(), parameterTypes, 5, 0)
        return LLVMAddFunction(llvmModule, "llvm.memset.p0i8.i32", functionType)!!
    }

    internal fun externalFunction(name: String, type: LLVMTypeRef, origin: LlvmSymbolOrigin): LLVMValueRef {
        this.imports.add(origin)

        val found = LLVMGetNamedFunction(llvmModule, name)
        if (found != null) {
            assert (getFunctionType(found) == type)
            assert (LLVMGetLinkage(found) == LLVMLinkage.LLVMExternalLinkage)
            return found
        } else {
            return LLVMAddFunction(llvmModule, name, type)!!
        }
    }

    private fun externalNounwindFunction(name: String, type: LLVMTypeRef, origin: LlvmSymbolOrigin): LLVMValueRef {
        val function = externalFunction(name, type, origin)
        setFunctionNoUnwind(function)
        return function
    }

    private val usedLibraries = mutableSetOf<LibraryReaderImpl>()

    val imports = object : LlvmImports {

        private val allLibraries = context.librariesWithDependencies.toSet()

        override fun add(origin: LlvmSymbolOrigin) {
            val reader = when (origin) {
                CurrentKonanModule -> return
                is DeserializedKonanModule -> origin.reader
            }

            if (reader !in allLibraries) {
                error("$reader (${reader.libraryName}) is used but not requested")
            }

            usedLibraries.add(reader as LibraryReaderImpl)
        }
    }

    val librariesToLink: List<KonanLibraryReader>  by lazy {
        context.config.immediateLibraries
                .filter { (!it.isDefaultLibrary && !context.config.purgeUserLibs) || it in usedLibraries }
                .withResolvedDependencies()
                .topoSort()
    }

    private fun List<LibraryReaderImpl>.topoSort(): List<LibraryReaderImpl> {
        var sorted = mutableListOf<LibraryReaderImpl>()
        val visited = mutableSetOf<LibraryReaderImpl>()
        val tempMarks = mutableSetOf<LibraryReaderImpl>()

        fun visit(node: LibraryReaderImpl, result: MutableList<LibraryReaderImpl>) {
            if (visited.contains(node)) return
            if (tempMarks.contains(node)) error("Cyclic dependency in library graph.")
            tempMarks.add(node)
            node.resolvedDependencies.forEach {
                visit(it, result)
            }
            visited.add(node)
            result += node
        }

        this.forEach next@{
            if (visited.contains(it)) return@next
            visit(it, sorted)
        }
        return sorted
    }

    val librariesForLibraryManifest: List<KonanLibraryReader> get() {
        // Note: library manifest should contain the list of all user libraries and frontend-used default libraries.
        // However this would result into linking too many default libraries into the application which uses current
        // library. This problem should probably be fixed by adding different kind of dependencies to library
        // manifest.
        // Currently the problem is workarounded like this:
        return this.librariesToLink
        // This list contains all user libraries and the default libraries required for link (not frontend).
        // That's why the workaround doesn't work only in very special cases, e.g. when `-nodefaultlibs` is enabled
        // when compiling the application, while the library API uses types from default libs.
    }

    val staticData = StaticData(context)

    private val target = context.config.target

    val runtimeFile = context.config.distribution.runtime(target)
    val runtime = Runtime(runtimeFile) // TODO: dispose

    init {
        LLVMSetDataLayout(llvmModule, runtime.dataLayout)
        LLVMSetTarget(llvmModule, runtime.target)
    }

    private fun importRtFunction(name: String) = importFunction(name, runtime.llvmModule)

    val allocInstanceFunction = importRtFunction("AllocInstance")
    val allocArrayFunction = importRtFunction("AllocArrayInstance")
    val initInstanceFunction = importRtFunction("InitInstance")
    val initSharedInstanceFunction = importRtFunction("InitSharedInstance")
    val updateReturnRefFunction = importRtFunction("UpdateReturnRef")
    val updateRefFunction = importRtFunction("UpdateRef")
    val enterFrameFunction = importRtFunction("EnterFrame")
    val leaveFrameFunction = importRtFunction("LeaveFrame")
    val getReturnSlotIfArenaFunction = importRtFunction("GetReturnSlotIfArena")
    val getParamSlotIfArenaFunction = importRtFunction("GetParamSlotIfArena")
    val lookupOpenMethodFunction = importRtFunction("LookupOpenMethod")
    val isInstanceFunction = importRtFunction("IsInstance")
    val checkInstanceFunction = importRtFunction("CheckInstance")
    val throwExceptionFunction = importRtFunction("ThrowException")
    val appendToInitalizersTail = importRtFunction("AppendToInitializersTail")
    val initRuntimeIfNeeded = importRtFunction("Kotlin_initRuntimeIfNeeded")
    val mutationCheck = importRtFunction("MutationCheck")

    val createKotlinObjCClass by lazy { importRtFunction("CreateKotlinObjCClass") }
    val getObjCKotlinTypeInfo by lazy { importRtFunction("GetObjCKotlinTypeInfo") }
    val missingInitImp by lazy { importRtFunction("MissingInitImp") }

    val Kotlin_ObjCExport_refToObjC by lazyRtFunction
    val Kotlin_ObjCExport_refFromObjC by lazyRtFunction
    val Kotlin_Interop_CreateNSArrayFromKList by lazyRtFunction
    val Kotlin_Interop_CreateNSMutableArrayFromKList by lazyRtFunction
    val Kotlin_Interop_CreateNSSetFromKSet by lazyRtFunction
    val Kotlin_Interop_CreateKotlinMutableSetFromKSet by lazyRtFunction
    val Kotlin_Interop_CreateNSDictionaryFromKMap by lazyRtFunction
    val Kotlin_Interop_CreateKotlinMutableDictonaryFromKMap by lazyRtFunction
    val Kotlin_ObjCExport_convertUnit by lazyRtFunction
    val Kotlin_ObjCExport_GetAssociatedObject by lazyRtFunction
    val Kotlin_ObjCExport_AbstractMethodCalled by lazyRtFunction
    val Kotlin_ObjCExport_RethrowExceptionAsNSError by lazyRtFunction
    val Kotlin_ObjCExport_RethrowNSErrorAsException by lazyRtFunction

    val objCExportEnabled = if (context.config.produce.isNativeBinary) {
        // Note: this defines the global declared in runtime (if any).
        staticData.placeGlobal("objCExportEnabled", Int8(0), isExported = true).also {
            it.setConstant(true)
        }
    } else {
        null
    }

    val tlsMode by lazy {
        when (target) {
            KonanTarget.WASM32, 
            is KonanTarget.ZEPHYR 
                -> LLVMThreadLocalMode.LLVMNotThreadLocal
            else 
                -> LLVMThreadLocalMode.LLVMGeneralDynamicTLSModel
        }
    }

    private val personalityFunctionName = when (target) {
        KonanTarget.MINGW_X64 -> "__gxx_personality_seh0"
        else -> "__gxx_personality_v0"
    }

    val gxxPersonalityFunction = externalNounwindFunction(
            personalityFunctionName,
            functionType(int32Type, true),
            origin = context.standardLlvmSymbolsOrigin
    )
    val cxaBeginCatchFunction = externalNounwindFunction(
            "__cxa_begin_catch",
            functionType(int8TypePtr, false, int8TypePtr),
            origin = context.standardLlvmSymbolsOrigin
    )
    val cxaEndCatchFunction = externalNounwindFunction(
            "__cxa_end_catch",
            functionType(voidType, false),
            origin = context.standardLlvmSymbolsOrigin
    )

    val memsetFunction = importMemset()

    val usedFunctions = mutableListOf<LLVMValueRef>()
    val usedGlobals = mutableListOf<LLVMValueRef>()
    val compilerUsedGlobals = mutableListOf<LLVMValueRef>()
    val staticInitializers = mutableListOf<LLVMValueRef>()
    val fileInitializers = mutableListOf<IrField>()
    val objects = mutableSetOf<LLVMValueRef>()
    val sharedObjects = mutableSetOf<LLVMValueRef>()

    private object lazyRtFunction {
        operator fun provideDelegate(
                thisRef: Llvm, property: KProperty<*>
        ) = object : ReadOnlyProperty<Llvm, LLVMValueRef> {

            val value by lazy { thisRef.importRtFunction(property.name) }

            override fun getValue(thisRef: Llvm, property: KProperty<*>): LLVMValueRef = value
        }
    }
}
