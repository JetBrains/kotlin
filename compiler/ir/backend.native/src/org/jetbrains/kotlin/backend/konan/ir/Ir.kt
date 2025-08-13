/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.ir

import org.jetbrains.kotlin.backend.common.ErrorReportingContext
import org.jetbrains.kotlin.backend.common.ir.FrontendNativeSymbols
import org.jetbrains.kotlin.backend.common.ir.KlibSymbols
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.lower.TestProcessorFunctionKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrEnumEntry
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.util.OperatorNameConventions

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
    val nativePtr = "NativePtr".internalClassId
    val functionAdapter = "FunctionAdapter".internalClassId

    // Interop classes
    private val String.interopClassId get() = ClassId(InteropFqNames.packageName, Name.identifier(this))
    private val String.interopInternalClassId get() = ClassId(InteropFqNames.internalPackageName, Name.identifier(this))

    val cToKotlinBridge = InteropFqNames.cToKotlinBridgeName.interopInternalClassId
    val kotlinToCBridge = InteropFqNames.kotlinToCBridgeName.interopInternalClassId
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
    val objectiveCKClassImpl = "ObjectiveCKClassImpl".internalInteropClassId

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
    val throwIrLinkageError = CallableId(StandardClassIds.BASE_INTERNAL_PACKAGE, Name.identifier("throwIrLinkageError"))
    val throwTypeCastException = "ThrowTypeCastException".internalCallableId
    val throwKotlinNothingValueException = "ThrowKotlinNothingValueException".internalCallableId
    val throwClassCastException = "ThrowClassCastException".internalCallableId
    val throwInvalidReceiverTypeException = "ThrowInvalidReceiverTypeException".internalCallableId
    val throwIllegalStateException = "ThrowIllegalStateException".internalCallableId
    val throwIllegalStateExceptionWithMessage = "ThrowIllegalStateExceptionWithMessage".internalCallableId
    val throwIllegalArgumentException = "ThrowIllegalArgumentException".internalCallableId
    val throwIllegalArgumentExceptionWithMessage = "ThrowIllegalArgumentExceptionWithMessage".internalCallableId
    val valuesForEnum = "valuesForEnum".internalCallableId
    val valueOfForEnum = "valueOfForEnum".internalCallableId
    val createUninitializedInstance = "createUninitializedInstance".internalCallableId
    val createUninitializedArray = "createUninitializedArray".internalCallableId
    val createEmptyString = "createEmptyString".internalCallableId
    val initInstance = "initInstance".internalCallableId
    val isSubtype = "isSubtype".internalCallableId
    val getContinuation = "getContinuation".internalCallableId
    val returnIfSuspended = "returnIfSuspended".internalCallableId
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

    val cstrProperty = InteropFqNames.cstrPropertyName.interopCallableId
    val wcstrProperty = InteropFqNames.wcstrPropertyName.interopCallableId
    val interopNativePointedRawPtrProperty = CallableId(ClassIds.nativePointed, Name.identifier(InteropFqNames.nativePointedRawPtrPropertyName))
    val cPointerRawValueProperty = CallableId(ClassIds.interopCPointer, Name.identifier(InteropFqNames.cPointerRawValuePropertyName))

    // Reflection functions
    private val String.reflectionCallableId get() = CallableId(StandardNames.KOTLIN_REFLECT_FQ_NAME, Name.identifier(this))
    val typeOfReflection = "typeOf".reflectionCallableId

    // Built-ins functions
    private val String.builtInsCallableId get() = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier(this))
    val isAssertionThrowingErrorEnabled = "isAssertionThrowingErrorEnabled".builtInsCallableId
    val getOrThrow = "getOrThrow".builtInsCallableId

    // Special stdlib public functions
    val enumEntries = CallableId(FqName("kotlin.enums"), Name.identifier("enumEntries"))
    val println = CallableId(FqName("kotlin.io"), Name.identifier("println"))
    val executeImpl = CallableId(KonanFqNames.packageName.child(Name.identifier("concurrent")), Name.identifier("executeImpl"))
    val createCleaner = CallableId(KonanFqNames.packageName.child(Name.identifier("ref")), Name.identifier("createCleaner"))
    val coroutineSuspended = CallableId(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, StandardNames.COROUTINE_SUSPENDED_NAME)
    val invokeSuspend = CallableId(ClassIds.baseContinuationImpl, Name.identifier("invokeSuspend"))
    val anyEquals = CallableId(StandardClassIds.Any, StandardNames.EQUALS_NAME)

    // collections functions
    private val String.collectionsId get() = CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier(this))
    val contentToString = "contentToString".collectionsId
    val contentHashCode = "contentHashCode".collectionsId
    val contentEquals = "contentEquals".collectionsId
    val copyInto = "copyInto".collectionsId
    val copyOf = "copyOf".collectionsId
}

