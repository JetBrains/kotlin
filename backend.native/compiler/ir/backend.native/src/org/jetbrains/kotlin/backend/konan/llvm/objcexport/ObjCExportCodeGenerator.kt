/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.*
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.ir.allParameters
import org.jetbrains.kotlin.backend.konan.ir.isOverridable
import org.jetbrains.kotlin.backend.konan.ir.isUnit
import org.jetbrains.kotlin.backend.konan.ir.llvmSymbolOrigin
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.ObjCCodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.objc.ObjCDataGenerator
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.backend.konan.serialization.resolveFakeOverrideMaybeAbstract
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.Name

internal fun TypeBridge.makeNothing() = when (this) {
    is ReferenceBridge, is BlockPointerBridge -> kNullInt8Ptr
    is ValueTypeBridge -> LLVMConstNull(this.objCValueType.llvmType)!!
}

internal class ObjCExportCodeGenerator(
        codegen: CodeGenerator,
        val namer: ObjCExportNamer,
        val mapper: ObjCExportMapper
) : ObjCCodeGenerator(codegen) {

    val symbols get() = context.ir.symbols

    val runtime get() = codegen.runtime
    val staticData get() = codegen.staticData

    val rttiGenerator = RTTIGenerator(context)

    private val objcTerminate: LLVMValueRef by lazy {
        context.llvm.externalFunction(
                "objc_terminate",
                functionType(voidType, false),
                CurrentKlibModuleOrigin
        ).also {
            setFunctionNoUnwind(it)
        }
    }

    val referencedSelectors = mutableMapOf<String, MethodBridge>()

    val externalGlobalInitializers = mutableMapOf<LLVMValueRef, ConstValue>()

    // TODO: currently bridges don't have any custom `landingpad`s,
    // so it is correct to use [callAtFunctionScope] here.
    // However, exception handling probably should be refactored
    // (e.g. moved from `IrToBitcode.kt` to [FunctionGenerationContext]).
    fun FunctionGenerationContext.callFromBridge(
            function: LLVMValueRef,
            args: List<LLVMValueRef>,
            resultLifetime: Lifetime = Lifetime.IRRELEVANT
    ): LLVMValueRef {

        // TODO: it is required only for Kotlin-to-Objective-C bridges.
        this.forwardingForeignExceptionsTerminatedWith = objcTerminate

        return call(function, args, resultLifetime, ExceptionHandler.Caller)
    }

    fun FunctionGenerationContext.genSendMessage(
            returnType: LLVMTypeRef,
            receiver: LLVMValueRef,
            selector: String,
            vararg args: LLVMValueRef
    ): LLVMValueRef {

        val objcMsgSendType = functionType(
                returnType,
                false,
                listOf(int8TypePtr, int8TypePtr) + args.map { it.type }
        )

        return callFromBridge(msgSender(objcMsgSendType), listOf(receiver, genSelector(selector)) + args)
    }

    fun FunctionGenerationContext.kotlinToObjC(
            value: LLVMValueRef,
            valueType: ObjCValueType
    ): LLVMValueRef = when (valueType) {
        ObjCValueType.BOOL -> zext(value, int8Type) // TODO: zext behaviour may be strange on bit types.

        ObjCValueType.UNICHAR,
        ObjCValueType.CHAR, ObjCValueType.SHORT, ObjCValueType.INT, ObjCValueType.LONG_LONG,
        ObjCValueType.UNSIGNED_CHAR, ObjCValueType.UNSIGNED_SHORT, ObjCValueType.UNSIGNED_INT,
        ObjCValueType.UNSIGNED_LONG_LONG,
        ObjCValueType.FLOAT, ObjCValueType.DOUBLE, ObjCValueType.POINTER -> value
    }

    private fun FunctionGenerationContext.objCToKotlin(
            value: LLVMValueRef,
            valueType: ObjCValueType
    ): LLVMValueRef = when (valueType) {
        ObjCValueType.BOOL -> icmpNe(value, Int8(0).llvm)

        ObjCValueType.UNICHAR,
        ObjCValueType.CHAR, ObjCValueType.SHORT, ObjCValueType.INT, ObjCValueType.LONG_LONG,
        ObjCValueType.UNSIGNED_CHAR, ObjCValueType.UNSIGNED_SHORT, ObjCValueType.UNSIGNED_INT,
        ObjCValueType.UNSIGNED_LONG_LONG,
        ObjCValueType.FLOAT, ObjCValueType.DOUBLE, ObjCValueType.POINTER -> value
    }

    fun FunctionGenerationContext.kotlinReferenceToObjC(value: LLVMValueRef) =
            callFromBridge(context.llvm.Kotlin_ObjCExport_refToObjC, listOf(value))

    fun FunctionGenerationContext.objCReferenceToKotlin(value: LLVMValueRef, resultLifetime: Lifetime) =
            callFromBridge(context.llvm.Kotlin_ObjCExport_refFromObjC, listOf(value), resultLifetime)

    private fun FunctionGenerationContext.objCBlockPointerToKotlin(
            value: LLVMValueRef,
            typeBridge: BlockPointerBridge,
            resultLifetime: Lifetime
    ) = callFromBridge(
            blockToKotlinFunctionConverter(typeBridge),
            listOf(value),
            resultLifetime
    )

    private val blockToKotlinFunctionConverterCache = mutableMapOf<BlockPointerBridge, LLVMValueRef>()

    internal fun blockToKotlinFunctionConverter(bridge: BlockPointerBridge): LLVMValueRef =
            blockToKotlinFunctionConverterCache.getOrPut(bridge) {
                generateBlockToKotlinFunctionConverter(bridge)
            }

    private fun FunctionGenerationContext.kotlinFunctionToObjCBlockPointer(
            typeBridge: BlockPointerBridge,
            value: LLVMValueRef
    ) = callFromBridge(kotlinFunctionToBlockConverter(typeBridge), listOf(value))

    private val blockAdapterToFunctionGenerator = BlockAdapterToFunctionGenerator(this)

    private val functionToBlockConverterCache = mutableMapOf<BlockPointerBridge, LLVMValueRef>()

    internal fun kotlinFunctionToBlockConverter(bridge: BlockPointerBridge): LLVMValueRef =
            functionToBlockConverterCache.getOrPut(bridge) {
                blockAdapterToFunctionGenerator.run {
                    generateConvertFunctionToBlock(bridge)
                }
            }

    fun FunctionGenerationContext.kotlinToObjC(
            value: LLVMValueRef,
            typeBridge: TypeBridge
    ): LLVMValueRef = if (LLVMTypeOf(value) == voidType) {
        typeBridge.makeNothing()
    } else {
        when (typeBridge) {
            is ReferenceBridge -> kotlinReferenceToObjC(value)
            is BlockPointerBridge -> kotlinFunctionToObjCBlockPointer(typeBridge, value)
            is ValueTypeBridge -> kotlinToObjC(value, typeBridge.objCValueType)
        }
    }

    fun FunctionGenerationContext.objCToKotlin(
            value: LLVMValueRef,
            typeBridge: TypeBridge,
            resultLifetime: Lifetime
    ): LLVMValueRef = when (typeBridge) {
        is ReferenceBridge -> objCReferenceToKotlin(value, resultLifetime)
        is BlockPointerBridge -> objCBlockPointerToKotlin(value, typeBridge, resultLifetime)
        is ValueTypeBridge -> objCToKotlin(value, typeBridge.objCValueType)
    }

    fun FunctionGenerationContext.initRuntimeIfNeeded() {
        callFromBridge(context.llvm.initRuntimeIfNeeded, emptyList())
    }

    inline fun FunctionGenerationContext.convertKotlin(
            genValue: (Lifetime) -> LLVMValueRef,
            actualType: IrType,
            expectedType: IrType,
            resultLifetime: Lifetime
    ): LLVMValueRef {

        val conversion = symbols.getTypeConversion(actualType, expectedType)
                ?: return genValue(resultLifetime)

        val value = genValue(Lifetime.ARGUMENT)

        return callFromBridge(conversion.owner.llvmFunction, listOf(value), resultLifetime)
    }

    private val objCTypeAdapters = mutableListOf<ObjCTypeAdapter>()

    internal fun generate(spec: ObjCExportCodeSpec) {
        spec.types.forEach {
            objCTypeAdapters += when (it) {
                is ObjCClassForKotlinClass -> {
                    val superClass = it.superClassNotAny ?: objCClassForAny

                    dataGenerator.emitEmptyClass(it.binaryName, superClass.binaryName)
                    // Note: it is generated only to be visible for linker.
                    // Methods will be added at runtime.

                    createTypeAdapter(it, superClass)
                }

                is ObjCProtocolForKotlinInterface -> createTypeAdapter(it, superClass = null)
            }
        }

        spec.files.forEach {
            objCTypeAdapters += createTypeAdapterForFileClass(it)
            dataGenerator.emitEmptyClass(it.binaryName, namer.kotlinAnyName.binaryName)
        }
    }

    internal fun emitRtti() {
        NSNumberKind.values().mapNotNull { it.mappedKotlinClassId }.forEach {
            dataGenerator.exportClass("Kotlin${it.shortClassName}")
        }
        dataGenerator.exportClass("KotlinMutableSet")
        dataGenerator.exportClass("KotlinMutableDictionary")

        emitSpecialClassesConvertions()

        objCTypeAdapters += createTypeAdapter(objCClassForAny, superClass = null)

        emitTypeAdapters()

        emitSelectorsHolder()

        emitStaticInitializers()
    }

    private fun emitTypeAdapters() {
        val placedClassAdapters = mutableMapOf<String, ConstPointer>()
        val placedInterfaceAdapters = mutableMapOf<String, ConstPointer>()

        objCTypeAdapters.forEach { adapter ->
            val typeAdapter = staticData.placeGlobal("", adapter).pointer
            val irClass = adapter.irClass

            val descriptorToAdapter = if (irClass?.isInterface == true) {
                placedInterfaceAdapters
            } else {
                // Objective-C class for Kotlin class or top-level declarations.
                placedClassAdapters
            }
            descriptorToAdapter[adapter.objCName] = typeAdapter

            if (irClass != null) {
                if (!context.llvmModuleSpecification.importsKotlinDeclarationsFromOtherSharedLibraries()) {
                    setObjCExportTypeInfo(irClass, typeAdapter = typeAdapter)
                } else {
                    // Optimization: avoid generating huge initializers;
                    // handled with "Kotlin_ObjCExport_initTypeAdapters" below.
                }
            }
        }

        fun emitSortedAdapters(nameToAdapter: Map<String, ConstPointer>, prefix: String) {
            val sortedAdapters = nameToAdapter.toList().sortedBy { it.first }.map {
                it.second
            }

            if (sortedAdapters.isNotEmpty()) {
                val type = sortedAdapters.first().llvmType
                val sortedAdaptersPointer = staticData.placeGlobalConstArray("", type, sortedAdapters)

                // Note: this globals replace runtime globals with weak linkage:
                val origin = context.standardLlvmSymbolsOrigin
                replaceExternalWeakOrCommonGlobal(prefix, sortedAdaptersPointer, origin)
                replaceExternalWeakOrCommonGlobal("${prefix}Num", Int32(sortedAdapters.size), origin)
            }
        }

        emitSortedAdapters(placedClassAdapters, "Kotlin_ObjCExport_sortedClassAdapters")
        emitSortedAdapters(placedInterfaceAdapters, "Kotlin_ObjCExport_sortedProtocolAdapters")

        if (context.llvmModuleSpecification.importsKotlinDeclarationsFromOtherSharedLibraries()) {
            replaceExternalWeakOrCommonGlobal(
                    "Kotlin_ObjCExport_initTypeAdapters",
                    Int1(1),
                    context.standardLlvmSymbolsOrigin
            )
        }
    }

    private fun emitStaticInitializers() {
        if (externalGlobalInitializers.isEmpty()) return

        val initializer = generateFunction(codegen, functionType(voidType, false), "initObjCExportGlobals") {
            externalGlobalInitializers.forEach { (global, value) ->
                store(value.llvm, global)
            }
            ret(null)
        }

        LLVMSetLinkage(initializer, LLVMLinkage.LLVMInternalLinkage)

        context.llvm.otherStaticInitializers += initializer
    }

    // TODO: consider including this into ObjCExportCodeSpec.
    private val objCClassForAny = ObjCClassForKotlinClass(
            namer.kotlinAnyName.binaryName,
            symbols.any,
            methods = listOf("equals", "hashCode", "toString").map { name ->
                symbols.any.owner.simpleFunctions().single { it.name == Name.identifier(name) }
            }.map {
                require(mapper.shouldBeExposed(it.descriptor))
                ObjCMethodForKotlinMethod(it.symbol)
            },
            categoryMethods = emptyList(),
            superClassNotAny = null
    )

    private fun emitSelectorsHolder() {
        val impType = functionType(voidType, false, int8TypePtr, int8TypePtr)
        val imp = generateFunction(codegen, impType, "") {
            unreachable()
        }

        val methods = referencedSelectors.map { (selector, bridge) ->
            ObjCDataGenerator.Method(selector, getEncoding(bridge), constPointer(imp))
        }

        dataGenerator.emitClass("KotlinSelectorsHolder", "NSObject", instanceMethods = methods)
    }

    private val impType = pointerType(functionType(int8TypePtr, true, int8TypePtr, int8TypePtr))

    internal val directMethodAdapters = mutableMapOf<DirectAdapterRequest, ObjCToKotlinMethodAdapter>()

    inner class ObjCToKotlinMethodAdapter(
            selector: String,
            encoding: String,
            imp: ConstPointer
    ) : Struct(
            runtime.objCToKotlinMethodAdapter,
            staticData.cStringLiteral(selector),
            staticData.cStringLiteral(encoding),
            imp.bitcast(impType)
    )

    inner class KotlinToObjCMethodAdapter(
            selector: String,
            nameSignature: Long,
            itablePlace: ClassLayoutBuilder.InterfaceTablePlace,
            vtableIndex: Int,
            kotlinImpl: ConstPointer
    ) : Struct(
            runtime.kotlinToObjCMethodAdapter,
            staticData.cStringLiteral(selector),
            Int64(nameSignature),
            Int32(itablePlace.interfaceId),
            Int32(itablePlace.methodIndex),
            Int32(vtableIndex),
            kotlinImpl
    )

    inner class ObjCTypeAdapter(
            val irClass: IrClass?,
            typeInfo: ConstPointer?,
            vtable: ConstPointer?,
            vtableSize: Int,
            methodTable: List<RTTIGenerator.MethodTableRecord>,
            itableSize: Int,
            val objCName: String,
            directAdapters: List<ObjCToKotlinMethodAdapter>,
            classAdapters: List<ObjCToKotlinMethodAdapter>,
            virtualAdapters: List<ObjCToKotlinMethodAdapter>,
            reverseAdapters: List<KotlinToObjCMethodAdapter>
    ) : Struct(
            runtime.objCTypeAdapter,
            typeInfo,

            vtable,
            Int32(vtableSize),

            staticData.placeGlobalConstArray("", runtime.methodTableRecordType, methodTable),
            Int32(methodTable.size),

            Int32(itableSize),

            staticData.cStringLiteral(objCName),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.objCToKotlinMethodAdapter,
                    directAdapters
            ),
            Int32(directAdapters.size),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.objCToKotlinMethodAdapter,
                    classAdapters
            ),
            Int32(classAdapters.size),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.objCToKotlinMethodAdapter,
                    virtualAdapters
            ),
            Int32(virtualAdapters.size),

            staticData.placeGlobalConstArray(
                    "",
                    runtime.kotlinToObjCMethodAdapter,
                    reverseAdapters
            ),
            Int32(reverseAdapters.size)
    )

}

