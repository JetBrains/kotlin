/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.COROUTINE_SUSPENDED_NAME
import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.ir.Symbols
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.lower.TestProcessorFunctionKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KonanNameConventions {
    val setWithoutBoundCheck = Name.special("<setWithoutBoundCheck>")
    val getWithoutBoundCheck = Name.special("<getWithoutBoundCheck>")
}

@OptIn(InternalSymbolFinderAPI::class, InternalKotlinNativeApi::class)
class KonanSymbols(
        context: ErrorReportingContext,
        irBuiltIns: IrBuiltIns,
        config: CompilerConfiguration,
) : Symbols(irBuiltIns) {
    val entryPoint = run {
        if (config.get(KonanConfigKeys.PRODUCE) != CompilerOutputKind.PROGRAM) return@run null

        val entryPoint = FqName(config.get(KonanConfigKeys.ENTRY) ?: when (config.get(KonanConfigKeys.GENERATE_TEST_RUNNER)) {
            TestRunnerKind.MAIN_THREAD -> "kotlin.native.internal.test.main"
            TestRunnerKind.WORKER -> "kotlin.native.internal.test.worker"
            TestRunnerKind.MAIN_THREAD_NO_EXIT -> "kotlin.native.internal.test.mainNoExit"
            else -> "main"
        })

        val entryName = entryPoint.shortName()
        val packageName = entryPoint.parent()

        fun IrSimpleFunctionSymbol.isArrayStringMain() =
                symbolFinder.getValueParametersCount(this) == 1 &&
                        symbolFinder.isValueParameterClass(this, 0, array) &&
                        symbolFinder.isValueParameterTypeArgumentClass(this, 0, 0, string)

        fun IrSimpleFunctionSymbol.isNoArgsMain() = symbolFinder.getValueParametersCount(this) == 0

        val candidates = symbolFinder.findFunctions(entryName, packageName)
                .filter {
                    symbolFinder.isReturnClass(it, unit) &&
                            symbolFinder.getTypeParametersCount(it) == 0 &&
                            symbolFinder.getVisibility(it).isPublicAPI
                }

        val main = candidates.singleOrNull { it.isArrayStringMain() } ?: candidates.singleOrNull { it.isNoArgsMain() }
        if (main == null) context.reportCompilationError("Could not find '$entryName' in '$packageName' package.")
        if (symbolFinder.isSuspend(main)) context.reportCompilationError("Entry point can not be a suspend function.")
        main
    }

    val nothing get() = irBuiltIns.nothingClass
    val throwable get() = irBuiltIns.throwableClass
    val enum get() = irBuiltIns.enumClass
    private val nativePtr = internalClass(NATIVE_PTR_NAME)
    val nativePointed = interopClass(InteropFqNames.nativePointedName)
    val nativePtrType = nativePtr.typeWith(arguments = emptyList())

    val immutableBlobOf = nativeFunction(IMMUTABLE_BLOB_OF)
    val immutableBlobOfImpl = internalFunction("immutableBlobOfImpl")

    val signedIntegerClasses = setOf(byte, short, int, long)
    val unsignedIntegerClasses = setOf(uByte!!, uShort!!, uInt!!, uLong!!)

    val allIntegerClasses = signedIntegerClasses + unsignedIntegerClasses

    val unsignedToSignedOfSameBitWidth = unsignedIntegerClasses.associateWith {
        when (it) {
            uByte -> byte
            uShort -> short
            uInt -> int
            uLong -> long
            else -> error(it.toString())
        }
    }

    val integerConversions = allIntegerClasses.flatMap { fromClass ->
        allIntegerClasses.map { toClass ->
            val name = Name.identifier("to${symbolFinder.getName(toClass).asString().replaceFirstChar(Char::uppercaseChar)}")
            val symbol = if (fromClass in signedIntegerClasses && toClass in unsignedIntegerClasses) {
                irBuiltIns.getNonBuiltInFunctionsByExtensionReceiver(name, "kotlin")[fromClass]!!
            } else {
                symbolFinder.findMemberFunction(fromClass, name)!!
            }

            (fromClass to toClass) to symbol
        }
    }.toMap()

    val symbolName = symbolFinder.topLevelClass(RuntimeNames.symbolNameAnnotation)
    val filterExceptions = symbolFinder.topLevelClass(RuntimeNames.filterExceptions)
    val exportForCppRuntime = symbolFinder.topLevelClass(RuntimeNames.exportForCppRuntime)
    val typedIntrinsic = symbolFinder.topLevelClass(RuntimeNames.typedIntrinsicAnnotation)

    val objCMethodImp = interopClass(InteropFqNames.objCMethodImpName)

    val processUnhandledException = nativeFunction("processUnhandledException")
    val terminateWithUnhandledException = nativeFunction("terminateWithUnhandledException")

    val interopNativePointedGetRawPointer = interopFunction(InteropFqNames.nativePointedGetRawPointerFunName) {
        symbolFinder.isExtensionReceiverClass(it, nativePointed)
    }

    val interopCPointer = interopClass(InteropFqNames.cPointerName)
    val interopCPointed = interopClass(InteropFqNames.cPointedName)
    val interopCstr = findTopLevelPropertyGetter(InteropFqNames.packageName, InteropFqNames.cstrPropertyName, string)
    val interopWcstr = findTopLevelPropertyGetter(InteropFqNames.packageName, InteropFqNames.wcstrPropertyName, string)
    val interopMemScope = interopClass(InteropFqNames.memScopeName)
    val interopCValue = interopClass(InteropFqNames.cValueName)
    val interopCValuesRef = interopClass(InteropFqNames.cValuesRefName)
    val interopCValueWrite = interopFunction(InteropFqNames.cValueWriteFunName) {
        symbolFinder.isExtensionReceiverClass(it, interopCValue)
    }
    val interopCValueRead = interopFunction(InteropFqNames.cValueReadFunName) {
        symbolFinder.getValueParametersCount(it) == 1
    }
    val interopAllocType = interopFunction(InteropFqNames.allocTypeFunName) {
        symbolFinder.getTypeParametersCount(it) == 0
    }

    val interopTypeOf = interopFunction(InteropFqNames.typeOfFunName)

    val interopCPointerGetRawValue = interopFunction(InteropFqNames.cPointerGetRawValueFunName) {
        symbolFinder.isExtensionReceiverClass(it, interopCPointer)
    }

    val interopAllocObjCObject = interopFunction(InteropFqNames.allocObjCObjectFunName)

    val interopForeignObjCObject = interopClass(InteropFqNames.foreignObjCObjectName)

    // These are possible supertypes of forward declarations - we need to reference them explicitly to force their deserialization.
    // TODO: Do it lazily.
    val interopCOpaque = interopClass(InteropFqNames.cOpaqueName)
    val interopObjCObject = interopClass(InteropFqNames.objCObjectName)
    val interopObjCObjectBase = interopClass(InteropFqNames.objCObjectBaseName)
    val interopObjCObjectBaseMeta = interopClass(InteropFqNames.objCObjectBaseMetaName)
    val interopObjCClass = interopClass(InteropFqNames.objCClassName)
    val interopObjCClassOf = interopClass(InteropFqNames.objCClassOfName)
    val interopObjCProtocol = interopClass(InteropFqNames.objCProtocolName)

    val interopBlockCopy = interopFunction("Block_copy")

    val interopObjCRelease = interopFunction("objc_release")

    val interopObjCRetain = interopFunction("objc_retain")

    val interopObjcRetainAutoreleaseReturnValue = interopFunction("objc_retainAutoreleaseReturnValue")

    val interopCreateObjCObjectHolder = interopFunction("createObjCObjectHolder")

    val interopCreateKotlinObjectHolder = interopFunction("createKotlinObjectHolder")
    val interopUnwrapKotlinObjectHolderImpl = interopFunction("unwrapKotlinObjectHolderImpl")

    val interopCreateObjCSuperStruct = interopFunction("createObjCSuperStruct")

    val interopGetMessenger = interopFunction("getMessenger")
    val interopGetMessengerStret = interopFunction("getMessengerStret")

    val interopGetObjCClass = interopFunction(InteropFqNames.getObjCClassFunName)
    val interopObjCObjectSuperInitCheck = interopFunction(InteropFqNames.objCObjectSuperInitCheckFunName)
    val interopObjCObjectInitBy = interopFunction(InteropFqNames.objCObjectInitByFunName)
    val interopObjCObjectRawValueGetter = interopFunction(InteropFqNames.objCObjectRawPtrFunName)

    val interopNativePointedRawPtrGetter = symbolFinder.findMemberPropertyGetter(interopClass(InteropFqNames.nativePointedName), Name.identifier(InteropFqNames.nativePointedRawPtrPropertyName))!!

    val interopCPointerRawValue: IrPropertySymbol = symbolFinder.findMemberProperty(interopClass(InteropFqNames.cPointerName), Name.identifier(InteropFqNames.cPointerRawValuePropertyName))!!

    val interopInterpretObjCPointer = interopFunction(InteropFqNames.interpretObjCPointerFunName)
    val interopInterpretObjCPointerOrNull = interopFunction(InteropFqNames.interpretObjCPointerOrNullFunName)
    val interopInterpretNullablePointed = interopFunction(InteropFqNames.interpretNullablePointedFunName)
    val interopInterpretCPointer = interopFunction(InteropFqNames.interpretCPointerFunName)

    val createForeignException = interopFunction("CreateForeignException")

    val interopCEnumVar = interopClass("CEnumVar")

    val nativeMemUtils = interopClass(InteropFqNames.nativeMemUtilsName)
    val nativeHeap = interopClass(InteropFqNames.nativeHeapName)

    val cStuctVar = interopClass(InteropFqNames.cStructVarName)
    val cStructVarConstructorSymbol = symbolFinder.findPrimaryConstructor(cStuctVar)!!
    val structVarPrimaryConstructor = symbolFinder.findPrimaryConstructor(symbolFinder.findNestedClass(cStuctVar, Name.identifier(InteropFqNames.TypeName))!!)!!

    val interopGetPtr = symbolFinder.findTopLevelPropertyGetter(InteropFqNames.packageName, "ptr") {
        symbolFinder.isTypeParameterUpperBoundClass(it, 0, interopCPointed)
    }

    val readBits = interopFunction("readBits")
    val writeBits = interopFunction("writeBits")

    val objCExportTrapOnUndeclaredException = internalFunction("trapOnUndeclaredException")
    val objCExportResumeContinuation = internalFunction("resumeContinuation")
    val objCExportResumeContinuationWithException = internalFunction("resumeContinuationWithException")
    val objCExportGetCoroutineSuspended = internalFunction("getCoroutineSuspended")
    val objCExportInterceptedContinuation = internalFunction("interceptedContinuation")

    val getNativeNullPtr = internalFunction("getNativeNullPtr")

    val boxCachePredicates = BoxCache.entries.associateWith {
        internalFunction("in${it.name.lowercase().replaceFirstChar(Char::uppercaseChar)}BoxCache")
    }

    val boxCacheGetters = BoxCache.entries.associateWith {
        internalFunction("getCached${it.name.lowercase().replaceFirstChar(Char::uppercaseChar)}Box")
    }

    val immutableBlob = nativeClass("ImmutableBlob")

    val executeImpl = symbolFinder.topLevelFunction(KonanFqNames.packageName.child(Name.identifier("concurrent")), "executeImpl")
    val createCleaner = symbolFinder.topLevelFunction(KonanFqNames.packageName.child(Name.identifier("ref")), "createCleaner")

    val areEqualByValueFunctions = internalFunctions("areEqualByValue")

    // TODO: this is strange. It should be a map from IrClassSymbol
    val areEqualByValue: Map<PrimitiveBinaryType, IrSimpleFunctionSymbol> by lazy {
        areEqualByValueFunctions.associateBy {
            it.owner.valueParameters[0].type.computePrimitiveBinaryTypeOrNull()!!
        }
    }

    val reinterpret = internalFunction("reinterpret")

    val theUnitInstance = internalFunction("theUnitInstance")

    val ieee754Equals = internalFunctions("ieee754Equals").toList()

    val equals = symbolFinder.findMemberFunction(any, Name.identifier("equals"))!!

    val throwArithmeticException = internalFunction("ThrowArithmeticException")

    val throwIndexOutOfBoundsException = internalFunction("ThrowIndexOutOfBoundsException")

    override val throwNullPointerException = internalFunction("ThrowNullPointerException")

    val throwNoWhenBranchMatchedException = internalFunction("ThrowNoWhenBranchMatchedException")
    val throwIrLinkageError = internalFunction("ThrowIrLinkageError")

    override val throwTypeCastException = internalFunction("ThrowTypeCastException")

    override val throwKotlinNothingValueException = internalFunction("ThrowKotlinNothingValueException")

    val throwClassCastException = internalFunction("ThrowClassCastException")

    val throwInvalidReceiverTypeException = internalFunction("ThrowInvalidReceiverTypeException")
    val throwIllegalStateException = internalFunction("ThrowIllegalStateException")
    val throwIllegalStateExceptionWithMessage = internalFunction("ThrowIllegalStateExceptionWithMessage")
    val throwIllegalArgumentException = internalFunction("ThrowIllegalArgumentException")
    val throwIllegalArgumentExceptionWithMessage = internalFunction("ThrowIllegalArgumentExceptionWithMessage")


    override val throwUninitializedPropertyAccessException = internalFunction("ThrowUninitializedPropertyAccessException")

    override val stringBuilder = symbolFinder.topLevelClass(StandardNames.TEXT_PACKAGE_FQ_NAME, "StringBuilder")

    override val defaultConstructorMarker = internalClass("DefaultConstructorMarker")

    private fun arrayToExtensionSymbolMap(name: String, filter: (IrFunctionSymbol) -> Boolean = { true }) =
            arrays.associateWith { classSymbol ->
                symbolFinder.topLevelFunction(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, name) { function ->
                    symbolFinder.isExtensionReceiverClass(function, classSymbol) && !symbolFinder.isExpect(function) && filter(function)
                }
            }

    val arrayContentToString = arrayToExtensionSymbolMap("contentToString") {
        symbolFinder.isExtensionReceiverNullable(it) == true
    }
    val arrayContentHashCode = arrayToExtensionSymbolMap("contentHashCode") {
        symbolFinder.isExtensionReceiverNullable(it) == true
    }
    val arrayContentEquals = arrayToExtensionSymbolMap("contentEquals") {
        symbolFinder.isExtensionReceiverNullable(it) == true
    }

    override val arraysContentEquals by lazy { arrayContentEquals.mapKeys { it.key.defaultType } }

    val copyInto = arrayToExtensionSymbolMap("copyInto")
    val copyOf = arrayToExtensionSymbolMap("copyOf") { symbolFinder.getValueParametersCount(it) == 0 }

    val arrayGet = arrays.associateWith { symbolFinder.findMemberFunction(it, Name.identifier("get"))!! }

    val arraySet = arrays.associateWith { symbolFinder.findMemberFunction(it, Name.identifier("set"))!! }

    val arraySize = arrays.associateWith { symbolFinder.findMemberPropertyGetter(it, Name.identifier("size"))!! }

    val valuesForEnum = internalFunction("valuesForEnum")

    val valueOfForEnum = internalFunction("valueOfForEnum")

    val createEnumEntries = symbolFinder.topLevelFunction(FqName("kotlin.enums"), "enumEntries") {
        symbolFinder.getValueParametersCount(it) == 1 && symbolFinder.isValueParameterClass(it, 0, array)
    }

    val enumEntriesInterface = symbolFinder.topLevelClass(FqName("kotlin.enums"), "EnumEntries")

    val createUninitializedInstance = internalFunction("createUninitializedInstance")

    val createUninitializedArray = internalFunction("createUninitializedArray")

    val createEmptyString = internalFunction("createEmptyString")

    val initInstance = internalFunction("initInstance")

    val isSubtype = internalFunction("isSubtype")

    val println = symbolFinder.topLevelFunction(FqName("kotlin.io"), "println") {
        symbolFinder.getValueParametersCount(it) == 1 && symbolFinder.isValueParameterClass(it, 0, string)
    }

    override val getContinuation = internalFunction("getContinuation")

    override val continuationClass = symbolFinder.topLevelClass(StandardNames.COROUTINES_PACKAGE_FQ_NAME, "Continuation")

    override val returnIfSuspended = internalFunction("returnIfSuspended")

    override val suspendCoroutineUninterceptedOrReturn = internalFunction("suspendCoroutineUninterceptedOrReturn")

    override val coroutineContextGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_PACKAGE_FQ_NAME, "coroutineContext", null)

    override val coroutineGetContext = internalFunction("getCoroutineContext")

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = internalCoroutinesClass("BaseContinuationImpl")

    val restrictedContinuationImpl = internalCoroutinesClass("RestrictedContinuationImpl")

    val continuationImpl = internalCoroutinesClass("ContinuationImpl")

    val invokeSuspendFunction = symbolFinder.findMemberFunction(baseContinuationImpl, Name.identifier("invokeSuspend"))!!

    override val coroutineSuspendedGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, COROUTINE_SUSPENDED_NAME.identifier, null)

    val saveCoroutineState = internalFunction("saveCoroutineState")
    val restoreCoroutineState = internalFunction("restoreCoroutineState")

    val cancellationException = symbolFinder.topLevelClass(KonanFqNames.cancellationException)

    val kotlinResult = symbolFinder.topLevelClass(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "Result")

    val kotlinResultGetOrThrow = symbolFinder.topLevelFunction(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "getOrThrow") {
        symbolFinder.isExtensionReceiverClass(it, kotlinResult)
    }

    override val functionAdapter = internalClass("FunctionAdapter")

    val refClass = internalClass("Ref")
    val kFunctionImpl = internalClass("KFunctionImpl")
    val kFunctionDescription = internalClass("KFunctionDescription")
    val kSuspendFunctionImpl = internalClass("KSuspendFunctionImpl")

    val kMutableProperty0 = reflectionClass("KMutableProperty0")
    val kMutableProperty1 = reflectionClass("KMutableProperty1")
    val kMutableProperty2 = reflectionClass("KMutableProperty2")

    val kProperty0Impl = internalClass("KProperty0Impl")
    val kProperty1Impl = internalClass("KProperty1Impl")
    val kProperty2Impl = internalClass("KProperty2Impl")
    val kMutableProperty0Impl = internalClass("KMutableProperty0Impl")
    val kMutableProperty1Impl = internalClass("KMutableProperty1Impl")
    val kMutableProperty2Impl = internalClass("KMutableProperty2Impl")

    val kLocalDelegatedPropertyImpl = internalClass("KLocalDelegatedPropertyImpl")
    val kLocalDelegatedMutablePropertyImpl = internalClass("KLocalDelegatedMutablePropertyImpl")

    val kType = reflectionClass("KType")
    val getObjectTypeInfo = internalFunction("getObjectTypeInfo")
    val kClassImpl = internalClass("KClassImpl")
    val kClassImplConstructor = symbolFinder.findPrimaryConstructor(kClassImpl)!!
    val kClassImplIntrinsicConstructor = symbolFinder.findNoParametersConstructor(kClassImpl)!!
    val kObjCClassImpl = symbolFinder.topLevelClass(RuntimeNames.kotlinxCInteropInternalPackageName, "ObjectiveCKClassImpl")
    val kObjCClassImplConstructor = symbolFinder.findPrimaryConstructor(kObjCClassImpl)!!
    val kObjCClassImplIntrinsicConstructor = symbolFinder.findNoParametersConstructor(kObjCClassImpl)!!
    val kClassUnsupportedImpl = internalClass("KClassUnsupportedImpl")
    val kTypeParameterImpl = internalClass("KTypeParameterImpl")
    val kTypeImpl = internalClass("KTypeImpl")
    val kTypeImplForTypeParametersWithRecursiveBounds = internalClass("KTypeImplForTypeParametersWithRecursiveBounds")
    val kTypeProjectionList = internalClass("KTypeProjectionList")
    val typeOf = reflectionFunction("typeOf")

    val threadLocal = symbolFinder.topLevelClass(KonanFqNames.threadLocal)

    val eagerInitialization = symbolFinder.topLevelClass(KonanFqNames.eagerInitialization)

    val noInline = symbolFinder.topLevelClass(KonanFqNames.noInline)

    val enumVarConstructorSymbol = symbolFinder.findPrimaryConstructor(interopClass(InteropFqNames.cEnumVarName))!!
    val primitiveVarPrimaryConstructor = symbolFinder.findPrimaryConstructor(symbolFinder.findNestedClass(interopClass(InteropFqNames.cPrimitiveVarName), Name.identifier(InteropFqNames.TypeName))!!)!!

    val isAssertionThrowingErrorEnabled = symbolFinder.topLevelFunction(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "isAssertionThrowingErrorEnabled")
    val isAssertionArgumentEvaluationEnabled = symbolFinder.topLevelFunction(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "isAssertionArgumentEvaluationEnabled")

    private fun findTopLevelPropertyGetter(packageName: FqName, name: String, extensionReceiverClass: IrClassSymbol?) =
            symbolFinder.findTopLevelPropertyGetter(packageName, name) { symbolFinder.isExtensionReceiverClass(it, extensionReceiverClass) }

    private fun internalFunctions(name: String) = symbolFinder.topLevelFunctions(RuntimeNames.kotlinNativeInternalPackageName, name)
    private inline fun nativeFunction(
            name: String,
            condition: (IrFunctionSymbol) -> Boolean = { true }
    ) = symbolFinder.topLevelFunction(KonanFqNames.packageName, name, condition)

    private inline fun internalFunction(
            name: String,
            condition: (IrFunctionSymbol) -> Boolean = { true }
    ) = symbolFinder.topLevelFunction(RuntimeNames.kotlinNativeInternalPackageName, name, condition)

    private inline fun interopFunction(
            name: String,
            condition: (IrFunctionSymbol) -> Boolean = { true }
    ) = symbolFinder.topLevelFunction(InteropFqNames.packageName, name, condition)

    private inline fun reflectionFunction(
            name: String,
            condition: (IrFunctionSymbol) -> Boolean = { true }
    ) = symbolFinder.topLevelFunction(StandardNames.KOTLIN_REFLECT_FQ_NAME, name, condition)


    private fun nativeClass(name: String) = symbolFinder.topLevelClass(KonanFqNames.packageName, name)
    private fun internalClass(name: String) = symbolFinder.topLevelClass(RuntimeNames.kotlinNativeInternalPackageName, name)
    private fun interopClass(name: String) = symbolFinder.topLevelClass(InteropFqNames.packageName, name)
    private fun reflectionClass(name: String) = symbolFinder.topLevelClass(StandardNames.KOTLIN_REFLECT_FQ_NAME, name)

    private fun internalCoroutinesClass(name: String) = symbolFinder.topLevelClass(RuntimeNames.kotlinNativeCoroutinesInternalPackageName, name)
    private fun konanTestClass(name: String) = symbolFinder.topLevelClass(RuntimeNames.kotlinNativeInternalTestPackageName, name)

    fun kFunctionN(n: Int) = irBuiltIns.kFunctionN(n).symbol

    fun kSuspendFunctionN(n: Int) = irBuiltIns.kSuspendFunctionN(n).symbol

    fun getKFunctionType(returnType: IrType, parameterTypes: List<IrType>) =
            kFunctionN(parameterTypes.size).typeWith(parameterTypes + returnType)

    val baseClassSuite = konanTestClass("BaseClassSuite")
    val topLevelSuite = konanTestClass("TopLevelSuite")
    val testFunctionKind = konanTestClass("TestFunctionKind")

    override val getWithoutBoundCheckName: Name? = KonanNameConventions.getWithoutBoundCheck

    override val setWithoutBoundCheckName: Name? = KonanNameConventions.setWithoutBoundCheck

    private val testFunctionKindCache by lazy {
        TestProcessorFunctionKind.entries.associateWith { kind ->
            if (kind.runtimeKindString.isEmpty())
                null
            else
                testFunctionKind.owner.declarations
                        .filterIsInstance<IrEnumEntry>()
                        .single { it.name == Name.identifier(kind.runtimeKindString) }
                        .symbol
        }
    }

    fun getTestFunctionKind(kind: TestProcessorFunctionKind) = testFunctionKindCache[kind]!!
}