private fun CompilerConfiguration.getMainCallableId() : CallableId? {
    if (get(KonanConfigKeys.PRODUCE) != CompilerOutputKind.PROGRAM) return null
    get(KonanConfigKeys.ENTRY)?.let {
        val entryPointFqName = FqName(it)
        return CallableId(entryPointFqName.parent(), entryPointFqName.shortName())
    }
    return when (get(KonanConfigKeys.GENERATE_TEST_RUNNER)) {
        TestRunnerKind.MAIN_THREAD -> CallableId(RuntimeNames.kotlinNativeInternalTestPackageName, StandardNames.MAIN)
        TestRunnerKind.WORKER -> CallableId(RuntimeNames.kotlinNativeInternalTestPackageName, Name.identifier("worker"))
        TestRunnerKind.MAIN_THREAD_NO_EXIT -> CallableId(RuntimeNames.kotlinNativeInternalTestPackageName, Name.identifier("mainNoExit"))
        TestRunnerKind.NONE, null -> CallableId(FqName.ROOT, StandardNames.MAIN)
    }
}

// TODO KT-77388 rename to `BackendNativeSymbolsImpl`
@OptIn(InternalSymbolFinderAPI::class, InternalKotlinNativeApi::class)
class KonanSymbols(
        context: ErrorReportingContext,
        irBuiltIns: IrBuiltIns,
        config: CompilerConfiguration,
) : FrontendNativeSymbols by FrontendNativeSymbols.FrontendNativeSymbolsImpl(irBuiltIns), KlibSymbols(irBuiltIns) {
    val entryPoint by run {
        val mainCallableId = config.getMainCallableId()
        val unfilteredCandidates = mainCallableId?.functionSymbols()
        lazy {
            unfilteredCandidates ?: return@lazy null
            fun IrType.isArrayMaybeOutString() : Boolean {
                if (this !is IrSimpleType) return false
                if (classOrNull != irBuiltIns.arrayClass) return false
                val argument = arguments.getOrNull(0) ?: return false
                if (argument !is IrTypeProjection) return false
                return argument.type.classOrNull == irBuiltIns.stringClass
            }
            fun IrSimpleFunction.isArrayStringMain() = hasShape(
                dispatchReceiver = false,
                extensionReceiver = false,
                contextParameters = 0,
                regularParameters = 1,
            ) && parameters[0].type.isArrayMaybeOutString()
            fun IrSimpleFunction.isNoArgsMain() = hasShape(
                dispatchReceiver = false,
                extensionReceiver = false,
                contextParameters = 0,
                regularParameters = 0,
            )

            val candidates = unfilteredCandidates.map { it.owner }
                .filter {
                    it.returnType.isUnit() && it.typeParameters.isEmpty() && it.visibility.isPublicAPI
                }

            val main = candidates.singleOrNull { it.isArrayStringMain() } ?: candidates.singleOrNull { it.isNoArgsMain() }
            if (main == null) context.reportCompilationError("Could not find '$mainCallableId' function.")
            if (main.isSuspend) context.reportCompilationError("Entry point can not be a suspend function.")
            main.symbol
        }
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

    val integerConversions by run {
        val signedIntegerClassIds = listOf(StandardClassIds.Byte, StandardClassIds.Short, StandardClassIds.Int, StandardClassIds.Long)
        val unsignedIntegerClassIds = listOf(StandardClassIds.UByte, StandardClassIds.UShort, StandardClassIds.UInt, StandardClassIds.ULong)
        val allIntegerClassIds = signedIntegerClassIds + unsignedIntegerClassIds
        val symbols = buildList {
            for (to in allIntegerClassIds) {
                val name = Name.identifier("to${to.shortClassName.asString().replaceFirstChar(Char::uppercaseChar)}")
                if (to in unsignedIntegerClassIds) {
                    add(CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, name))
                }
                for (from in allIntegerClassIds) {
                    if (from in unsignedIntegerClassIds || to in signedIntegerClassIds) {
                        add(CallableId(from, name))
                    }
                }
            }
        }.flatMap { it.functionSymbols() }
        lazy {
            symbols
                .groupBy { it.owner.parameters[0].type.classOrFail to it.owner.returnType.classOrFail }
                .mapValues { (k, v) ->
                    v.singleOrNull() ?: error("No single conversion found from ${k.first} to ${k.second}")
                }.also {
                    for (from in allIntegerClasses) {
                        for (to in allIntegerClasses) {
                            require((from to to) in it) { "No conversion not found from $from to $to" }
                        }
                    }
                }
        }
    }

    val symbolName = ClassIds.symbolName.classSymbol()
    val filterExceptions = ClassIds.filterExceptions.classSymbol()
    val exportForCppRuntime = ClassIds.exportForCppRuntime.classSymbol()
    val typedIntrinsic = ClassIds.typedIntrinsic.classSymbol()
    val cToKotlinBridge = ClassIds.cToKotlinBridge.classSymbol()
    val kotlinToCBridge = ClassIds.kotlinToCBridge.classSymbol()
    val interopCallMarker = symbolFinder.topLevelFunction(RuntimeNames.kotlinxCInteropInternalPackageName, "interopCallMarker")

    val objCMethodImp = ClassIds.objCMethodImp.classSymbol()

    val processUnhandledException = CallableIds.processUnhandledException.functionSymbol()
    val terminateWithUnhandledException = CallableIds.terminateWithUnhandledException.functionSymbol()

    val interopNativePointedGetRawPointer by CallableIds.nativePointedGetRawPointer.functionSymbol {
        it.extensionReceiverClass == nativePointed
    }

    val interopCPointer = ClassIds.interopCPointer.classSymbol()
    val interopCPointed = ClassIds.interopCPointed.classSymbol()
    val interopCVariable = ClassIds.interopCVariable.classSymbol()
    val interopCstr by CallableIds.cstrProperty.getterSymbol(extensionReceiverClass = string)
    val interopWcstr by CallableIds.wcstrProperty.getterSymbol(extensionReceiverClass = string)
    val interopMemScope = ClassIds.interopMemScope.classSymbol()
    val interopCValue = ClassIds.interopCValue.classSymbol()
    val interopCValues = ClassIds.interopCValues.classSymbol()
    val interopCValuesRef = ClassIds.interopCValuesRef.classSymbol()
    val interopCValueWrite by CallableIds.cValueWrite.functionSymbol {
        it.extensionReceiverClass == interopCValue
    }
    val interopCValueRead by CallableIds.cValueRead.functionSymbol {
        it.hasShape(
            extensionReceiver = true,
            regularParameters = 1
        )
    }
    val interopAllocType by CallableIds.allocType.functionSymbol {
        it.typeParameters.isEmpty()
    }

    val interopTypeOf = CallableIds.typeOf.functionSymbol()

    val interopCPointerGetRawValue by CallableIds.cPointerGetRawValue.functionSymbol {
        it.extensionReceiverClass == interopCPointer
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

    val interopNativePointedRawPtrGetter by CallableIds.interopNativePointedRawPtrProperty.getterSymbol()
    val interopCPointerRawValueGetter by CallableIds.cPointerRawValueProperty.getterSymbol()

    val interopInterpretObjCPointer = CallableIds.interpretObjCPointer.functionSymbol()
    val interopInterpretObjCPointerOrNull = CallableIds.interpretObjCPointerOrNull.functionSymbol()
    val interopInterpretNullablePointed = CallableIds.interpretNullablePointed.functionSymbol()
    val interopInterpretCPointer = CallableIds.interpretCPointer.functionSymbol()

    val createForeignException = CallableIds.createForeignException.functionSymbol()

    val interopCEnumVar = ClassIds.interopCEnumVar.classSymbol()

    val nativeMemUtils = ClassIds.nativeMemUtils.classSymbol()
    val nativeHeap = ClassIds.nativeHeap.classSymbol()

    val cStructVarConstructorSymbol by ClassIds.cStuctVar.primaryConstructorSymbol()
    val structVarTypePrimaryConstructor by ClassIds.cStructVarType.primaryConstructorSymbol()

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
        areEqualByValueFunctions.groupBy {
            it.owner.parameters[0].type.computePrimitiveBinaryTypeOrNull()!!
        }.mapValues {
            it.value.singleOrNull() ?: error("Several ${CallableIds.areEqualByValue} functions found for type ${it.key}")
        }
    }

    val reinterpret = CallableIds.reinterpret.functionSymbol()

    val theUnitInstance = CallableIds.theUnitInstance.functionSymbol()

    val ieee754Equals = CallableIds.ieee754Equals.functionSymbols()

    val equals = CallableIds.anyEquals.functionSymbol()

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

    override val stringBuilder = ClassIds.stringBuilder.classSymbol()

    private fun arrayToExtensionSymbolMap(callableId: CallableId, condition: (IrFunction) -> Boolean = { true }): Lazy<Map<IrClassSymbol, IrSimpleFunctionSymbol>> {
        val allSymbols = callableId.functionSymbols()
        return lazy {
            allSymbols
                .filter { !it.owner.isExpect && condition(it.owner) }
                .groupBy { it.owner.extensionReceiverClass }
                .filterKeys { it in arrays }
                .mapKeys { it.key!! }
                .mapValues { it.value.singleOrNull() ?: error("Several functions $callableId found for extension receiver ${it.key}") }
        }
    }

    val arrayContentToString by arrayToExtensionSymbolMap(CallableIds.contentToString) {
        it.extensionReceiverType?.isMarkedNullable() == true
    }
    val arrayContentHashCode by arrayToExtensionSymbolMap(CallableIds.contentHashCode) {
        it.extensionReceiverType?.isMarkedNullable() == true
    }
    val arrayContentEquals by arrayToExtensionSymbolMap(CallableIds.contentEquals) {
        it.extensionReceiverType?.isMarkedNullable() == true
    }

    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol> by lazy { arrayContentEquals.mapKeys { it.key.defaultType } }

    val copyInto by arrayToExtensionSymbolMap(CallableIds.copyInto)
    val copyOf by arrayToExtensionSymbolMap(CallableIds.copyOf) { it.hasShape(extensionReceiver = true) }

    private fun arrayFunctionsMap(name: Name) = lazy {
        arrays.associateWith { clazz -> clazz.owner.simpleFunctions().single { it.name == name }.symbol }
    }
    private fun arrayPropertyGettersMap(name: Name) = lazy {
        arrays.associateWith { clazz -> clazz.owner.properties.single { it.name == name }.getter!!.symbol }
    }

    val arrayGet by arrayFunctionsMap(OperatorNameConventions.GET)
    val arraySet by arrayFunctionsMap(OperatorNameConventions.SET)
    val arraySize by arrayPropertyGettersMap(Name.identifier("size"))

    val valuesForEnum = CallableIds.valuesForEnum.functionSymbol()

    val valueOfForEnum = CallableIds.valueOfForEnum.functionSymbol()

    val createEnumEntries by CallableIds.enumEntries.functionSymbol {
        it.hasShape(regularParameters = 1) && it.parameters[0].type.classOrNull == array
    }

    val enumEntriesInterface = ClassIds.enumEntries.classSymbol()

    val createUninitializedInstance = CallableIds.createUninitializedInstance.functionSymbol()

    val createUninitializedArray = CallableIds.createUninitializedArray.functionSymbol()

    val createEmptyString = CallableIds.createEmptyString.functionSymbol()

    val initInstance = CallableIds.initInstance.functionSymbol()

    val isSubtype = CallableIds.isSubtype.functionSymbol()

    val println by CallableIds.println.functionSymbol {
        it.hasShape(regularParameters = 1, parameterTypes = listOf(irBuiltIns.stringType))
    }

    override val getContinuation = CallableIds.getContinuation.functionSymbol()

    override val continuationClass = ClassIds.continuation.classSymbol()

    override val returnIfSuspended = CallableIds.returnIfSuspended.functionSymbol()

    override val coroutineImpl get() = TODO()

    val baseContinuationImpl = ClassIds.baseContinuationImpl.classSymbol()

    val restrictedContinuationImpl = ClassIds.restrictedContinuationImpl.classSymbol()

    val continuationImpl = ClassIds.continuationImpl.classSymbol()

    val invokeSuspendFunction = CallableIds.invokeSuspend.functionSymbol()

    override val coroutineSuspendedGetter by CallableIds.coroutineSuspended.getterSymbol()

    val saveCoroutineState = CallableIds.saveCoroutineState.functionSymbol()
    val restoreCoroutineState = CallableIds.restoreCoroutineState.functionSymbol()

    val cancellationException = ClassIds.cancellationException.classSymbol()

    val kotlinResult = ClassIds.kotlinResult.classSymbol()

    val kotlinResultGetOrThrow by CallableIds.getOrThrow.functionSymbol {
        it.extensionReceiverClass == kotlinResult
    }

    override val functionAdapter = ClassIds.functionAdapter.classSymbol()

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
    val kClassImplConstructor by ClassIds.kClassImpl.primaryConstructorSymbol()
    val kClassImplIntrinsicConstructor by ClassIds.kClassImpl.noParametersConstructorSymbol()
    val kObjectiveCKClassImplIntrinsicConstructor by ClassIds.objectiveCKClassImpl.noParametersConstructorSymbol()
    val kClassUnsupportedImpl = ClassIds.kClassUnsupportedImpl.classSymbol()
    val kTypeParameterImpl = ClassIds.kTypeParameterImpl.classSymbol()
    val kTypeImpl = ClassIds.kTypeImpl.classSymbol()
    val kTypeImplForTypeParametersWithRecursiveBounds = ClassIds.kTypeImplForTypeParametersWithRecursiveBounds.classSymbol()
    val kTypeProjectionList = ClassIds.kTypeProjectionList.classSymbol()
    val typeOf = CallableIds.typeOfReflection.functionSymbol()

    val threadLocal = ClassIds.threadLocal.classSymbol()

    val eagerInitialization = ClassIds.eagerInitialization.classSymbol()

    val noInline = ClassIds.noInline.classSymbol()

    val enumVarConstructorSymbol by ClassIds.interopCEnumVar.primaryConstructorSymbol()
    val primitiveVarTypePrimaryConstructor by ClassIds.interopCPrimitiveVarType.primaryConstructorSymbol()

    val isAssertionThrowingErrorEnabled = CallableIds.isAssertionThrowingErrorEnabled.functionSymbol()

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