private fun ObjCExportCodeGenerator.replaceExternalWeakOrCommonGlobal(
        name: String,
        value: ConstValue,
        origin: CompiledKlibModuleOrigin
) {
    if (context.llvmModuleSpecification.importsKotlinDeclarationsFromOtherSharedLibraries()) {
        val global = codegen.importGlobal(name, value.llvmType, origin)
        externalGlobalInitializers[global] = value
    } else {
        context.llvmImports.add(origin)
        val global = staticData.placeGlobal(name, value, isExported = true)

        if (context.llvmModuleSpecification.importsKotlinDeclarationsFromOtherObjectFiles()) {
            // Note: actually this is required only if global's weak/common definition is in another object file,
            // but it is simpler to do this for all globals, considering that all usages can't be removed by DCE anyway.
            context.llvm.usedGlobals += global.llvmGlobal
        }
    }
}

private fun ObjCExportCodeGenerator.setObjCExportTypeInfo(
        irClass: IrClass,
        converter: ConstPointer? = null,
        objCClass: ConstPointer? = null,
        typeAdapter: ConstPointer? = null
) {
    if (converter != null) {
        assert(converter.llvmType == pointerType(functionType(int8TypePtr, false, codegen.kObjHeaderPtr)))
    }

    val objCExportAddition = Struct(runtime.typeInfoObjCExportAddition,
            converter?.bitcast(int8TypePtr),
            objCClass,
            typeAdapter
    )

    val writableTypeInfoType = runtime.writableTypeInfoType!!
    val writableTypeInfoValue = Struct(writableTypeInfoType, objCExportAddition)

    if (codegen.isExternal(irClass)) {
        // Note: this global replaces the external one with common linkage.
        replaceExternalWeakOrCommonGlobal(
                irClass.writableTypeInfoSymbolName,
                writableTypeInfoValue,
                irClass.llvmSymbolOrigin
        )
    } else {
        context.llvmDeclarations.forClass(irClass).writableTypeInfoGlobal!!.also {
            it.setLinkage(LLVMLinkage.LLVMExternalLinkage)
        }.setInitializer(writableTypeInfoValue)
    }
}

