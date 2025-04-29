/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

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
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object KonanNameConventions {
    val setWithoutBoundCheck = Name.special("<setWithoutBoundCheck>")
    val getWithoutBoundCheck = Name.special("<getWithoutBoundCheck>")
}

// TODO: KT-77494 - move this class ids into more appropriate places.
private object ClassIds {
    // Native classes
    private val String.nativeClassId get() = ClassId(KonanFqNames.packageName, Name.identifier(this))
    val immutableBlob = "ImmutableBlob".nativeClassId

    // Annotations
    val threadLocal = ClassId.topLevel(KonanFqNames.threadLocal)
    val eagerInitialization = ClassId.topLevel(KonanFqNames.eagerInitialization)
    val noInline = ClassId.topLevel(KonanFqNames.noInline)
    val symbolName = ClassId.topLevel(RuntimeNames.symbolNameAnnotation)
    val filterExceptions = ClassId.topLevel(RuntimeNames.filterExceptions)
    val exportForCppRuntime = ClassId.topLevel(RuntimeNames.exportForCppRuntime)
    val typedIntrinsic = ClassId.topLevel(RuntimeNames.typedIntrinsicAnnotation)

    // Internal classes
    private val String.internalClassId get() = ClassId(RuntimeNames.kotlinNativeInternalPackageName, Name.identifier(this))
    val refClass = "Ref".internalClassId
    val kFunctionImpl = "KFunctionImpl".internalClassId
    val kFunctionDescription = "KFunctionDescription".internalClassId
    val kFunctionDescriptionCorrect = kFunctionDescription.createNestedClassId(Name.identifier("Correct"))
    val kFunctionDescriptionLinkageError = kFunctionDescription.createNestedClassId(Name.identifier("LinkageError"))
    val kSuspendFunctionImpl = "KSuspendFunctionImpl".internalClassId
    val kProperty0Impl = "KProperty0Impl".internalClassId
    val kProperty1Impl = "KProperty1Impl".internalClassId
    val kProperty2Impl = "KProperty2Impl".internalClassId
    val kMutableProperty0Impl = "KMutableProperty0Impl".internalClassId
    val kMutableProperty1Impl = "KMutableProperty1Impl".internalClassId
    val kMutableProperty2Impl = "KMutableProperty2Impl".internalClassId
    val kLocalDelegatedPropertyImpl = "KLocalDelegatedPropertyImpl".internalClassId
    val kLocalDelegatedMutablePropertyImpl = "KLocalDelegatedMutablePropertyImpl".internalClassId
    val kClassImpl = "KClassImpl".internalClassId
    val kClassUnsupportedImpl = "KClassUnsupportedImpl".internalClassId
    val kTypeParameterImpl = "KTypeParameterImpl".internalClassId
    val kTypeImpl = "KTypeImpl".internalClassId
    val kTypeImplForTypeParametersWithRecursiveBounds = "KTypeImplForTypeParametersWithRecursiveBounds".internalClassId
    val kTypeProjectionList = "KTypeProjectionList".internalClassId
    val defaultConstructorMarker = "DefaultConstructorMarker".internalClassId
    val nativePtr = "NativePtr".internalClassId
    val functionAdapter = "FunctionAdapter".internalClassId

    // Interop classes
    private val String.interopClassId get() = ClassId(InteropFqNames.packageName, Name.identifier(this))

    val nativePointed = InteropFqNames.nativePointedName.interopClassId
    val interopCPointer = InteropFqNames.cPointerName.interopClassId
    val interopCPointed = InteropFqNames.cPointedName.interopClassId
    val interopCVariable = InteropFqNames.cVariableName.interopClassId
    val interopMemScope = InteropFqNames.memScopeName.interopClassId
    val interopCValue = InteropFqNames.cValueName.interopClassId
    val interopCValues = InteropFqNames.cValuesName.interopClassId
    val interopCValuesRef = InteropFqNames.cValuesRefName.interopClassId
    val interopCOpaque = InteropFqNames.cOpaqueName.interopClassId
    val interopObjCObject = InteropFqNames.objCObjectName.interopClassId
    val interopObjCObjectBase = InteropFqNames.objCObjectBaseName.interopClassId
    val interopObjCObjectBaseMeta = InteropFqNames.objCObjectBaseMetaName.interopClassId
    val interopObjCClass = InteropFqNames.objCClassName.interopClassId
    val interopObjCClassOf = InteropFqNames.objCClassOfName.interopClassId
    val interopObjCProtocol = InteropFqNames.objCProtocolName.interopClassId
    val interopForeignObjCObject = InteropFqNames.foreignObjCObjectName.interopClassId
    val interopCEnumVar = InteropFqNames.cEnumVarName.interopClassId
    val interopCPrimitiveVar = InteropFqNames.cPrimitiveVarName.interopClassId
    val interopCPrimitiveVarType = interopCPrimitiveVar.createNestedClassId(Name.identifier(InteropFqNames.TypeName))
    val nativeMemUtils = InteropFqNames.nativeMemUtilsName.interopClassId
    val nativeHeap = InteropFqNames.nativeHeapName.interopClassId
    val cStuctVar = InteropFqNames.cStructVarName.interopClassId
    val cStructVarType = cStuctVar.createNestedClassId(Name.identifier(InteropFqNames.TypeName))
    val objCMethodImp = InteropFqNames.objCMethodImpName.interopClassId

