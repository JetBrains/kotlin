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
import org.jetbrains.kotlin.name.CallableId
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

// TODO: KT-77494 - move this callable ids into more appropriate places.
private object CallableIds {

    // Native functions
    private val String.nativeCallableId get() = CallableId(KonanFqNames.packageName, Name.identifier(this))
    val processUnhandledException = "processUnhandledException".nativeCallableId
    val terminateWithUnhandledException = "terminateWithUnhandledException".nativeCallableId
    val immutableBlobOf = "immutableBlobOf".nativeCallableId

    // Internal functions
    private val String.internalCallableId get() = CallableId(RuntimeNames.kotlinNativeInternalPackageName, Name.identifier(this))
    val immutableBlobOfImpl = "immutableBlobOfImpl".internalCallableId
    val trapOnUndeclaredException = "trapOnUndeclaredException".internalCallableId
    val resumeContinuation = "resumeContinuation".internalCallableId
    val resumeContinuationWithException = "resumeContinuationWithException".internalCallableId
    val getCoroutineSuspended = "getCoroutineSuspended".internalCallableId
    val interceptedContinuation = "interceptedContinuation".internalCallableId
    val getNativeNullPtr = "getNativeNullPtr".internalCallableId
    val reinterpret = "reinterpret".internalCallableId
    val theUnitInstance = "theUnitInstance".internalCallableId
    val throwArithmeticException = "ThrowArithmeticException".internalCallableId
    val throwIndexOutOfBoundsException = "ThrowIndexOutOfBoundsException".internalCallableId
    val throwNullPointerException = "ThrowNullPointerException".internalCallableId
    val throwNoWhenBranchMatchedException = "ThrowNoWhenBranchMatchedException".internalCallableId
    val throwIrLinkageError = "ThrowIrLinkageError".internalCallableId
    val throwTypeCastException = "ThrowTypeCastException".internalCallableId
    val throwKotlinNothingValueException = "ThrowKotlinNothingValueException".internalCallableId
    val throwClassCastException = "ThrowClassCastException".internalCallableId
    val throwInvalidReceiverTypeException = "ThrowInvalidReceiverTypeException".internalCallableId
    val throwIllegalStateException = "ThrowIllegalStateException".internalCallableId
    val throwIllegalStateExceptionWithMessage = "ThrowIllegalStateExceptionWithMessage".internalCallableId
    val throwIllegalArgumentException = "ThrowIllegalArgumentException".internalCallableId
    val throwIllegalArgumentExceptionWithMessage = "ThrowIllegalArgumentExceptionWithMessage".internalCallableId
    val throwUninitializedPropertyAccessException = "ThrowUninitializedPropertyAccessException".internalCallableId
    val valuesForEnum = "valuesForEnum".internalCallableId
    val valueOfForEnum = "valueOfForEnum".internalCallableId
    val createUninitializedInstance = "createUninitializedInstance".internalCallableId
    val createUninitializedArray = "createUninitializedArray".internalCallableId
    val createEmptyString = "createEmptyString".internalCallableId
    val initInstance = "initInstance".internalCallableId
    val isSubtype = "isSubtype".internalCallableId
    val getContinuation = "getContinuation".internalCallableId
    val returnIfSuspended = "returnIfSuspended".internalCallableId
    val suspendCoroutineUninterceptedOrReturn = "suspendCoroutineUninterceptedOrReturn".internalCallableId
    val getCoroutineContext = "getCoroutineContext".internalCallableId
    val saveCoroutineState = "saveCoroutineState".internalCallableId
    val restoreCoroutineState = "restoreCoroutineState".internalCallableId
    val getObjectTypeInfo = "getObjectTypeInfo".internalCallableId
    val areEqualByValue = "areEqualByValue".internalCallableId
    val ieee754Equals = "ieee754Equals".internalCallableId
    fun inBoxCache(type: BoxCache) = "in${type.name.lowercase().replaceFirstChar(Char::uppercaseChar)}BoxCache".internalCallableId
    fun getCached(type: BoxCache) = "getCached${type.name.lowercase().replaceFirstChar(Char::uppercaseChar)}Box".internalCallableId