private val ObjCExportCodeGenerator.kotlinToObjCFunctionType: LLVMTypeRef
    get() = functionType(int8TypePtr, false, codegen.kObjHeaderPtr)

private fun ObjCExportCodeGenerator.emitBoxConverters() {
    val irBuiltIns = context.irBuiltIns

    emitBoxConverter(irBuiltIns.booleanClass, ObjCValueType.BOOL, "numberWithBool:")
    emitBoxConverter(irBuiltIns.byteClass, ObjCValueType.CHAR, "numberWithChar:")
    emitBoxConverter(irBuiltIns.shortClass, ObjCValueType.SHORT, "numberWithShort:")
    emitBoxConverter(irBuiltIns.intClass, ObjCValueType.INT, "numberWithInt:")
    emitBoxConverter(irBuiltIns.longClass, ObjCValueType.LONG_LONG, "numberWithLongLong:")
    emitBoxConverter(symbols.uByte, ObjCValueType.UNSIGNED_CHAR, "numberWithUnsignedChar:")
    emitBoxConverter(symbols.uShort, ObjCValueType.UNSIGNED_SHORT, "numberWithUnsignedShort:")
    emitBoxConverter(symbols.uInt, ObjCValueType.UNSIGNED_INT, "numberWithUnsignedInt:")
    emitBoxConverter(symbols.uLong, ObjCValueType.UNSIGNED_LONG_LONG, "numberWithUnsignedLongLong:")
    emitBoxConverter(irBuiltIns.floatClass, ObjCValueType.FLOAT, "numberWithFloat:")
    emitBoxConverter(irBuiltIns.doubleClass, ObjCValueType.DOUBLE, "numberWithDouble:")
}

private fun ObjCExportCodeGenerator.emitBoxConverter(
        boxClassSymbol: IrClassSymbol,
        objCValueType: ObjCValueType,
        nsNumberFactorySelector: String
) {
    val boxClass = boxClassSymbol.owner
    val name = "${boxClass.name}ToNSNumber"

    val converter = generateFunction(codegen, kotlinToObjCFunctionType, name) {
        val unboxFunction = context.getUnboxFunction(boxClass).llvmFunction
        val kotlinValue = callFromBridge(
                unboxFunction,
                listOf(param(0)),
                Lifetime.IRRELEVANT
        )

        val value = kotlinToObjC(kotlinValue, objCValueType)

        val nsNumberSubclass = genGetLinkedClass("Kotlin${boxClass.name}")
        ret(genSendMessage(int8TypePtr, nsNumberSubclass, nsNumberFactorySelector, value))
    }

    LLVMSetLinkage(converter, LLVMLinkage.LLVMPrivateLinkage)
    setObjCExportTypeInfo(boxClass, constPointer(converter))
}

private fun ObjCExportCodeGenerator.emitFunctionConverters() {
    context.ir.symbols.functionIrClassFactory.builtFunctionNClasses.forEach { functionClass ->
        val converter = kotlinFunctionToBlockConverter(BlockPointerBridge(functionClass.arity, returnsVoid = false))
        setObjCExportTypeInfo(functionClass.irClass, constPointer(converter))
    }
}

private fun ObjCExportCodeGenerator.emitBlockToKotlinFunctionConverters() {
    val converters = context.ir.symbols.functionIrClassFactory.builtFunctionNClasses.map {
        val bridge = BlockPointerBridge(numberOfParameters = it.arity, returnsVoid = false)
        constPointer(blockToKotlinFunctionConverter(bridge))
    }
    val ptr = staticData.placeGlobalArray(
            "",
            converters.first().llvmType,
            converters
    ).pointer.getElementPtr(0)

    // Note: this global replaces the weak global defined in runtime.
    replaceExternalWeakOrCommonGlobal(
            "Kotlin_ObjCExport_blockToFunctionConverters",
            ptr,
            context.standardLlvmSymbolsOrigin
    )
}