    // Internal interop classes
    private val String.internalInteropClassId get() = ClassId(RuntimeNames.kotlinxCInteropInternalPackageName, Name.identifier(this))
    val kObjCClassImpl = "ObjectiveCKClassImpl".internalInteropClassId

    // Reflection classes
    private val String.reflectionClassId get() = ClassId(StandardNames.KOTLIN_REFLECT_FQ_NAME, Name.identifier(this))
    val kMutableProperty0 = "KMutableProperty0".reflectionClassId
    val kMutableProperty1 = "KMutableProperty1".reflectionClassId
    val kMutableProperty2 = "KMutableProperty2".reflectionClassId
    val kType = "KType".reflectionClassId

    // Special standard library classes
    val stringBuilder = ClassId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier("StringBuilder"))
    val enumEntries = ClassId(FqName("kotlin.enums"), Name.identifier("EnumEntries"))
    val continuation = ClassId(StandardNames.COROUTINES_PACKAGE_FQ_NAME, Name.identifier("Continuation"))
    val cancellationException = ClassId.topLevel(KonanFqNames.cancellationException)
    val kotlinResult = ClassId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("Result"))

    // Internal coroutines classes
    private val String.internalCoroutinesClassId get() = ClassId(RuntimeNames.kotlinNativeCoroutinesInternalPackageName, Name.identifier(this))
    val baseContinuationImpl = "BaseContinuationImpl".internalCoroutinesClassId
    val restrictedContinuationImpl = "RestrictedContinuationImpl".internalCoroutinesClassId
    val continuationImpl = "ContinuationImpl".internalCoroutinesClassId

    // Test classes
    private val String.internalTestClassId get() = ClassId(RuntimeNames.kotlinNativeInternalTestPackageName, Name.identifier(this))
    val baseClassSuite = "BaseClassSuite".internalTestClassId
    val topLevelSuite = "TopLevelSuite".internalTestClassId
    val testFunctionKind = "TestFunctionKind".internalTestClassId
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
    private val nativePtr = ClassIds.nativePtr.classSymbol()
    val nativePointed = ClassIds.nativePointed.classSymbol()
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

    val symbolName = ClassIds.symbolName.classSymbol()
    val filterExceptions = ClassIds.filterExceptions.classSymbol()
    val exportForCppRuntime = ClassIds.exportForCppRuntime.classSymbol()
    val typedIntrinsic = ClassIds.typedIntrinsic.classSymbol()

    val objCMethodImp = ClassIds.objCMethodImp.classSymbol()

    val processUnhandledException = nativeFunction("processUnhandledException")
    val terminateWithUnhandledException = nativeFunction("terminateWithUnhandledException")

    val interopNativePointedGetRawPointer = interopFunction(InteropFqNames.nativePointedGetRawPointerFunName) {
        symbolFinder.isExtensionReceiverClass(it, nativePointed)
    }

    val interopCPointer = ClassIds.interopCPointer.classSymbol()
    val interopCPointed = ClassIds.interopCPointed.classSymbol()
    val interopCVariable = ClassIds.interopCVariable.classSymbol()
    val interopCstr = findTopLevelPropertyGetter(InteropFqNames.packageName, InteropFqNames.cstrPropertyName, string)
    val interopWcstr = findTopLevelPropertyGetter(InteropFqNames.packageName, InteropFqNames.wcstrPropertyName, string)
    val interopMemScope = ClassIds.interopMemScope.classSymbol()
    val interopCValue = ClassIds.interopCValue.classSymbol()
    val interopCValues = ClassIds.interopCValues.classSymbol()
    val interopCValuesRef = ClassIds.interopCValuesRef.classSymbol()
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

    val interopForeignObjCObject = ClassIds.interopForeignObjCObject.classSymbol()

    // These are possible supertypes of forward declarations - we need to reference them explicitly to force their deserialization.
    // TODO: Do it lazily.
    val interopCOpaque = ClassIds.interopCOpaque.classSymbol()
    val interopObjCObject = ClassIds.interopObjCObject.classSymbol()
    val interopObjCObjectBase = ClassIds.interopObjCObjectBase.classSymbol()
    val interopObjCObjectBaseMeta = ClassIds.interopObjCObjectBaseMeta.classSymbol()
    val interopObjCClass = ClassIds.interopObjCClass.classSymbol()
    val interopObjCClassOf = ClassIds.interopObjCClassOf.classSymbol()
    val interopObjCProtocol = ClassIds.interopObjCProtocol.classSymbol()

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

    val interopNativePointedRawPtrGetter = symbolFinder.findMemberPropertyGetter(ClassIds.nativePointed.classSymbol(), Name.identifier(InteropFqNames.nativePointedRawPtrPropertyName))!!

    val interopCPointerRawValue: IrPropertySymbol = symbolFinder.findMemberProperty(ClassIds.interopCPointer.classSymbol(), Name.identifier(InteropFqNames.cPointerRawValuePropertyName))!!

    val interopInterpretObjCPointer = interopFunction(InteropFqNames.interpretObjCPointerFunName)
    val interopInterpretObjCPointerOrNull = interopFunction(InteropFqNames.interpretObjCPointerOrNullFunName)
    val interopInterpretNullablePointed = interopFunction(InteropFqNames.interpretNullablePointedFunName)
    val interopInterpretCPointer = interopFunction(InteropFqNames.interpretCPointerFunName)

    val createForeignException = interopFunction("CreateForeignException")

    val interopCEnumVar = ClassIds.interopCEnumVar.classSymbol()

    val nativeMemUtils = ClassIds.nativeMemUtils.classSymbol()
    val nativeHeap = ClassIds.nativeHeap.classSymbol()

    val cStuctVar = ClassIds.cStuctVar.classSymbol()
    val cStructVarConstructorSymbol = symbolFinder.findPrimaryConstructor(cStuctVar)!!
    val structVarTypePrimaryConstructor = symbolFinder.findPrimaryConstructor(ClassIds.cStructVarType.classSymbol())!!

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

    val immutableBlob = ClassIds.immutableBlob.classSymbol()

    val executeImpl = symbolFinder.topLevelFunction(KonanFqNames.packageName.child(Name.identifier("concurrent")), "executeImpl")
    val createCleaner = symbolFinder.topLevelFunction(KonanFqNames.packageName.child(Name.identifier("ref")), "createCleaner")

    val areEqualByValueFunctions = internalFunctions("areEqualByValue")

    // TODO: this is strange. It should be a map from IrClassSymbol
    val areEqualByValue: Map<PrimitiveBinaryType, IrSimpleFunctionSymbol> by lazy {
        areEqualByValueFunctions.associateBy {
            it.owner.parameters[0].type.computePrimitiveBinaryTypeOrNull()!!
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

    override val stringBuilder = ClassIds.stringBuilder.classSymbol()

    override val defaultConstructorMarker = ClassIds.defaultConstructorMarker.classSymbol()

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

    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol> by lazy { arrayContentEquals.mapKeys { it.key.defaultType } }

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

    val enumEntriesInterface = ClassIds.enumEntries.classSymbol()

    val createUninitializedInstance = internalFunction("createUninitializedInstance")

    val createUninitializedArray = internalFunction("createUninitializedArray")

    val createEmptyString = internalFunction("createEmptyString")

    val initInstance = internalFunction("initInstance")

    val isSubtype = internalFunction("isSubtype")

    val println = symbolFinder.topLevelFunction(FqName("kotlin.io"), "println") {
        symbolFinder.getValueParametersCount(it) == 1 && symbolFinder.isValueParameterClass(it, 0, string)
    }

    override val getContinuation = internalFunction("getContinuation")

    override val continuationClass = ClassIds.continuation.classSymbol()

    override val returnIfSuspended = internalFunction("returnIfSuspended")

    override val suspendCoroutineUninterceptedOrReturn = internalFunction("suspendCoroutineUninterceptedOrReturn")

    override val coroutineContextGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_PACKAGE_FQ_NAME, "coroutineContext", null)

    override val coroutineGetContext = internalFunction("getCoroutineContext")

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = ClassIds.baseContinuationImpl.classSymbol()

    val restrictedContinuationImpl = ClassIds.restrictedContinuationImpl.classSymbol()

    val continuationImpl = ClassIds.continuationImpl.classSymbol()

    val invokeSuspendFunction = symbolFinder.findMemberFunction(baseContinuationImpl, Name.identifier("invokeSuspend"))!!

    override val coroutineSuspendedGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, StandardNames.COROUTINE_SUSPENDED_NAME.identifier, null)

    val saveCoroutineState = internalFunction("saveCoroutineState")
    val restoreCoroutineState = internalFunction("restoreCoroutineState")

    val cancellationException = ClassIds.cancellationException.classSymbol()

    val kotlinResult = ClassIds.kotlinResult.classSymbol()

    val kotlinResultGetOrThrow = symbolFinder.topLevelFunction(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, "getOrThrow") {
        symbolFinder.isExtensionReceiverClass(it, kotlinResult)
    }

    override val functionAdapter = ClassIds.functionAdapter.classSymbol()

    val refClass = ClassIds.refClass.classSymbol()
    val kFunctionImpl = ClassIds.kFunctionImpl.classSymbol()
    val kFunctionDescription = ClassIds.kFunctionDescription.classSymbol()
    val kFunctionDescriptionCorrect = ClassIds.kFunctionDescriptionCorrect.classSymbol()
    val kFunctionDescriptionLinkageError = ClassIds.kFunctionDescriptionLinkageError.classSymbol()
    val kSuspendFunctionImpl = ClassIds.kSuspendFunctionImpl.classSymbol()

    val kMutableProperty0 = ClassIds.kMutableProperty0.classSymbol()
    val kMutableProperty1 = ClassIds.kMutableProperty1.classSymbol()
    val kMutableProperty2 = ClassIds.kMutableProperty2.classSymbol()

    val kProperty0Impl = ClassIds.kProperty0Impl.classSymbol()
    val kProperty1Impl = ClassIds.kProperty1Impl.classSymbol()
    val kProperty2Impl = ClassIds.kProperty2Impl.classSymbol()
    val kMutableProperty0Impl = ClassIds.kMutableProperty0Impl.classSymbol()
    val kMutableProperty1Impl = ClassIds.kMutableProperty1Impl.classSymbol()
    val kMutableProperty2Impl = ClassIds.kMutableProperty2Impl.classSymbol()

    val kLocalDelegatedPropertyImpl = ClassIds.kLocalDelegatedPropertyImpl.classSymbol()
    val kLocalDelegatedMutablePropertyImpl = ClassIds.kLocalDelegatedMutablePropertyImpl.classSymbol()

    val kType = ClassIds.kType.classSymbol()
    val getObjectTypeInfo = internalFunction("getObjectTypeInfo")
    val kClassImpl = ClassIds.kClassImpl.classSymbol()
    val kClassImplConstructor = symbolFinder.findPrimaryConstructor(kClassImpl)!!
    val kClassImplIntrinsicConstructor = symbolFinder.findNoParametersConstructor(kClassImpl)!!
    val kObjCClassImpl = ClassIds.kObjCClassImpl.classSymbol()
    val kObjCClassImplConstructor = symbolFinder.findPrimaryConstructor(kObjCClassImpl)!!
    val kObjCClassImplIntrinsicConstructor = symbolFinder.findNoParametersConstructor(kObjCClassImpl)!!
    val kClassUnsupportedImpl = ClassIds.kClassUnsupportedImpl.classSymbol()
    val kTypeParameterImpl = ClassIds.kTypeParameterImpl.classSymbol()
    val kTypeImpl = ClassIds.kTypeImpl.classSymbol()
    val kTypeImplForTypeParametersWithRecursiveBounds = ClassIds.kTypeImplForTypeParametersWithRecursiveBounds.classSymbol()
    val kTypeProjectionList = ClassIds.kTypeProjectionList.classSymbol()
    val typeOf = reflectionFunction("typeOf")

    val threadLocal = ClassIds.threadLocal.classSymbol()

    val eagerInitialization = ClassIds.eagerInitialization.classSymbol()

    val noInline = ClassIds.noInline.classSymbol()

    val enumVarConstructorSymbol = symbolFinder.findPrimaryConstructor(ClassIds.interopCEnumVar.classSymbol())!!
    val primitiveVarTypePrimaryConstructor = symbolFinder.findPrimaryConstructor(ClassIds.interopCPrimitiveVarType.classSymbol())!!

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


    private fun ClassId.classSymbol() = symbolFinder.findClass(this) ?: error("Class $this is not found")

    val baseClassSuite = ClassIds.baseClassSuite.classSymbol()
    val topLevelSuite = ClassIds.topLevelSuite.classSymbol()
    val testFunctionKind = ClassIds.testFunctionKind.classSymbol()

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