    // Interop functions
    private val String.interopCallableId get() = CallableId(InteropFqNames.packageName, Name.identifier(this))
    val nativePointedGetRawPointer = InteropFqNames.nativePointedGetRawPointerFunName.interopCallableId
    val cValueWrite = InteropFqNames.cValueWriteFunName.interopCallableId
    val cValueRead = InteropFqNames.cValueReadFunName.interopCallableId
    val allocType = InteropFqNames.allocTypeFunName.interopCallableId
    val typeOf = InteropFqNames.typeOfFunName.interopCallableId
    val cPointerGetRawValue = InteropFqNames.cPointerGetRawValueFunName.interopCallableId
    val allocObjCObject = InteropFqNames.allocObjCObjectFunName.interopCallableId
    val blockCopy = "Block_copy".interopCallableId
    val objcRelease = "objc_release".interopCallableId
    val objcRetain = "objc_retain".interopCallableId
    val objcRetainAutoreleaseReturnValue = "objc_retainAutoreleaseReturnValue".interopCallableId
    val createObjCObjectHolder = "createObjCObjectHolder".interopCallableId
    val createKotlinObjectHolder = "createKotlinObjectHolder".interopCallableId
    val unwrapKotlinObjectHolderImpl = "unwrapKotlinObjectHolderImpl".interopCallableId
    val createObjCSuperStruct = "createObjCSuperStruct".interopCallableId
    val getMessenger = "getMessenger".interopCallableId
    val getMessengerStret = "getMessengerStret".interopCallableId
    val getObjCClass = InteropFqNames.getObjCClassFunName.interopCallableId
    val objCObjectSuperInitCheck = InteropFqNames.objCObjectSuperInitCheckFunName.interopCallableId
    val objCObjectInitBy = InteropFqNames.objCObjectInitByFunName.interopCallableId
    val objCObjectRawPtr = InteropFqNames.objCObjectRawPtrFunName.interopCallableId
    val interpretObjCPointer = InteropFqNames.interpretObjCPointerFunName.interopCallableId
    val interpretObjCPointerOrNull = InteropFqNames.interpretObjCPointerOrNullFunName.interopCallableId
    val interpretNullablePointed = InteropFqNames.interpretNullablePointedFunName.interopCallableId
    val interpretCPointer = InteropFqNames.interpretCPointerFunName.interopCallableId
    val createForeignException = "CreateForeignException".interopCallableId
    val readBits = "readBits".interopCallableId
    val writeBits = "writeBits".interopCallableId

    // Reflection functions
    private val String.reflectionCallableId get() = CallableId(StandardNames.KOTLIN_REFLECT_FQ_NAME, Name.identifier(this))
    val typeOfReflection = "typeOf".reflectionCallableId

    // Built-ins functions
    private val String.builtInsCallableId get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
    val isAssertionThrowingErrorEnabled = "isAssertionThrowingErrorEnabled".builtInsCallableId
    val isAssertionArgumentEvaluationEnabled = "isAssertionArgumentEvaluationEnabled".builtInsCallableId
    val getOrThrow = "getOrThrow".builtInsCallableId

    // Special stdlib public functions
    val enumEntries = CallableId(FqName("kotlin.enums"), Name.identifier("enumEntries"))
    val println = CallableId(FqName("kotlin.io"), Name.identifier("println"))
    val executeImpl = CallableId(KonanFqNames.packageName.child(Name.identifier("concurrent")), Name.identifier("executeImpl"))
    val createCleaner = CallableId(KonanFqNames.packageName.child(Name.identifier("ref")), Name.identifier("createCleaner"))
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

    val immutableBlobOf = CallableIds.immutableBlobOf.functionSymbol()
    val immutableBlobOfImpl = CallableIds.immutableBlobOfImpl.functionSymbol()

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