private fun ObjCExportCodeGenerator.emitSpecialClassesConvertions() {
    setObjCExportTypeInfo(
            symbols.string.owner,
            constPointer(context.llvm.Kotlin_ObjCExport_CreateNSStringFromKString)
    )

    emitCollectionConverters()

    emitBoxConverters()

    emitFunctionConverters()

    emitBlockToKotlinFunctionConverters()
}

private fun ObjCExportCodeGenerator.emitCollectionConverters() {

    fun importConverter(name: String): ConstPointer = constPointer(context.llvm.externalFunction(
            name,
            kotlinToObjCFunctionType,
            CurrentKlibModuleOrigin
    ))

    setObjCExportTypeInfo(
            symbols.list.owner,
            importConverter("Kotlin_Interop_CreateNSArrayFromKList")
    )

    setObjCExportTypeInfo(
            symbols.mutableList.owner,
            importConverter("Kotlin_Interop_CreateNSMutableArrayFromKList")
    )

    setObjCExportTypeInfo(
            symbols.set.owner,
            importConverter("Kotlin_Interop_CreateNSSetFromKSet")
    )

    setObjCExportTypeInfo(
            symbols.mutableSet.owner,
            importConverter("Kotlin_Interop_CreateKotlinMutableSetFromKSet")
    )

    setObjCExportTypeInfo(
            symbols.map.owner,
            importConverter("Kotlin_Interop_CreateNSDictionaryFromKMap")
    )

    setObjCExportTypeInfo(
            symbols.mutableMap.owner,
            importConverter("Kotlin_Interop_CreateKotlinMutableDictonaryFromKMap")
    )
}

private inline fun ObjCExportCodeGenerator.generateObjCImpBy(
        methodBridge: MethodBridge,
        genBody: FunctionGenerationContext.() -> Unit
): LLVMValueRef {
    val result = LLVMAddFunction(context.llvmModule, "objc2kotlin", objCFunctionType(context, methodBridge))!!

    generateFunction(codegen, result) {
        genBody()
    }

    LLVMSetLinkage(result, LLVMLinkage.LLVMInternalLinkage)
    return result
}

private fun ObjCExportCodeGenerator.generateAbstractObjCImp(methodBridge: MethodBridge): LLVMValueRef =
        generateObjCImpBy(methodBridge) {
            callFromBridge(
                    context.llvm.Kotlin_ObjCExport_AbstractMethodCalled,
                    listOf(param(0), param(1))
            )
            unreachable()
        }

private fun ObjCExportCodeGenerator.generateObjCImp(
        target: IrFunction?,
        methodBridge: MethodBridge,
        isVirtual: Boolean = false
) = if (target == null) {
    generateAbstractObjCImp(methodBridge)
} else {
    generateObjCImp(methodBridge, isDirect = !isVirtual) { args, resultLifetime, exceptionHandler ->
        val llvmTarget = if (!isVirtual) {
            codegen.llvmFunction(target)
        } else {
            lookupVirtualImpl(args.first(), target)
        }

        call(llvmTarget, args, resultLifetime, exceptionHandler)
    }
}

private fun ObjCExportCodeGenerator.generateObjCImp(
        methodBridge: MethodBridge,
        isDirect: Boolean,
        callKotlin: FunctionGenerationContext.(
                args: List<LLVMValueRef>,
                resultLifetime: Lifetime,
                exceptionHandler: ExceptionHandler
        ) -> LLVMValueRef?
): LLVMValueRef = generateObjCImpBy(methodBridge) {
    if (isDirect) {
        // Consider this call inlinable. If it is inlined into a bridge with no debug information,
        // lldb will not decode the inlined frame even if the callee has debug information.
        initBridgeDebugInfo()
        // TODO: consider adding debug info to other bridges.
    }

    val returnType = methodBridge.returnBridge

    // TODO: call [NSObject init] if it is a constructor?
    // TODO: check for abstract class if it is a constructor.

    if (!methodBridge.isInstance) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.
    }

    var errorOutPtr: LLVMValueRef? = null
    var kotlinResultOutPtr: LLVMValueRef? = null
    lateinit var kotlinResultOutBridge: TypeBridge

    val kotlinArgs = methodBridge.paramBridges.mapIndexedNotNull { index, paramBridge ->
        val parameter = param(index)
        when (paramBridge) {
            is MethodBridgeValueParameter.Mapped ->
                objCToKotlin(parameter, paramBridge.bridge, Lifetime.ARGUMENT)

            MethodBridgeReceiver.Static, MethodBridgeSelector -> null
            MethodBridgeReceiver.Instance -> objCReferenceToKotlin(parameter, Lifetime.ARGUMENT)

            MethodBridgeReceiver.Factory -> null // actual value added by [callKotlin].

            MethodBridgeValueParameter.ErrorOutParameter -> {
                assert(errorOutPtr == null)
                errorOutPtr = parameter
                null
            }

            is MethodBridgeValueParameter.KotlinResultOutParameter -> {
                assert(kotlinResultOutPtr == null)
                kotlinResultOutPtr = parameter
                kotlinResultOutBridge = paramBridge.bridge
                null
            }
        }
    }

    // TODO: consider merging this handler with function cleanup.
    val exceptionHandler = if (errorOutPtr == null) {
        kotlinExceptionHandler { exception ->
            callFromBridge(symbols.objCExportTrapOnUndeclaredException.owner.llvmFunction, listOf(exception))
            unreachable()
        }
    } else {
        kotlinExceptionHandler { exception ->
            callFromBridge(
                    context.llvm.Kotlin_ObjCExport_RethrowExceptionAsNSError,
                    listOf(exception, errorOutPtr!!)
            )

            val returnValue = when (returnType) {
                !is MethodBridge.ReturnValue.WithError ->
                    error("bridge with error parameter has unexpected return type: $returnType")

                MethodBridge.ReturnValue.WithError.Success -> Int8(0).llvm // false

                is MethodBridge.ReturnValue.WithError.RefOrNull -> {
                    if (returnType.successBridge == MethodBridge.ReturnValue.Instance.InitResult) {
                        // Release init receiver, as required by convention.
                        callFromBridge(objcRelease, listOf(param(0)))
                    }
                    kNullInt8Ptr
                }
            }

            ret(returnValue)
        }
    }

    val targetResult = callKotlin(kotlinArgs, Lifetime.ARGUMENT, exceptionHandler)

    kotlinResultOutPtr?.let {
        ifThen(icmpNe(it, LLVMConstNull(it.type)!!)) {
            val objCResult = kotlinToObjC(targetResult!!, kotlinResultOutBridge)
            store(objCResult, it)
        }
    }

    tailrec fun genReturnValueOnSuccess(returnBridge: MethodBridge.ReturnValue): LLVMValueRef? = when (returnBridge) {
        MethodBridge.ReturnValue.Void -> null
        MethodBridge.ReturnValue.HashCode -> {
            val kotlinHashCode = targetResult!!
            if (codegen.context.is64BitNSInteger()) zext(kotlinHashCode, int64Type) else kotlinHashCode
        }
        is MethodBridge.ReturnValue.Mapped -> kotlinToObjC(targetResult!!, returnBridge.bridge)
        MethodBridge.ReturnValue.WithError.Success -> Int8(1).llvm // true
        is MethodBridge.ReturnValue.WithError.RefOrNull -> genReturnValueOnSuccess(returnBridge.successBridge)
        MethodBridge.ReturnValue.Instance.InitResult -> param(0)
        MethodBridge.ReturnValue.Instance.FactoryResult -> kotlinReferenceToObjC(targetResult!!) // provided by [callKotlin]
    }

    ret(genReturnValueOnSuccess(returnType))
}

