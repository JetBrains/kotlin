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

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.*
import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.backend.konan.irasdescriptors.constructedClass
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.llvm.objc.ObjCCodeGenerator
import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class ObjCExportCodeGenerator(
        codegen: CodeGenerator,
        val namer: ObjCExportNamer,
        val mapper: ObjCExportMapper
) : ObjCCodeGenerator(codegen) {

    val runtime get() = codegen.runtime
    val staticData get() = codegen.staticData

    val rttiGenerator = RTTIGenerator(context)

    private val objcTerminate: LLVMValueRef by lazy {
        context.llvm.externalFunction(
                "objc_terminate",
                functionType(voidType, false),
                CurrentKonanModule
        ).also {
            setFunctionNoUnwind(it)
        }
    }

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

        ObjCValueType.CHAR, ObjCValueType.UNSIGNED_SHORT, ObjCValueType.SHORT,
        ObjCValueType.INT, ObjCValueType.LONG_LONG, ObjCValueType.FLOAT, ObjCValueType.DOUBLE -> value
    }

    private fun FunctionGenerationContext.objCToKotlin(
            value: LLVMValueRef,
            valueType: ObjCValueType
    ): LLVMValueRef = when (valueType) {
        ObjCValueType.BOOL -> icmpNe(value, Int8(0).llvm)

        ObjCValueType.CHAR, ObjCValueType.UNSIGNED_SHORT, ObjCValueType.SHORT,
        ObjCValueType.INT, ObjCValueType.LONG_LONG, ObjCValueType.FLOAT, ObjCValueType.DOUBLE -> value
    }

    fun FunctionGenerationContext.kotlinReferenceToObjC(value: LLVMValueRef) =
            callFromBridge(context.llvm.Kotlin_ObjCExport_refToObjC, listOf(value))

    fun FunctionGenerationContext.objCReferenceToKotlin(value: LLVMValueRef, resultLifetime: Lifetime) =
            callFromBridge(context.llvm.Kotlin_ObjCExport_refFromObjC, listOf(value), resultLifetime)

    fun FunctionGenerationContext.kotlinToObjC(
            value: LLVMValueRef,
            typeBridge: TypeBridge
    ): LLVMValueRef = when (typeBridge) {
        is ReferenceBridge -> kotlinReferenceToObjC(value)
        is ValueTypeBridge -> kotlinToObjC(value, typeBridge.objCValueType)
    }

    fun FunctionGenerationContext.objCToKotlin(
            value: LLVMValueRef,
            typeBridge: TypeBridge,
            resultLifetime: Lifetime
    ): LLVMValueRef = when (typeBridge) {
        is ReferenceBridge -> objCReferenceToKotlin(value, resultLifetime)
        is ValueTypeBridge -> objCToKotlin(value, typeBridge.objCValueType)
    }

    fun FunctionGenerationContext.initRuntimeIfNeeded() {
        callFromBridge(context.llvm.initRuntimeIfNeeded, emptyList())
    }

    inline fun FunctionGenerationContext.convertKotlin(
            genValue: (Lifetime) -> LLVMValueRef,
            actualType: KotlinType,
            expectedType: KotlinType,
            resultLifetime: Lifetime
    ): LLVMValueRef {

        val conversion = context.ir.symbols.getTypeConversion(actualType, expectedType)
                ?: return genValue(resultLifetime)

        val value = genValue(Lifetime.ARGUMENT)

        return callFromBridge(conversion.owner.llvmFunction, listOf(value), resultLifetime)
    }

    internal fun emitRtti(
            generatedClasses: Collection<ClassDescriptor>,
            topLevel: Map<FqName, List<CallableMemberDescriptor>>
    ) {
        val objCTypeAdapters = mutableListOf<ObjCTypeAdapter>()

        generatedClasses.forEach {
            objCTypeAdapters += createTypeAdapter(it)
            val className = namer.getClassOrProtocolName(it)
            val superClass = it.getSuperClassOrAny()
            val superClassName = namer.getClassOrProtocolName(superClass)

            dataGenerator.emitEmptyClass(className, superClassName)
            // Note: it is generated only to be visible for linker.
            // Methods will be added at runtime.
        }

        topLevel.forEach { fqName, declarations ->
            objCTypeAdapters += createTypeAdapterForPackage(fqName, declarations)
            dataGenerator.emitEmptyClass(namer.getPackageName(fqName), namer.kotlinAnyName)
        }

        dataGenerator.exportClass("KotlinMutableSet")
        dataGenerator.exportClass("KotlinMutableDictionary")

        emitSpecialClassesConvertions()

        objCTypeAdapters += createTypeAdapter(context.builtIns.any)

        val placedClassAdapters = mutableMapOf<String, ConstPointer>()
        val placedInterfaceAdapters = mutableMapOf<String, ConstPointer>()

        objCTypeAdapters.forEach { adapter ->
            val typeAdapter = staticData.placeGlobal("", adapter).pointer
            val descriptor = adapter.descriptor

            val descriptorToAdapter = if (descriptor?.isInterface == true) {
                placedInterfaceAdapters
            } else {
                // Objective-C class for Kotlin class or top-level declarations.
                placedClassAdapters
            }
            descriptorToAdapter[adapter.objCName] = typeAdapter

            if (descriptor != null) {
                setObjCExportTypeInfo(descriptor, typeAdapter = typeAdapter)
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
                staticData.placeGlobal(prefix, sortedAdaptersPointer, isExported = true)
                staticData.placeGlobal("${prefix}Num", Int32(sortedAdapters.size), isExported = true)
            }
        }

        emitSortedAdapters(placedClassAdapters, "Kotlin_ObjCExport_sortedClassAdapters")
        emitSortedAdapters(placedInterfaceAdapters, "Kotlin_ObjCExport_sortedProtocolAdapters")
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
            vtableIndex: Int,
            kotlinImpl: ConstPointer
    ) : Struct(
            runtime.kotlinToObjCMethodAdapter,
            staticData.cStringLiteral(selector),
            Int64(nameSignature),
            Int32(vtableIndex),
            kotlinImpl
    )

    inner class ObjCTypeAdapter(
            val descriptor: ClassDescriptor?,
            typeInfo: ConstPointer?,
            vtable: ConstPointer?,
            vtableSize: Int,
            methodTable: List<RTTIGenerator.MethodTableRecord>,
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

private fun ObjCExportCodeGenerator.setObjCExportTypeInfo(
        descriptor: ClassDescriptor,
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

    val irClass = context.ir.get(descriptor)
    val global = if (codegen.isExternal(irClass)) {
        // Note: this global replaces the external one with common linkage.
        staticData.createGlobal(
                writableTypeInfoType,
                irClass.writableTypeInfoSymbolName,
                isExported = true
        )
    } else {
        context.llvmDeclarations.forClass(irClass).writableTypeInfoGlobal!!.also {
            it.setLinkage(LLVMLinkage.LLVMExternalLinkage)
        }
    }

    global.setInitializer(writableTypeInfoValue)
}

private val ObjCExportCodeGenerator.kotlinToObjCFunctionType: LLVMTypeRef
    get() = functionType(int8TypePtr, false, codegen.kObjHeaderPtr)

private fun ObjCExportCodeGenerator.emitBoxConverter(objCValueType: ObjCValueType) {
    val valueType = objCValueType.kotlinValueType

    val symbols = context.ir.symbols

    val name = "${valueType.classFqName.shortName()}To${objCValueType.nsNumberName}"

    val converter = generateFunction(codegen, kotlinToObjCFunctionType, name) {
        val unboxFunction = symbols.getUnboxFunction(valueType).owner.llvmFunction
        val kotlinValue = callFromBridge(
                unboxFunction,
                listOf(param(0)),
                Lifetime.IRRELEVANT
        )

        val value = kotlinToObjC(kotlinValue, objCValueType)

        val nsNumber = genGetSystemClass("NSNumber")
        ret(genSendMessage(int8TypePtr, nsNumber, objCValueType.nsNumberFactorySelector, value))
    }

    LLVMSetLinkage(converter, LLVMLinkage.LLVMPrivateLinkage)

    val boxClass = symbols.boxClasses[valueType]!!
    setObjCExportTypeInfo(boxClass.descriptor, constPointer(converter))
}

private fun ObjCExportCodeGenerator.emitFunctionConverters() {
    val generator = BlockAdapterToFunctionGenerator(this)

    (0 .. mapper.maxFunctionTypeParameterCount).forEach { numberOfParameters ->
        val converter = generator.run { generateConvertFunctionToBlock(numberOfParameters) }
        setObjCExportTypeInfo(context.builtIns.getFunction(numberOfParameters), constPointer(converter))
    }
}

private fun ObjCExportCodeGenerator.generateKotlinFunctionAdapterToBlock(numberOfParameters: Int): ConstPointer {
    val irInterface = context.ir.symbols.functions[numberOfParameters].owner
    val invokeMethod = irInterface.declarations.filterIsInstance<IrSimpleFunction>()
            .single { it.name == OperatorNameConventions.INVOKE }

    val invokeImpl = generateKotlinFunctionImpl(invokeMethod.descriptor)

    return rttiGenerator.generateSyntheticInterfaceImpl(
            irInterface,
            mapOf(invokeMethod to invokeImpl)
    )
}

private fun ObjCExportCodeGenerator.emitKotlinFunctionAdaptersToBlock() {
    val ptr = staticData.placeGlobalArray(
            "",
            pointerType(runtime.typeInfoType),
            (0 .. mapper.maxFunctionTypeParameterCount).map {
                generateKotlinFunctionAdapterToBlock(it)
            }
    ).pointer.getElementPtr(0)

    // Note: this global replaces the weak global defined in runtime.
    staticData.placeGlobal("Kotlin_ObjCExport_functionAdaptersToBlock", ptr, isExported = true)
}

private fun ObjCExportCodeGenerator.emitSpecialClassesConvertions() {
    setObjCExportTypeInfo(
            context.builtIns.string,
            constPointer(context.llvm.Kotlin_ObjCExport_CreateNSStringFromKString)
    )

    setObjCExportTypeInfo(
            context.builtIns.list,
            constPointer(context.llvm.Kotlin_Interop_CreateNSArrayFromKList)
    )

    setObjCExportTypeInfo(
            context.builtIns.mutableList,
            constPointer(context.llvm.Kotlin_Interop_CreateNSMutableArrayFromKList)
    )

    setObjCExportTypeInfo(
            context.builtIns.set,
            constPointer(context.llvm.Kotlin_Interop_CreateNSSetFromKSet)
    )

    setObjCExportTypeInfo(
            context.builtIns.mutableSet,
            constPointer(context.llvm.Kotlin_Interop_CreateKotlinMutableSetFromKSet)
    )

    setObjCExportTypeInfo(
            context.builtIns.map,
            constPointer(context.llvm.Kotlin_Interop_CreateNSDictionaryFromKMap)
    )

    setObjCExportTypeInfo(
            context.builtIns.mutableMap,
            constPointer(context.llvm.Kotlin_Interop_CreateKotlinMutableDictonaryFromKMap)
    )

    ObjCValueType.values().forEach {
        emitBoxConverter(it)
    }

    emitFunctionConverters()

    emitKotlinFunctionAdaptersToBlock()
}

private inline fun ObjCExportCodeGenerator.generateObjCImpBy(
        methodBridge: MethodBridge,
        genBody: FunctionGenerationContext.() -> Unit
): LLVMValueRef {
    val result = LLVMAddFunction(context.llvmModule, "", objCFunctionType(methodBridge))!!

    generateFunction(codegen, result) {
        genBody()
    }

    LLVMSetLinkage(result, LLVMLinkage.LLVMPrivateLinkage)
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
    generateObjCImp(methodBridge) { args, resultLifetime, exceptionHandler ->
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
        callKotlin: FunctionGenerationContext.(
                args: List<LLVMValueRef>,
                resultLifetime: Lifetime,
                exceptionHandler: ExceptionHandler
        ) -> LLVMValueRef?
): LLVMValueRef = generateObjCImpBy(methodBridge) {

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
            callFromBridge(context.ir.symbols.objCExportTrapOnUndeclaredException.owner.llvmFunction, listOf(exception))
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
            assert(codegen.context.is64Bit())
            zext(targetResult!!, kInt64)
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
        target: ConstructorDescriptor,
        methodBridge: MethodBridge
): LLVMValueRef = generateObjCImp(methodBridge) { args, resultLifetime, exceptionHandler ->
    val targetIr = context.ir.get(target)

    val arrayInstance = callFromBridge(
            context.llvm.allocArrayFunction,
            listOf((targetIr as IrConstructor).constructedClass.llvmTypeInfoPtr, args.first()),
            resultLifetime = Lifetime.ARGUMENT
    )

    call(targetIr.llvmFunction, listOf(arrayInstance) + args, resultLifetime, exceptionHandler)
    arrayInstance
}

// TODO: cache bridges.
private fun ObjCExportCodeGenerator.generateKotlinToObjCBridge(
        descriptor: FunctionDescriptor,
        baseMethod: FunctionDescriptor
): ConstPointer {
    val methodBridge = mapper.bridgeMethod(baseMethod)

    val parameterToBase = descriptor.allParameters.zip(baseMethod.allParameters).toMap()

    val objcMsgSend = msgSender(objCFunctionType(methodBridge))

    val functionType = codegen.getLlvmFunctionType(context.ir.get(descriptor))

    val result = generateFunction(codegen, functionType, "") {
        var errorOutPtr: LLVMValueRef? = null
        var kotlinResultOutPtr: LLVMValueRef? = null
        lateinit var kotlinResultOutBridge: TypeBridge

        val parameters = descriptor.allParameters.mapIndexed { index, parameterDescriptor ->
            parameterDescriptor to param(index)
        }.toMap()

        val objCArgs = methodBridge.parametersAssociated(descriptor).map { (bridge, parameter) ->
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
                MethodBridgeSelector -> genSelector(namer.getSelector(baseMethod))

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
                assert(codegen.context.is64Bit())
                val low = trunc(targetResult, int32Type)
                val high = trunc(shr(targetResult, 32, signed = false), int32Type)
                xor(low, high)
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

        val baseReturnType = baseMethod.returnType!!
        val actualReturnType = descriptor.returnType!!

        val retVal = when {
            actualReturnType.isUnit() -> {
                genKotlinBaseMethodResult(Lifetime.ARGUMENT, methodBridge.returnBridge)
                null
            }
            baseReturnType.isUnit() -> {
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
        descriptor: FunctionDescriptor,
        baseMethod: FunctionDescriptor,
        functionName: String,
        vtableIndex: Int?
): ObjCExportCodeGenerator.KotlinToObjCMethodAdapter {

    val nameSignature = functionName.localHash.value
    val selector = namer.getSelector(baseMethod)

    val kotlinToObjC = generateKotlinToObjCBridge(
            descriptor,
            baseMethod
    ).bitcast(int8TypePtr)

    return KotlinToObjCMethodAdapter(selector, nameSignature, vtableIndex ?: -1, kotlinToObjC)
}

private fun ObjCExportCodeGenerator.createMethodVirtualAdapter(
        baseMethod: FunctionDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(mapper.isBaseMethod(baseMethod))

    val selector = namer.getSelector(baseMethod)

    val methodBridge = mapper.bridgeMethod(baseMethod)
    val objCToKotlin = constPointer(generateObjCImp(context.ir.get(baseMethod), methodBridge, isVirtual = true))
    return ObjCToKotlinMethodAdapter(selector, getEncoding(methodBridge), objCToKotlin)
}

private fun ObjCExportCodeGenerator.createMethodAdapter(
        implementation: FunctionDescriptor?,
        baseMethod: FunctionDescriptor
) = createMethodAdapter(DirectAdapterRequest(implementation?.let { context.ir.get(it) }, baseMethod))

private fun ObjCExportCodeGenerator.createMethodAdapter(
        request: DirectAdapterRequest
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter = this.directMethodAdapters.getOrPut(request) {

    val selectorName = namer.getSelector(request.base)
    val methodBridge = mapper.bridgeMethod(request.base)
    val objCEncoding = getEncoding(methodBridge)
    val objCToKotlin = constPointer(generateObjCImp(request.implementation, methodBridge))

    ObjCToKotlinMethodAdapter(selectorName, objCEncoding, objCToKotlin)
}

private fun ObjCExportCodeGenerator.createConstructorAdapter(
        descriptor: ConstructorDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter = createMethodAdapter(descriptor, descriptor)

private fun ObjCExportCodeGenerator.createArrayConstructorAdapter(
        descriptor: ConstructorDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    val selectorName = namer.getSelector(descriptor)
    val methodBridge = mapper.bridgeMethod(descriptor)
    val objCEncoding = getEncoding(methodBridge)
    val objCToKotlin = constPointer(generateObjCImpForArrayConstructor(descriptor, methodBridge))

    return ObjCToKotlinMethodAdapter(selectorName, objCEncoding, objCToKotlin)
}

private fun ObjCExportCodeGenerator.vtableIndex(descriptor: FunctionDescriptor): Int? {
    assert(descriptor.isOverridable)
    val classDescriptor = descriptor.containingDeclaration as ClassDescriptor
    return if (classDescriptor.isInterface) {
        null
    } else {
        context.getVtableBuilder(context.ir.get(classDescriptor))
                .vtableIndex(context.ir.get(descriptor) as IrSimpleFunction)
    }
}

private fun ObjCExportCodeGenerator.createTypeAdapterForPackage(
        fqName: FqName,
        declarations: List<CallableMemberDescriptor>
): ObjCExportCodeGenerator.ObjCTypeAdapter {
    val name = namer.getPackageName(fqName)

    val adapters = declarations.toMethods().map { createMethodAdapter(it, it) }

    return ObjCTypeAdapter(
            descriptor = null,
            typeInfo = null,
            vtable = null,
            vtableSize = -1,
            methodTable = emptyList(),
            objCName = name,
            directAdapters = emptyList(),
            classAdapters = adapters,
            virtualAdapters = emptyList(),
            reverseAdapters = emptyList()
    )
}

private fun ObjCExportCodeGenerator.createTypeAdapter(
        descriptor: ClassDescriptor
): ObjCExportCodeGenerator.ObjCTypeAdapter {
    val adapters = mutableListOf<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter>()
    val classAdapters = mutableListOf<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter>()

    if (descriptor != context.builtIns.any) {
        descriptor.constructors.forEach {
            if (mapper.shouldBeExposed(it)) {
                if (it.constructedClass.isArray) {
                    classAdapters += createArrayConstructorAdapter(it)
                } else {
                    adapters += createConstructorAdapter(it)
                }
            }
        }
    }

    val categoryMethods = mapper.getCategoryMembersFor(descriptor).toMethods()

    val exposedMethods = descriptor.contributedMethods.filter { mapper.shouldBeExposed(it) } + categoryMethods

    exposedMethods.forEach {
        adapters += createDirectAdapters(it)
    }

    val reverseAdapters = mutableListOf<ObjCExportCodeGenerator.KotlinToObjCMethodAdapter>()

    exposedMethods.forEach { method ->
        val baseMethods = mapper.getBaseMethods(method)
        val hasSelectorClash = baseMethods.map { namer.getSelector(it) }.distinct().size > 1

        if (method.isOverridable && !hasSelectorClash) {
            val baseMethod = baseMethods.first()

            val presentVtableBridges = mutableSetOf<Int?>(null)
            val presentMethodTableBridges = mutableSetOf<String>()

            val allOverriddenDescriptors = method.allOverriddenDescriptors.map { it.original }

            val (inherited, uninherited) = allOverriddenDescriptors.partition {
                it != method && mapper.shouldBeExposed(it)
            }

            inherited.forEach {
                presentVtableBridges += vtableIndex(it)
                presentMethodTableBridges += context.ir.get(it).functionName
            }

            uninherited.forEach {
                val vtableIndex = vtableIndex(it)
                val functionName = context.ir.get(it).functionName

                if (vtableIndex !in presentVtableBridges || functionName !in presentMethodTableBridges) {
                    presentVtableBridges += vtableIndex
                    presentMethodTableBridges += functionName
                    reverseAdapters += createReverseAdapter(it, baseMethod, functionName, vtableIndex)
                }
            }

        } else {
            // Mark it as non-overridable:
            baseMethods.distinctBy { namer.getSelector(it) }.forEach { base ->
                reverseAdapters += KotlinToObjCMethodAdapter(
                        namer.getSelector(base),
                        -1,
                        -1,
                        NullPointer(int8Type)
                )
            }

            // TODO: some fake-overrides can be skipped.
        }
    }

    val virtualAdapters = exposedMethods
            .filter { mapper.isBaseMethod(it) && it.isOverridable }
            .map { createMethodVirtualAdapter(it) }

    val irClass = context.ir.get(descriptor)
    val typeInfo = constPointer(codegen.typeInfoValue(irClass))
    val objCName = namer.getClassOrProtocolName(descriptor)

    val vtableSize = if (descriptor.kind == ClassKind.INTERFACE) {
        -1
    } else {
        context.getVtableBuilder(irClass).vtableEntries.size
    }

    val vtable = if (!descriptor.isInterface && !irClass.typeInfoHasVtableAttached) {
        staticData.placeGlobal("", rttiGenerator.vtable(irClass)).also {
            it.setConstant(true)
        }.pointer.getElementPtr(0)
    } else {
        null
    }

    val methodTable = if (!descriptor.isInterface && descriptor.isAbstract()) {
        rttiGenerator.methodTableRecords(irClass)
    } else {
        emptyList()
    }

    when (descriptor.kind) {
        ClassKind.OBJECT -> {
            classAdapters += if (descriptor.isUnit()) {
                createUnitInstanceAdapter()
            } else {
                createObjectInstanceAdapter(descriptor)
            }
        }
        ClassKind.ENUM_CLASS -> {
            descriptor.enumEntries.mapTo(classAdapters) {
                createEnumEntryAdapter(it)
            }
        }
        else -> {
            // Nothing special.
        }
    }

    return ObjCTypeAdapter(
            descriptor,
            typeInfo,
            vtable,
            vtableSize,
            methodTable,
            objCName,
            adapters,
            classAdapters,
            virtualAdapters,
            reverseAdapters
    )
}

internal data class DirectAdapterRequest(val implementation: IrFunction?, val base: FunctionDescriptor)

private fun ObjCExportCodeGenerator.createDirectAdapters(
        method: FunctionDescriptor
): List<ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter> {

    fun FunctionDescriptor.getAllRequiredDirectAdapters() = mapper.getBaseMethods(this).map { base ->
        val implementation = if (this.modality == Modality.ABSTRACT) {
            null
        } else {
            OverriddenFunctionDescriptor(
                    context.ir.get(this) as IrSimpleFunction,
                    context.ir.get(base) as IrSimpleFunction
            ).getImplementation(context)
        }
        DirectAdapterRequest(implementation, base)
    }

    val superClassMethod = method.overriddenDescriptors
            .atMostOne { !(it.containingDeclaration as ClassDescriptor).isInterface }?.original

    val inheritedAdapters = superClassMethod?.getAllRequiredDirectAdapters().orEmpty()
    val requiredAdapters = method.getAllRequiredDirectAdapters()

    return (requiredAdapters - inheritedAdapters)
            .distinctBy { namer.getSelector(it.base) }
            .map {
                createMethodAdapter(it)
            }
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
    val imp = generateFunction(codegen, objCFunctionType(methodBridge), "") {
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
        descriptor: ClassDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(descriptor.kind == ClassKind.OBJECT)
    assert(!descriptor.isUnit())

    val selector = namer.getObjectInstanceSelector(descriptor)

    return generateObjCToKotlinSyntheticGetter(selector) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.

        val value = getObjectValue(context.ir.get(descriptor), shared = false, locationInfo = null, exceptionHandler = ExceptionHandler.Caller)
        ret(kotlinToObjC(value, ReferenceBridge))
    }
}

private fun ObjCExportCodeGenerator.createEnumEntryAdapter(
        descriptor: ClassDescriptor
): ObjCExportCodeGenerator.ObjCToKotlinMethodAdapter {
    assert(descriptor.kind == ClassKind.ENUM_ENTRY)

    val selector = namer.getEnumEntrySelector(descriptor)

    return generateObjCToKotlinSyntheticGetter(selector) {
        initRuntimeIfNeeded() // For instance methods it gets called when allocating.

        val value = getEnumEntry(context.ir.getEnumEntry(descriptor), ExceptionHandler.Caller)
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

private fun objCFunctionType(methodBridge: MethodBridge): LLVMTypeRef {
    val paramTypes = methodBridge.paramBridges.map { it.objCType }

    val returnType = methodBridge.returnBridge.objCType

    return functionType(returnType, false, *(paramTypes.toTypedArray()))
}

private val ObjCValueType.llvmType: LLVMTypeRef get() = when (this) {
    ObjCValueType.BOOL -> int8Type
    ObjCValueType.CHAR -> int8Type
    ObjCValueType.UNSIGNED_SHORT -> int16Type
    ObjCValueType.SHORT -> int16Type
    ObjCValueType.INT -> int32Type
    ObjCValueType.LONG_LONG -> kInt64
    ObjCValueType.FLOAT -> LLVMFloatType()!!
    ObjCValueType.DOUBLE -> LLVMDoubleType()!!
}

private val MethodBridgeParameter.objCType: LLVMTypeRef get() = when (this) {
    is MethodBridgeValueParameter.Mapped -> this.bridge.objCType
    is MethodBridgeReceiver -> ReferenceBridge.objCType
    MethodBridgeSelector -> int8TypePtr
    MethodBridgeValueParameter.ErrorOutParameter -> pointerType(ReferenceBridge.objCType)
    is MethodBridgeValueParameter.KotlinResultOutParameter -> pointerType(this.bridge.objCType)
}

private val MethodBridge.ReturnValue.objCType: LLVMTypeRef get() = when (this) {
    MethodBridge.ReturnValue.Void -> voidType
    MethodBridge.ReturnValue.HashCode -> kInt64 // TODO: only for 64-bit platforms
    is MethodBridge.ReturnValue.Mapped -> this.bridge.objCType
    MethodBridge.ReturnValue.WithError.Success -> ObjCValueType.BOOL.llvmType

    MethodBridge.ReturnValue.Instance.InitResult,
    MethodBridge.ReturnValue.Instance.FactoryResult,
    is MethodBridge.ReturnValue.WithError.RefOrNull -> ReferenceBridge.objCType
}

private val TypeBridge.objCType: LLVMTypeRef get() = when (this) {
    is ReferenceBridge -> int8TypePtr
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

    val returnTypeEncoding = methodBridge.returnBridge.objCEncoding

    val paramSize = paramOffset
    return "$returnTypeEncoding$paramSize$params"
}

private val MethodBridge.ReturnValue.objCEncoding: String get() = when (this) {
    MethodBridge.ReturnValue.Void -> "v"
    MethodBridge.ReturnValue.HashCode -> "L" // NSUInteger = unsigned long; // TODO: `unsigned int` on watchOS
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
    ReferenceBridge -> "@"
    is ValueTypeBridge -> this.objCValueType.encoding
}

internal fun Context.is64Bit(): Boolean = this.config.target.architecture.bitness == 64