    val processUnhandledException = CallableIds.processUnhandledException.functionSymbol()
    val terminateWithUnhandledException = CallableIds.terminateWithUnhandledException.functionSymbol()

    val interopNativePointedGetRawPointer = CallableIds.nativePointedGetRawPointer.functionSymbol {
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
    val interopCValueWrite = CallableIds.cValueWrite.functionSymbol {
        symbolFinder.isExtensionReceiverClass(it, interopCValue)
    }
    val interopCValueRead = CallableIds.cValueRead.functionSymbol {
        symbolFinder.getValueParametersCount(it) == 1
    }
    val interopAllocType = CallableIds.allocType.functionSymbol {
        symbolFinder.getTypeParametersCount(it) == 0
    }

    val interopTypeOf = CallableIds.typeOf.functionSymbol()

    val interopCPointerGetRawValue = CallableIds.cPointerGetRawValue.functionSymbol {
        symbolFinder.isExtensionReceiverClass(it, interopCPointer)
    }

    val interopAllocObjCObject = CallableIds.allocObjCObject.functionSymbol()

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

    val interopBlockCopy = CallableIds.blockCopy.functionSymbol()

    val interopObjCRelease = CallableIds.objcRelease.functionSymbol()

    val interopObjCRetain = CallableIds.objcRetain.functionSymbol()

    val interopObjcRetainAutoreleaseReturnValue = CallableIds.objcRetainAutoreleaseReturnValue.functionSymbol()

    val interopCreateObjCObjectHolder = CallableIds.createObjCObjectHolder.functionSymbol()

    val interopCreateKotlinObjectHolder = CallableIds.createKotlinObjectHolder.functionSymbol()
    val interopUnwrapKotlinObjectHolderImpl = CallableIds.unwrapKotlinObjectHolderImpl.functionSymbol()

    val interopCreateObjCSuperStruct = CallableIds.createObjCSuperStruct.functionSymbol()

    val interopGetMessenger = CallableIds.getMessenger.functionSymbol()
    val interopGetMessengerStret = CallableIds.getMessengerStret.functionSymbol()

    val interopGetObjCClass = CallableIds.getObjCClass.functionSymbol()
    val interopObjCObjectSuperInitCheck = CallableIds.objCObjectSuperInitCheck.functionSymbol()
    val interopObjCObjectInitBy = CallableIds.objCObjectInitBy.functionSymbol()
    val interopObjCObjectRawValueGetter = CallableIds.objCObjectRawPtr.functionSymbol()

    val interopNativePointedRawPtrGetter = symbolFinder.findMemberPropertyGetter(ClassIds.nativePointed.classSymbol(), Name.identifier(InteropFqNames.nativePointedRawPtrPropertyName))!!

    val interopCPointerRawValue: IrPropertySymbol = symbolFinder.findMemberProperty(ClassIds.interopCPointer.classSymbol(), Name.identifier(InteropFqNames.cPointerRawValuePropertyName))!!

    val interopInterpretObjCPointer = CallableIds.interpretObjCPointer.functionSymbol()
    val interopInterpretObjCPointerOrNull = CallableIds.interpretObjCPointerOrNull.functionSymbol()
    val interopInterpretNullablePointed = CallableIds.interpretNullablePointed.functionSymbol()
    val interopInterpretCPointer = CallableIds.interpretCPointer.functionSymbol()

    val createForeignException = CallableIds.createForeignException.functionSymbol()

    val interopCEnumVar = ClassIds.interopCEnumVar.classSymbol()

    val nativeMemUtils = ClassIds.nativeMemUtils.classSymbol()
    val nativeHeap = ClassIds.nativeHeap.classSymbol()

    val cStuctVar = ClassIds.cStuctVar.classSymbol()
    val cStructVarConstructorSymbol = symbolFinder.findPrimaryConstructor(cStuctVar)!!
    val structVarTypePrimaryConstructor = symbolFinder.findPrimaryConstructor(ClassIds.cStructVarType.classSymbol())!!

    val interopGetPtr = symbolFinder.findTopLevelPropertyGetter(InteropFqNames.packageName, "ptr") {
        symbolFinder.isTypeParameterUpperBoundClass(it, 0, interopCPointed)
    }

    val readBits = CallableIds.readBits.functionSymbol()
    val writeBits = CallableIds.writeBits.functionSymbol()

    val objCExportTrapOnUndeclaredException = CallableIds.trapOnUndeclaredException.functionSymbol()
    val objCExportResumeContinuation = CallableIds.resumeContinuation.functionSymbol()
    val objCExportResumeContinuationWithException = CallableIds.resumeContinuationWithException.functionSymbol()
    val objCExportGetCoroutineSuspended = CallableIds.getCoroutineSuspended.functionSymbol()
    val objCExportInterceptedContinuation = CallableIds.interceptedContinuation.functionSymbol()

    val getNativeNullPtr = CallableIds.getNativeNullPtr.functionSymbol()

    val boxCachePredicates = BoxCache.entries.associateWith {
        CallableIds.inBoxCache(it).functionSymbol()
    }

    val boxCacheGetters = BoxCache.entries.associateWith {
        CallableIds.getCached(it).functionSymbol()
    }

    val immutableBlob = ClassIds.immutableBlob.classSymbol()

    val executeImpl = CallableIds.executeImpl.functionSymbol()
    val createCleaner = CallableIds.createCleaner.functionSymbol()

    val areEqualByValueFunctions = CallableIds.areEqualByValue.functionSymbols()

    // TODO: this is strange. It should be a map from IrClassSymbol
    val areEqualByValue: Map<PrimitiveBinaryType, IrSimpleFunctionSymbol> by lazy {
        areEqualByValueFunctions.associateBy {
            it.owner.parameters[0].type.computePrimitiveBinaryTypeOrNull()!!
        }
    }

    val reinterpret = CallableIds.reinterpret.functionSymbol()

    val theUnitInstance = CallableIds.theUnitInstance.functionSymbol()

    val ieee754Equals = CallableIds.ieee754Equals.functionSymbols()

    val equals = symbolFinder.findMemberFunction(any, Name.identifier("equals"))!!

    val throwArithmeticException = CallableIds.throwArithmeticException.functionSymbol()

    val throwIndexOutOfBoundsException = CallableIds.throwIndexOutOfBoundsException.functionSymbol()

    override val throwNullPointerException = CallableIds.throwNullPointerException.functionSymbol()

    val throwNoWhenBranchMatchedException = CallableIds.throwNoWhenBranchMatchedException.functionSymbol()
    val throwIrLinkageError = CallableIds.throwIrLinkageError.functionSymbol()

    override val throwTypeCastException = CallableIds.throwTypeCastException.functionSymbol()

    override val throwKotlinNothingValueException = CallableIds.throwKotlinNothingValueException.functionSymbol()

    val throwClassCastException = CallableIds.throwClassCastException.functionSymbol()

    val throwInvalidReceiverTypeException = CallableIds.throwInvalidReceiverTypeException.functionSymbol()
    val throwIllegalStateException = CallableIds.throwIllegalStateException.functionSymbol()
    val throwIllegalStateExceptionWithMessage = CallableIds.throwIllegalStateExceptionWithMessage.functionSymbol()
    val throwIllegalArgumentException = CallableIds.throwIllegalArgumentException.functionSymbol()
    val throwIllegalArgumentExceptionWithMessage = CallableIds.throwIllegalArgumentExceptionWithMessage.functionSymbol()


    override val throwUninitializedPropertyAccessException = CallableIds.throwUninitializedPropertyAccessException.functionSymbol()

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

    val valuesForEnum = CallableIds.valuesForEnum.functionSymbol()

    val valueOfForEnum = CallableIds.valueOfForEnum.functionSymbol()

    val createEnumEntries = CallableIds.enumEntries.functionSymbol {
        symbolFinder.getValueParametersCount(it) == 1 && symbolFinder.isValueParameterClass(it, 0, array)
    }

    val enumEntriesInterface = ClassIds.enumEntries.classSymbol()

    val createUninitializedInstance = CallableIds.createUninitializedInstance.functionSymbol()

    val createUninitializedArray = CallableIds.createUninitializedArray.functionSymbol()

    val createEmptyString = CallableIds.createEmptyString.functionSymbol()

    val initInstance = CallableIds.initInstance.functionSymbol()

    val isSubtype = CallableIds.isSubtype.functionSymbol()

    val println = CallableIds.println.functionSymbol {
        symbolFinder.getValueParametersCount(it) == 1 && symbolFinder.isValueParameterClass(it, 0, string)
    }

    override val getContinuation = CallableIds.getContinuation.functionSymbol()

    override val continuationClass = ClassIds.continuation.classSymbol()

    override val returnIfSuspended = CallableIds.returnIfSuspended.functionSymbol()

    override val suspendCoroutineUninterceptedOrReturn = CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()

    override val coroutineContextGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_PACKAGE_FQ_NAME, "coroutineContext", null)