private fun ObjCExportCodeGenerator.generateObjCImpForArrayConstructor(
        target: IrConstructor,
        methodBridge: MethodBridge
): LLVMValueRef = generateObjCImp(methodBridge, isDirect = true) { args, resultLifetime, exceptionHandler ->
    val arrayInstance = callFromBridge(
            context.llvm.allocArrayFunction,
            listOf(target.constructedClass.llvmTypeInfoPtr, args.first()),
            resultLifetime = Lifetime.ARGUMENT
    )

    call(target.llvmFunction, listOf(arrayInstance) + args, resultLifetime, exceptionHandler)
    arrayInstance
}

// TODO: cache bridges.
private fun ObjCExportCodeGenerator.generateKotlinToObjCBridge(
        irFunction: IrFunction,
        baseIrFunction: IrFunction
): ConstPointer {
    val baseMethod = baseIrFunction.descriptor

    val methodBridge = mapper.bridgeMethod(baseMethod)

    val parameterToBase = irFunction.allParameters.zip(baseIrFunction.allParameters).toMap()

    val objcMsgSend = msgSender(objCFunctionType(context, methodBridge))

    val functionType = codegen.getLlvmFunctionType(irFunction)

    val result = generateFunction(codegen, functionType, "kotlin2objc") {
        var errorOutPtr: LLVMValueRef? = null
        var kotlinResultOutPtr: LLVMValueRef? = null
        lateinit var kotlinResultOutBridge: TypeBridge

        val parameters = irFunction.allParameters.mapIndexed { index, parameterDescriptor ->
            parameterDescriptor to param(index)
        }.toMap()

        val objCArgs = methodBridge.parametersAssociated(irFunction).map { (bridge, parameter) ->
            when (bridge) {
                is MethodBridgeValueParameter.Mapped -> {
                    parameter!!
                    val kotlinValue = convertKotlin(
                            { parameters[parameter]!! },
                            actualType = parameter.type,
                            expectedType = parameterToBase[parameter]!!.type,
                            resultLifetime = Lifetime.ARGUMENT
                    )
                    kotlinToObjC(kotlinValue, bridge.bridge)
                }

                MethodBridgeReceiver.Instance -> kotlinReferenceToObjC(parameters[parameter]!!)
                MethodBridgeSelector -> {
                    val selector = namer.getSelector(baseMethod)
                    referencedSelectors.getOrPut(selector) { methodBridge }
                    genSelector(selector)
                }

                MethodBridgeReceiver.Static,
                MethodBridgeReceiver.Factory ->
                    error("Method is not instance and thus can't have bridge for overriding: $baseMethod")

                MethodBridgeValueParameter.ErrorOutParameter ->
                    alloca(int8TypePtr).also { errorOutPtr = it }

                is MethodBridgeValueParameter.KotlinResultOutParameter ->
                    alloca(bridge.bridge.objCType).also {
                        kotlinResultOutPtr = it
                        kotlinResultOutBridge = bridge.bridge
                    }
            }
        }

        val targetResult = callFromBridge(objcMsgSend, objCArgs)

        assert(baseMethod !is ConstructorDescriptor)

        fun rethrow() {
            val error = load(errorOutPtr!!)
            callFromBridge(context.llvm.Kotlin_ObjCExport_RethrowNSErrorAsException, listOf(error))
            unreachable()
        }

        fun genKotlinBaseMethodResult(
                lifetime: Lifetime,
                returnBridge: MethodBridge.ReturnValue
        ): LLVMValueRef? = when (returnBridge) {
            MethodBridge.ReturnValue.Void -> null

            MethodBridge.ReturnValue.HashCode -> {
                if (codegen.context.is64BitNSInteger()) {
                    val low = trunc(targetResult, int32Type)
                    val high = trunc(shr(targetResult, 32, signed = false), int32Type)
                    xor(low, high)
                } else {
                    targetResult
                }
            }

            is MethodBridge.ReturnValue.Mapped -> {
                objCToKotlin(targetResult, returnBridge.bridge, lifetime)
            }

            MethodBridge.ReturnValue.WithError.Success -> {
                ifThen(icmpEq(targetResult, Int8(0).llvm)) {
                    rethrow()
                }

                kotlinResultOutPtr?.let {
                    objCToKotlin(load(it), kotlinResultOutBridge, lifetime)
                }
            }

            is MethodBridge.ReturnValue.WithError.RefOrNull -> {
                ifThen(icmpEq(targetResult, kNullInt8Ptr)) {
                    rethrow()
                }
                assert(kotlinResultOutPtr == null)
                genKotlinBaseMethodResult(lifetime, returnBridge.successBridge)
            }

            MethodBridge.ReturnValue.Instance.InitResult,
            MethodBridge.ReturnValue.Instance.FactoryResult ->
                error("init or factory method can't have bridge for overriding: $baseMethod")
        }

        val baseReturnType = baseIrFunction.returnType
        val actualReturnType = irFunction.returnType

        val retVal = when {
            actualReturnType.isUnit() || actualReturnType.isNothing() -> {
                genKotlinBaseMethodResult(Lifetime.ARGUMENT, methodBridge.returnBridge)
                null
            }
            baseReturnType.isUnit() || baseReturnType.isNothing() -> {
                genKotlinBaseMethodResult(Lifetime.ARGUMENT, methodBridge.returnBridge)
                codegen.theUnitInstanceRef.llvm
            }
            else ->
                convertKotlin(
                        { lifetime -> genKotlinBaseMethodResult(lifetime, methodBridge.returnBridge)!! },
                        actualType = baseReturnType,
                        expectedType = actualReturnType,
                        resultLifetime = Lifetime.RETURN_VALUE
                )
        }

        ret(retVal)
    }

    LLVMSetLinkage(result, LLVMLinkage.LLVMPrivateLinkage)

    return constPointer(result)
}

private fun ObjCExportCodeGenerator.createReverseAdapter(
        irFunction: IrFunction,
        baseMethod: IrFunction,
        functionName: String,
        vtableIndex: Int?,
        itablePlace: ClassLayoutBuilder.InterfaceTablePlace?
): ObjCExportCodeGenerator.KotlinToObjCMethodAdapter {

    val nameSignature = functionName.localHash.value
    val selector = namer.getSelector(baseMethod.descriptor)

    val kotlinToObjC = generateKotlinToObjCBridge(
            irFunction,
            baseMethod
    ).bitcast(int8TypePtr)

    return KotlinToObjCMethodAdapter(selector, nameSignature,
            itablePlace ?: ClassLayoutBuilder.InterfaceTablePlace.INVALID,
            vtableIndex ?: -1,
            kotlinToObjC)
}

private fun ObjCExportCodeGenerator.createMethodVirtualAdapter(
        baseMethod: IrFunction
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(mapper.isBaseMethod(baseMethod.descriptor))

    val selector = namer.getSelector(baseMethod.descriptor)

    val methodBridge = mapper.bridgeMethod(baseMethod.descriptor)
    val objCToKotlin = constPointer(generateObjCImp(baseMethod, methodBridge, isVirtual = true))
    return ObjCToKotlinMethodAdapter(selector, getEncoding(methodBridge), objCToKotlin)
}

private fun ObjCExportCodeGenerator.createMethodAdapter(
        implementation: IrFunction?,
        baseMethod: IrFunction
) = createMethodAdapter(DirectAdapterRequest(implementation, baseMethod))

private fun ObjCExportCodeGenerator.createFinalMethodAdapter(
        irFunction: IrSimpleFunction
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    require(irFunction.modality == Modality.FINAL)
    return createMethodAdapter(irFunction, irFunction)
}

private fun ObjCExportCodeGenerator.createMethodAdapter(
        request: DirectAdapterRequest
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter = this.directMethodAdapters.getOrPut(request) {

    val selectorName = namer.getSelector(request.base.descriptor)
    val methodBridge = mapper.bridgeMethod(request.base.descriptor)
    val objCEncoding = getEncoding(methodBridge)
    val objCToKotlin = constPointer(generateObjCImp(request.implementation, methodBridge))

    ObjCToKotlinMethodAdapter(selectorName, objCEncoding, objCToKotlin)
}

private fun ObjCExportCodeGenerator.createConstructorAdapter(
        irConstructor: IrConstructor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter = createMethodAdapter(irConstructor, irConstructor)

private fun ObjCExportCodeGenerator.createArrayConstructorAdapter(
        irConstructor: IrConstructor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    val selectorName = namer.getSelector(irConstructor.descriptor)
    val methodBridge = mapper.bridgeMethod(irConstructor.descriptor)
    val objCEncoding = getEncoding(methodBridge)
    val objCToKotlin = constPointer(generateObjCImpForArrayConstructor(irConstructor, methodBridge))

    return ObjCToKotlinMethodAdapter(selectorName, objCEncoding, objCToKotlin)
}

private fun ObjCExportCodeGenerator.vtableIndex(irFunction: IrSimpleFunction): Int? {
    assert(irFunction.isOverridable)
    val irClass = irFunction.parentAsClass
    return if (irClass.isInterface) {
        null
    } else {
        context.getLayoutBuilder(irClass).vtableIndex(irFunction)
    }
}

private fun ObjCExportCodeGenerator.itablePlace(irFunction: IrSimpleFunction): ClassLayoutBuilder.InterfaceTablePlace? {
    assert(irFunction.isOverridable)
    val irClass = irFunction.parentAsClass
    return if (irClass.isInterface && context.ghaEnabled()
            && (irFunction.isReal || irFunction.resolveFakeOverrideMaybeAbstract().parent != context.irBuiltIns.anyClass.owner)) {
        context.getLayoutBuilder(irClass).itablePlace(irFunction)
    } else {
        null
    }
}

private fun ObjCExportCodeGenerator.createTypeAdapterForFileClass(
        fileClass: ObjCClassForKotlinFile
): ObjCExportCodeGenerator.ObjCTypeAdapter {
    val name = fileClass.binaryName

    val adapters = fileClass.methods.map { createFinalMethodAdapter(it.baseMethod.owner) }

    return ObjCTypeAdapter(
            irClass = null,
            typeInfo = null,
            vtable = null,
            vtableSize = -1,
            methodTable = emptyList(),
            itableSize = -1,
            objCName = name,
            directAdapters = emptyList(),
            classAdapters = adapters,
            virtualAdapters = emptyList(),
            reverseAdapters = emptyList()
    )
}

private fun ObjCExportCodeGenerator.createTypeAdapter(
        type: ObjCTypeForKotlinType,
        superClass: ObjCClassForKotlinClass?
): ObjCExportCodeGenerator.ObjCTypeAdapter {
    val irClass = type.irClassSymbol.owner
    val adapters = mutableListOf<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter>()
    val classAdapters = mutableListOf<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter>()

    type.methods.forEach {
        when (it) {
            is ObjCInitMethodForKotlinConstructor -> {
                adapters += createConstructorAdapter(it.irConstructorSymbol.owner)
            }
            is ObjCFactoryMethodForKotlinArrayConstructor -> {
                classAdapters += createArrayConstructorAdapter(it.irConstructorSymbol.owner)
            }
            is ObjCGetterForKotlinEnumEntry -> {
                classAdapters += createEnumEntryAdapter(it.irEnumEntrySymbol.owner)
            }
            is ObjCMethodForKotlinMethod -> {} // Handled below.
        }.let {} // Force exhaustive.
    }

    val reverseAdapters = mutableListOf<ObjCExportCodeGenerator.KotlinToObjCMethodAdapter>()

    if (type is ObjCClassForKotlinClass) {

        type.categoryMethods.forEach {
            val irFunction = it.baseMethod.owner
            adapters += createFinalMethodAdapter(irFunction)
            reverseAdapters += nonOverridableAdapter(irFunction.descriptor, hasSelectorAmbiguity = false)
        }

        adapters += createDirectAdapters(type, superClass)
    }

    reverseAdapters += createReverseAdapters(type)

    val virtualAdapters = type.kotlinMethods.map { it.baseMethod.owner }
            .filter { it.parentAsClass == irClass && it.isOverridable }
            .map { createMethodVirtualAdapter(it) }

    val typeInfo = constPointer(codegen.typeInfoValue(irClass))
    val objCName = type.binaryName

    val vtableSize = if (irClass.kind == ClassKind.INTERFACE) {
        -1
    } else {
        context.getLayoutBuilder(irClass).vtableEntries.size
    }

    val vtable = if (!irClass.isInterface && !irClass.typeInfoHasVtableAttached) {
        staticData.placeGlobal("", rttiGenerator.vtable(irClass)).also {
            it.setConstant(true)
        }.pointer.getElementPtr(0)
    } else {
        null
    }

    val methodTable = if (!irClass.isInterface && irClass.isAbstract()) {
        rttiGenerator.methodTableRecords(irClass)
    } else {
        emptyList()
    }

    val itableSize = if (irClass.isInterface)
        context.getLayoutBuilder(irClass).interfaceTableEntries.size
    else -1

    when (irClass.kind) {
        ClassKind.OBJECT -> {
            classAdapters += if (irClass.isUnit()) {
                createUnitInstanceAdapter()
            } else {
                createObjectInstanceAdapter(irClass)
            }
        }
        else -> {
            // Nothing special.
        }
    }

    return ObjCTypeAdapter(
            irClass,
            typeInfo,
            vtable,
            vtableSize,
            methodTable,
            itableSize,
            objCName,
            adapters,
            classAdapters,
            virtualAdapters,
            reverseAdapters
    )
}

private fun ObjCExportCodeGenerator.createReverseAdapters(
        type: ObjCTypeForKotlinType
): List<ObjCExportCodeGenerator.KotlinToObjCMethodAdapter> {
    val result = mutableListOf<ObjCExportCodeGenerator.KotlinToObjCMethodAdapter>()
    val allBaseMethods = type.kotlinMethods.map { it.baseMethod.owner }.toSet()

    for (method in type.irClassSymbol.owner.simpleFunctions()) {
        val baseMethods = method.allOverriddenFunctions.filter { it in allBaseMethods }
        if (baseMethods.isEmpty()) continue

        val hasSelectorAmbiguity = baseMethods.map { namer.getSelector(it.descriptor) }.distinct().size > 1

        if (method.isOverridable && !hasSelectorAmbiguity) {
            val baseMethod = baseMethods.first()

            val presentVtableBridges = mutableSetOf<Int?>(null)
            val presentMethodTableBridges = mutableSetOf<String>()
            val presentItableBridges = mutableSetOf<ClassLayoutBuilder.InterfaceTablePlace?>(null)

            val allOverriddenMethods = method.allOverriddenFunctions

            val (inherited, uninherited) = allOverriddenMethods.partition {
                it != method && mapper.shouldBeExposed(it.descriptor)
            }

            inherited.forEach {
                presentVtableBridges += vtableIndex(it)
                presentMethodTableBridges += it.functionName
                presentItableBridges += itablePlace(it)
            }

            uninherited.forEach {
                val vtableIndex = vtableIndex(it)
                val functionName = it.functionName
                val itablePlace = itablePlace(it)

                if (vtableIndex !in presentVtableBridges || functionName !in presentMethodTableBridges
                        || itablePlace !in presentItableBridges) {
                    presentVtableBridges += vtableIndex
                    presentMethodTableBridges += functionName
                    presentItableBridges += itablePlace
                    result += createReverseAdapter(it, baseMethod, functionName, vtableIndex, itablePlace)
                }
            }

        } else {
            // Mark it as non-overridable:
            baseMethods.distinctBy { namer.getSelector(it.descriptor) }.forEach { baseMethod ->
                result += nonOverridableAdapter(baseMethod.descriptor, hasSelectorAmbiguity)
            }
        }
    }

    return result
}

private fun ObjCExportCodeGenerator.nonOverridableAdapter(
        baseMethod: FunctionDescriptor,
        hasSelectorAmbiguity: Boolean
): ObjCExportCodeGenerator.KotlinToObjCMethodAdapter = KotlinToObjCMethodAdapter(
    namer.getSelector(baseMethod),
    -1,
    vtableIndex = if (hasSelectorAmbiguity) -2 else -1, // Describes the reason.
    kotlinImpl = NullPointer(int8Type),
    itablePlace = ClassLayoutBuilder.InterfaceTablePlace.INVALID
)

private val ObjCTypeForKotlinType.kotlinMethods: List<ObjCMethodForKotlinMethod>
    get() = this.methods.filterIsInstance<ObjCMethodForKotlinMethod>()

internal data class DirectAdapterRequest(val implementation: IrFunction?, val base: IrFunction)

private fun ObjCExportCodeGenerator.createDirectAdapters(
        typeDeclaration: ObjCClassForKotlinClass,
        superClass: ObjCClassForKotlinClass?
): List<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter> {

    fun ObjCClassForKotlinClass.getAllRequiredDirectAdapters() = this.kotlinMethods.map { method ->
        DirectAdapterRequest(
                findImplementation(irClassSymbol.owner, method.baseMethod.owner, context),
                method.baseMethod.owner
        )
    }

    val inheritedAdapters = superClass?.getAllRequiredDirectAdapters().orEmpty()
    val requiredAdapters = typeDeclaration.getAllRequiredDirectAdapters() - inheritedAdapters

    return requiredAdapters.distinctBy { namer.getSelector(it.base.descriptor) }.map { createMethodAdapter(it) }
}

private fun findImplementation(irClass: IrClass, method: IrSimpleFunction, context: Context): IrSimpleFunction? {
    val override = irClass.simpleFunctions().singleOrNull {
        method in it.allOverriddenFunctions
    } ?: error("no implementation for ${method.descriptor}\nin ${irClass.descriptor}")
    return OverriddenFunctionInfo(override, method).getImplementation(context)
}

private inline fun ObjCExportCodeGenerator.generateObjCToKotlinSyntheticGetter(
        selector: String,
        block: FunctionGenerationContext.() -> Unit
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {

    val methodBridge = MethodBridge(
            MethodBridge.ReturnValue.Mapped(ReferenceBridge),
            MethodBridgeReceiver.Static, valueParameters = emptyList()
    )

    val encoding = getEncoding(methodBridge)
    val imp = generateFunction(codegen, objCFunctionType(context, methodBridge), "objc2kotlin") {
        block()
    }

    LLVMSetLinkage(imp, LLVMLinkage.LLVMPrivateLinkage)

    return ObjCToKotlinMethodAdapter(selector, encoding, constPointer(imp))
}

private fun ObjCExportCodeGenerator.createUnitInstanceAdapter() =
        generateObjCToKotlinSyntheticGetter(
                namer.getObjectInstanceSelector(context.builtIns.unit)
        ) {
            initRuntimeIfNeeded() // For instance methods it gets called when allocating.

            ret(callFromBridge(context.llvm.Kotlin_ObjCExport_convertUnit, listOf(codegen.theUnitInstanceRef.llvm)))
        }

private fun ObjCExportCodeGenerator.createObjectInstanceAdapter(
        irClass: IrClass
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(irClass.kind == ClassKind.OBJECT)
    assert(!irClass.isUnit())

    val selector = namer.getObjectInstanceSelector(irClass.descriptor)

    return generateObjCToKotlinSyntheticGetter(selector) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.
        val value = getObjectValue(irClass, startLocationInfo = null, exceptionHandler = ExceptionHandler.Caller)
        ret(kotlinToObjC(value, ReferenceBridge))
    }
}

private fun ObjCExportCodeGenerator.createEnumEntryAdapter(
        irEnumEntry: IrEnumEntry
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    val selector = namer.getEnumEntrySelector(irEnumEntry.descriptor)

    return generateObjCToKotlinSyntheticGetter(selector) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.

        val value = getEnumEntry(irEnumEntry, ExceptionHandler.Caller)
        ret(kotlinToObjC(value, ReferenceBridge))
    }
}

private fun List<CallableMemberDescriptor>.toMethods(): List<FunctionDescriptor> = this.flatMap {
    when (it) {
        is PropertyDescriptor -> listOfNotNull(it.getter, it.setter)
        is FunctionDescriptor -> listOf(it)
        else -> error(it)
    }
}

private fun objCFunctionType(context: Context, methodBridge: MethodBridge): LLVMTypeRef {
    val paramTypes = methodBridge.paramBridges.map { it.objCType }

    val returnType = methodBridge.returnBridge.objCType(context)

    return functionType(returnType, false, *(paramTypes.toTypedArray()))
}

private val ObjCValueType.llvmType: LLVMTypeRef get() = when (this) {
    ObjCValueType.BOOL -> int8Type
    ObjCValueType.UNICHAR -> int16Type
    ObjCValueType.CHAR -> int8Type
    ObjCValueType.SHORT -> int16Type
    ObjCValueType.INT -> int32Type
    ObjCValueType.LONG_LONG -> int64Type
    ObjCValueType.UNSIGNED_CHAR -> int8Type
    ObjCValueType.UNSIGNED_SHORT -> int16Type
    ObjCValueType.UNSIGNED_INT -> int32Type
    ObjCValueType.UNSIGNED_LONG_LONG -> int64Type
    ObjCValueType.FLOAT -> floatType
    ObjCValueType.DOUBLE -> doubleType
    ObjCValueType.POINTER -> kInt8Ptr
}

private val MethodBridgeParameter.objCType: LLVMTypeRef get() = when (this) {
    is MethodBridgeValueParameter.Mapped -> this.bridge.objCType
    is MethodBridgeReceiver -> ReferenceBridge.objCType
    MethodBridgeSelector -> int8TypePtr
    MethodBridgeValueParameter.ErrorOutParameter -> pointerType(ReferenceBridge.objCType)
    is MethodBridgeValueParameter.KotlinResultOutParameter -> pointerType(this.bridge.objCType)
}

private fun MethodBridge.ReturnValue.objCType(context: Context): LLVMTypeRef {
    return when (this) {
        MethodBridge.ReturnValue.Void -> voidType
        MethodBridge.ReturnValue.HashCode -> if (context.is64BitNSInteger()) int64Type else int32Type
        is MethodBridge.ReturnValue.Mapped -> this.bridge.objCType
        MethodBridge.ReturnValue.WithError.Success -> ObjCValueType.BOOL.llvmType

        MethodBridge.ReturnValue.Instance.InitResult,
        MethodBridge.ReturnValue.Instance.FactoryResult,
        is MethodBridge.ReturnValue.WithError.RefOrNull -> ReferenceBridge.objCType
    }
}

private val TypeBridge.objCType: LLVMTypeRef get() = when (this) {
    is ReferenceBridge, is BlockPointerBridge -> int8TypePtr
    is ValueTypeBridge -> this.objCValueType.llvmType
}

internal fun ObjCExportCodeGenerator.getEncoding(methodBridge: MethodBridge): String {
    var paramOffset = 0

    val params = buildString {
        methodBridge.paramBridges.forEach {
            append(it.objCEncoding)
            append(paramOffset)
            paramOffset += LLVMStoreSizeOfType(runtime.targetData, it.objCType).toInt()
        }
    }

    val targetFamily = context.config.target.family
    val returnTypeEncoding = methodBridge.returnBridge.getObjCEncoding(targetFamily)

    val paramSize = paramOffset
    return "$returnTypeEncoding$paramSize$params"
}

// https://developer.apple.com/documentation/objectivec/nsuinteger?language=objc
// `typedef unsigned long NSUInteger` on iOS, macOS, tvOS.
// `typedef unsigned int NSInteger` on watchOS.
private val Family.nsUIntegerEncoding: String get() = when (this) {
    Family.OSX,
    Family.IOS,
    Family.TVOS -> "L"
    Family.WATCHOS -> "I"
    else -> error("Unexpected target platform: $this")
}

private fun MethodBridge.ReturnValue.getObjCEncoding(targetFamily: Family): String = when (this) {
    MethodBridge.ReturnValue.Void -> "v"
    MethodBridge.ReturnValue.HashCode -> targetFamily.nsUIntegerEncoding
    is MethodBridge.ReturnValue.Mapped -> this.bridge.objCEncoding
    MethodBridge.ReturnValue.WithError.Success -> ObjCValueType.BOOL.encoding

    MethodBridge.ReturnValue.Instance.InitResult,
    MethodBridge.ReturnValue.Instance.FactoryResult,
    is MethodBridge.ReturnValue.WithError.RefOrNull -> ReferenceBridge.objCEncoding
}

private val MethodBridgeParameter.objCEncoding: String get() = when (this) {
    is MethodBridgeValueParameter.Mapped -> this.bridge.objCEncoding
    is MethodBridgeReceiver -> ReferenceBridge.objCEncoding
    MethodBridgeSelector -> ":"
    MethodBridgeValueParameter.ErrorOutParameter -> "^${ReferenceBridge.objCEncoding}"
    is MethodBridgeValueParameter.KotlinResultOutParameter -> "^${this.bridge.objCEncoding}"
}

private val TypeBridge.objCEncoding: String get() = when (this) {
    ReferenceBridge, is BlockPointerBridge -> "@"
    is ValueTypeBridge -> this.objCValueType.encoding
}

private fun Context.is64BitNSInteger(): Boolean = when (val target = this.config.target) {
    KonanTarget.IOS_X64,
    KonanTarget.IOS_ARM64,
    KonanTarget.TVOS_ARM64,
    KonanTarget.TVOS_X64,
    KonanTarget.MACOS_X64 -> true
    KonanTarget.WATCHOS_ARM64,
    KonanTarget.WATCHOS_ARM32,
    KonanTarget.WATCHOS_X86,
    KonanTarget.IOS_ARM32 -> false
    KonanTarget.ANDROID_X64,
    KonanTarget.ANDROID_X86,
    KonanTarget.ANDROID_ARM32,
    KonanTarget.ANDROID_ARM64,
    KonanTarget.LINUX_X64,
    KonanTarget.MINGW_X86,
    KonanTarget.MINGW_X64,
    KonanTarget.LINUX_ARM64,
    KonanTarget.LINUX_ARM32_HFP,
    KonanTarget.LINUX_MIPS32,
    KonanTarget.LINUX_MIPSEL32,
    KonanTarget.WASM32,
    is KonanTarget.ZEPHYR -> error("Target $target has no support for NSInteger type.")
    KonanTarget.WATCHOS_X64 -> error("Target $target is not supported.")
}

internal fun Context.is64BitLong(): Boolean = when (val target = this.config.target) {
    KonanTarget.IOS_X64,
    KonanTarget.IOS_ARM64,
    KonanTarget.TVOS_ARM64,
    KonanTarget.TVOS_X64,
    KonanTarget.ANDROID_X64,
    KonanTarget.ANDROID_ARM64,
    KonanTarget.LINUX_ARM64,
    KonanTarget.MINGW_X64,
    KonanTarget.LINUX_X64,
    KonanTarget.MACOS_X64 -> true
    KonanTarget.WATCHOS_ARM64,
    KonanTarget.WATCHOS_ARM32,
    KonanTarget.ANDROID_X86,
    KonanTarget.ANDROID_ARM32,
    KonanTarget.WATCHOS_X86,
    KonanTarget.MINGW_X86,
    KonanTarget.LINUX_ARM32_HFP,
    KonanTarget.LINUX_MIPS32,
    KonanTarget.LINUX_MIPSEL32,
    KonanTarget.WASM32,
    is KonanTarget.ZEPHYR,
    KonanTarget.IOS_ARM32 -> false
    KonanTarget.WATCHOS_X64 -> error("Target $target is not supported.")
}