    override val coroutineGetContext = CallableIds.getCoroutineContext.functionSymbol()

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = ClassIds.baseContinuationImpl.classSymbol()

    val restrictedContinuationImpl = ClassIds.restrictedContinuationImpl.classSymbol()

    val continuationImpl = ClassIds.continuationImpl.classSymbol()

    val invokeSuspendFunction = symbolFinder.findMemberFunction(baseContinuationImpl, Name.identifier("invokeSuspend"))!!

    override val coroutineSuspendedGetter =
            findTopLevelPropertyGetter(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, StandardNames.COROUTINE_SUSPENDED_NAME.identifier, null)

    val saveCoroutineState = CallableIds.saveCoroutineState.functionSymbol()
    val restoreCoroutineState = CallableIds.restoreCoroutineState.functionSymbol()

    val cancellationException = ClassIds.cancellationException.classSymbol()

    val kotlinResult = ClassIds.kotlinResult.classSymbol()

    val kotlinResultGetOrThrow = CallableIds.getOrThrow.functionSymbol {
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
    val getObjectTypeInfo = CallableIds.getObjectTypeInfo.functionSymbol()
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
    val typeOf = CallableIds.typeOfReflection.functionSymbol()

    val threadLocal = ClassIds.threadLocal.classSymbol()

    val eagerInitialization = ClassIds.eagerInitialization.classSymbol()

    val noInline = ClassIds.noInline.classSymbol()

    val enumVarConstructorSymbol = symbolFinder.findPrimaryConstructor(ClassIds.interopCEnumVar.classSymbol())!!
    val primitiveVarTypePrimaryConstructor = symbolFinder.findPrimaryConstructor(ClassIds.interopCPrimitiveVarType.classSymbol())!!

    val isAssertionThrowingErrorEnabled = CallableIds.isAssertionThrowingErrorEnabled.functionSymbol()
    val isAssertionArgumentEvaluationEnabled = CallableIds.isAssertionArgumentEvaluationEnabled.functionSymbol()

    private fun findTopLevelPropertyGetter(packageName: FqName, name: String, extensionReceiverClass: IrClassSymbol?) =
            symbolFinder.findTopLevelPropertyGetter(packageName, name) { symbolFinder.isExtensionReceiverClass(it, extensionReceiverClass) }

    private fun ClassId.classSymbol() = symbolFinder.findClass(this) ?: error("Class $this is not found")
    private fun CallableId.functionSymbols() = symbolFinder.findFunctions(this).toList()
    private inline fun CallableId.functionSymbol(condition: (IrFunctionSymbol) -> Boolean = { true }) = symbolFinder.topLevelFunction(this, condition)

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
