/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.PreSerializationWasmSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.backend.js.BackendWebSymbols
import org.jetbrains.kotlin.ir.backend.js.ReflectionSymbols
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget
import org.jetbrains.kotlin.wasm.config.wasmUseStackSwitchingProposal

class BackendWasmSymbols(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
) : PreSerializationWasmSymbols by PreSerializationWasmSymbols.Impl(irBuiltIns), BackendWebSymbols(irBuiltIns) {

    override val getWithoutBoundCheckName: Name = Name.identifier("getWithoutBoundCheck")
    override val setWithoutBoundCheckName: Name = Name.identifier("setWithoutBoundCheck")

    internal inner class WasmReflectionSymbols : ReflectionSymbols {
        override val createKType: IrSimpleFunctionSymbol by CallableIds.createKType.functionSymbol()
        override val getKClass: IrSimpleFunctionSymbol by CallableIds.getKClass.functionSymbol()
        override val getKClassFromExpression: IrSimpleFunctionSymbol by CallableIds.getKClassFromExpression.functionSymbol()
        override val createDynamicKType: IrSimpleFunctionSymbol get() = error("Dynamic type is not supported by Wasm")
        override val createKTypeParameter: IrSimpleFunctionSymbol by CallableIds.createKTypeParameter.functionSymbol()
        override val getStarKTypeProjection by CallableIds.getStarKTypeProjection.functionSymbol()
        override val createCovariantKTypeProjection by CallableIds.createCovariantKTypeProjection.functionSymbol()
        override val createInvariantKTypeProjection by CallableIds.createInvariantKTypeProjection.functionSymbol()
        override val createContravariantKTypeProjection by CallableIds.createContravariantKTypeProjection.functionSymbol()
        override val kTypeClass: IrClassSymbol = ClassIds.kTypeClass.classSymbol()

        val wasmTypeInfoData: IrClassSymbol = ClassIds.TypeInfoData.classSymbol()
        val kClassImpl: IrClassSymbol = ClassIds.KClassImpl.classSymbol()
        val kClassInterfaceImpl: IrClassSymbol = ClassIds.KClassInterfaceImpl.classSymbol()
        val isSupportedInterface by CallableIds.isSupportedInterface.functionSymbol()
        val getInterfaceVTable by CallableIds.getInterfaceVTable.functionSymbol()
        val wasmGetInterfaceVTableBodyImpl by CallableIds.wasmGetInterfaceVTableBodyImpl.functionSymbol()
        // XXX To be changed after bootstrap.
        val kFunctionImpl: IrClassSymbol = ClassIds.KFunctionImplNew.classSymbolOrNull() ?: ClassIds.KFunctionImpl.classSymbol()
        val kFunctionErrorImpl: IrClassSymbol = ClassIds.KFunctionErrorImpl.classSymbol()
    }

    internal val reflectionSymbols: WasmReflectionSymbols = WasmReflectionSymbols()

    internal val eagerInitialization: IrClassSymbol = ClassIds.EagerInitialization.classSymbol()

    internal val isNotFirstWasmExportCall: IrPropertySymbol by CallableIds.isNotFirstWasmExportCall.propertySymbol()

    val tryGetAssociatedObject by CallableIds.tryGetAssociatedObject.functionSymbol()

    internal val callAssociatedObjectGetter by CallableIds.callAssociatedObjectGetter.functionSymbol()
    val registerModuleDescriptor by CallableIds.registerModuleDescriptor.functionSymbol()

    internal val wasmLongImmutableArray = ClassIds.WasmLongImmutableArray.classSymbol()

    val asserts: Iterable<IrSimpleFunctionSymbol> by CallableIds.assert.functionSymbols()
    override val throwNullPointerException by CallableIds.THROW_NPE.functionSymbol()
    override val throwISE by CallableIds.THROW_ISE.functionSymbol()
    override val throwTypeCastException by CallableIds.THROW_CCE.functionSymbol()
    val throwTypeCastWithInfoException by CallableIds.THROW_CCE_WITH_INFO.functionSymbol()
    override val throwIAE by CallableIds.THROW_IAE.functionSymbol()
    val throwNoBranchMatchedException by CallableIds.throwNoBranchMatchedException.functionSymbol()
    override val throwKotlinNothingValueException by CallableIds.throwKotlinNothingValueException.functionSymbol()
    override val stringBuilder = ClassIds.StringBuilder.classSymbol()
    override val getContinuation by CallableIds.getContinuation.functionSymbol()
    override val returnIfSuspended by CallableIds.returnIfSuspended.functionSymbol()

    val throwLinkageError by CallableIds.throwIrLinkageError.functionSymbol()

    val appendable = ClassIds.Appendable.classSymbol()

    val enumValueOfIntrinsic by CallableIds.enumValueOfIntrinsic.functionSymbol()
    val enumValuesIntrinsic by CallableIds.enumValuesIntrinsic.functionSymbol()
    val enumEntriesIntrinsic by CallableIds.enumEntriesIntrinsic.functionSymbol()

    val coroutineEmptyContinuation: IrPropertySymbol by CallableIds.EmptyContinuation.propertySymbol()

    override val functionAdapter = ClassIds.FunctionAdapter.classSymbol()

    override val defaultConstructorMarker: IrClassSymbol = ClassIds.DefaultConstructorMarker.classSymbol()

    val wasmUnreachable by CallableIds.wasm_unreachable.functionSymbol()

    val voidType by ClassIds.Void.defaultType()

    private val consumeAnyIntoVoid by CallableIds.consumeAnyIntoVoid.functionSymbol()

    private val consumePrimitiveIntoVoid: Map<IrType, IrSimpleFunctionSymbol> by run {
        val consumeBooleanIntoVoid by CallableIds.consumeBooleanIntoVoid.functionSymbol()
        val consumeByteIntoVoid by CallableIds.consumeByteIntoVoid.functionSymbol()
        val consumeShortIntoVoid by CallableIds.consumeShortIntoVoid.functionSymbol()
        val consumeCharIntoVoid by CallableIds.consumeCharIntoVoid.functionSymbol()
        val consumeIntIntoVoid by CallableIds.consumeIntIntoVoid.functionSymbol()
        val consumeLongIntoVoid by CallableIds.consumeLongIntoVoid.functionSymbol()
        val consumeFloatIntoVoid by CallableIds.consumeFloatIntoVoid.functionSymbol()
        val consumeDoubleIntoVoid by CallableIds.consumeDoubleIntoVoid.functionSymbol()
        lazy {
            mapOf(
                irBuiltIns.booleanType to consumeBooleanIntoVoid,
                irBuiltIns.byteType to consumeByteIntoVoid,
                irBuiltIns.shortType to consumeShortIntoVoid,
                irBuiltIns.charType to consumeCharIntoVoid,
                irBuiltIns.intType to consumeIntIntoVoid,
                irBuiltIns.longType to consumeLongIntoVoid,
                irBuiltIns.floatType to consumeFloatIntoVoid,
                irBuiltIns.doubleType to consumeDoubleIntoVoid,
            )
        }
    }

    fun findVoidConsumer(type: IrType): IrSimpleFunctionSymbol =
        consumePrimitiveIntoVoid[type] ?: consumeAnyIntoVoid

    val equalityFunctions: Map<IrType, IrSimpleFunctionSymbol> by run {
        val wasm_i32_eq by CallableIds.wasm_i32_eq.functionSymbol()
        val wasm_i64_eq by CallableIds.wasm_i64_eq.functionSymbol()
        lazy {
            mapOf(
                irBuiltIns.booleanType to wasm_i32_eq,
                irBuiltIns.byteType to wasm_i32_eq,
                irBuiltIns.shortType to wasm_i32_eq,
                irBuiltIns.ubyteType to wasm_i32_eq,
                irBuiltIns.ushortType to wasm_i32_eq,
                irBuiltIns.charType to wasm_i32_eq,
                irBuiltIns.intType to wasm_i32_eq,
                irBuiltIns.uintType to wasm_i32_eq,
                irBuiltIns.longType to wasm_i64_eq,
                irBuiltIns.ulongType to wasm_i64_eq,
            )
        }
    }

    val floatEqualityFunctions: Map<IrType, IrSimpleFunctionSymbol> by run {
        val wasm_f32_eq by CallableIds.wasm_f32_eq.functionSymbol()
        val wasm_f64_eq by CallableIds.wasm_f64_eq.functionSymbol()
        lazy {
            mapOf(
                irBuiltIns.floatType to wasm_f32_eq,
                irBuiltIns.doubleType to wasm_f64_eq,
            )
        }
    }

    val comparisonBuiltInsToWasmIntrinsics: Map<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol> by irBuiltIns.run {
        fun wasmPrimitiveTypeName(classifier: IrClassifierSymbol): String = when (classifier) {
            booleanClass, byteClass, shortClass, charClass, intClass -> "i32"
            floatClass -> "f32"
            doubleClass -> "f64"
            longClass -> "i64"
            else -> error("Unknown primitive type")
        }

        val lazyList = listOf(
            lessFunByOperandType to "lt",
            lessOrEqualFunByOperandType to "le",
            greaterOrEqualFunByOperandType to "ge",
            greaterFunByOperandType to "gt"
        ).flatMap { [typeToBuiltIn, wasmOp] ->
            typeToBuiltIn.map { [type, builtin] ->
                val wasmType = wasmPrimitiveTypeName(type)
                val markSign = if (wasmType == "i32" || wasmType == "i64") "_s" else ""
                builtin to "wasm_${wasmType}_$wasmOp$markSign".wasmCallableId.functionSymbol()
            }
        }

        lazy { lazyList.associate { it.first to it.second.value } }
    }

    val booleanAnd by CallableIds.wasm_i32_and.functionSymbol()
    val refEq by CallableIds.wasm_ref_eq.functionSymbol()
    val refIsNull by CallableIds.wasm_ref_is_null.functionSymbol()
    val refTest by CallableIds.wasm_ref_test.functionSymbol()
    val refCastNull by CallableIds.wasm_ref_cast_null.functionSymbol()
    val callRef by CallableIds.wasm_call_ref.functionSymbol()
    val wasmArrayCopy by CallableIds.wasm_array_copy.functionSymbol()
    val wasmArrayNewData0 by CallableIds.array_new_data0.functionSymbol()
    val wasmArrayNewData by CallableIds.array_new_data.functionSymbol()
    val wasmArrayNewData0CharArray by CallableIds.array_new_data0_char_array.functionSymbolOrNull()

    val intToLong by CallableIds.wasm_i64_extend_i32_s.functionSymbol()

    val rangeCheck by CallableIds.rangeCheck.functionSymbol()

    val getBoxedBoolean: IrSimpleFunctionSymbol by CallableIds.getBoxedBoolean.functionSymbol()
    val boxBoolean: IrSimpleFunctionSymbol by CallableIds.boxBoolean.functionSymbol()
    val boxIntrinsic: IrSimpleFunctionSymbol by CallableIds.boxIntrinsic.functionSymbol()
    val unboxIntrinsic: IrSimpleFunctionSymbol by CallableIds.unboxIntrinsic.functionSymbol()

    val createString by CallableIds.createString.functionSymbol()

    val getWasmAbiVersion by CallableIds.getWasmAbiVersion.functionSymbol()

    val registerRootSuiteBlock by CallableIds.registerRootSuiteBlock.functionSymbolOrNull()
    val runRootSuites by CallableIds.runRootSuites.functionSymbolOrNull()

    val wasmTypeId by CallableIds.wasmTypeId.functionSymbol()
    val wasmGetTypeRtti by CallableIds.wasmGetTypeRtti.functionSymbol()
    val likely by CallableIds.likely.functionSymbol()
    val unlikely by CallableIds.unlikely.functionSymbol()
    val wasmGetRttiSupportedInterfaces by CallableIds.wasmGetRttiSupportedInterfaces.functionSymbol()
    val wasmGetRttiIntField by CallableIds.wasmGetRttiIntField.functionSymbol()
    val wasmGetRttiLongField by CallableIds.wasmGetRttiLongField.functionSymbol()
    val wasmGetRttiSuperClass by CallableIds.wasmGetRttiSuperClass.functionSymbol()
    val wasmGetObjectRtti by CallableIds.wasmGetObjectRtti.functionSymbol()
    val wasmGetQualifierImpl by CallableIds.wasmGetQualifierImpl.functionSymbol()
    val wasmGetSimpleNameImpl by CallableIds.wasmGetSimpleNameImpl.functionSymbol()
    val wasmArrayAnyIndexOfValue by CallableIds.wasmArrayAnyIndexOfValue.functionSymbol()

    val wasmIsInterface by CallableIds.wasmIsInterface.functionSymbol()

    val nullableEquals by CallableIds.nullableEquals.functionSymbol()
    val anyNtoString by CallableIds.anyNtoString.functionSymbol()

    val nullableFloatIeee754Equals by CallableIds.nullableFloatIeee754Equals.functionSymbol()
    val nullableDoubleIeee754Equals by CallableIds.nullableDoubleIeee754Equals.functionSymbol()

    val returnArgumentIfItIsKotlinAny by CallableIds.returnArgumentIfItIsKotlinAny.functionSymbol()

    val startCoroutineUninterceptedOrReturnIntrinsics: List<IrSimpleFunctionSymbol> by run {
        val startCoroutineUninterceptedOrReturnIntrinsic0 by CallableIds.startCoroutineUninterceptedOrReturnIntrinsic0.functionSymbol()
        val startCoroutineUninterceptedOrReturnIntrinsic1 by CallableIds.startCoroutineUninterceptedOrReturnIntrinsic1.functionSymbol()
        val startCoroutineUninterceptedOrReturnIntrinsic2 by CallableIds.startCoroutineUninterceptedOrReturnIntrinsic2.functionSymbol()
        lazyOf(
            listOf(
                startCoroutineUninterceptedOrReturnIntrinsic0,
                startCoroutineUninterceptedOrReturnIntrinsic1,
                startCoroutineUninterceptedOrReturnIntrinsic2,
            )
        )
    }
    val coroutinesStackSwitchingIntrinsics =
        if (configuration.wasmUseStackSwitchingProposal)
            CoroutinesStackSwitchingIntrinsics()
        else null
    val coroutinesStateMachineIntrinsics =
        if (!configuration.wasmUseStackSwitchingProposal)
            CoroutinesStateMachineIntrinsics()
        else null

    override val coroutineImpl: IrClassSymbol =
        (coroutinesStackSwitchingIntrinsics?.coroutineImpl ?: coroutinesStateMachineIntrinsics!!.coroutineImpl).classSymbol()

    private val String.coroutinesClassId get() = ClassId(StandardNames.COROUTINES_PACKAGE_FQ_NAME, Name.identifier(this))

    inner class CoroutinesStackSwitchingIntrinsics {
        val suspendFunctionToContref: List<IrSimpleFunctionSymbol> by run {
            val symbols = CallableIds.suspendFunctionToContref.map { it.functionSymbol() }

            lazyOf(symbols.map { it.value })
        }

        val suspendFunctionToContrefImpl: List<IrSimpleFunctionSymbol> by run {
            val symbols = CallableIds.suspendFunctionToContrefImpl.map { it.functionSymbol() }

            lazyOf(symbols.map { it.value })
        }
        val nullContrefIntrinsic by CallableIds.nullContrefIntrinsic.functionSymbol()
        val suspendIntrinsic by CallableIds.suspendIntrinsic.functionSymbol()
        val resumeThrowIntrinsic by CallableIds.resumeThrowIntrinsic.functionSymbol()
        val resumeWithIntrinsic by CallableIds.resumeWithIntrinsic.functionSymbol()
        val resumeWithImpl by CallableIds.resumeWithImpl.functionSymbol()
        val coroutineImpl = "CoroutineImplStackSwitching".coroutinesClassId

        val createCoroutineUninterceptedIntrinsicsStackSwitching: List<IrSimpleFunctionSymbol> by run {
            val createCoroutineUninterceptedIntrinsic0StackSwitching by CallableIds.createCoroutineUninterceptedIntrinsic0StackSwitching.functionSymbol()
            val createCoroutineUninterceptedIntrinsic1StackSwitching by CallableIds.createCoroutineUninterceptedIntrinsic1StackSwitching.functionSymbol()
            lazyOf(
                listOf(
                    createCoroutineUninterceptedIntrinsic0StackSwitching,
                    createCoroutineUninterceptedIntrinsic1StackSwitching,
                )
            )
        }
    }

    inner class CoroutinesStateMachineIntrinsics {
        val coroutineImpl = "CoroutineImplStateMachine".coroutinesClassId

        val createSimpleCoroutineFromSuspendFunction by
        CallableIds.createSimpleCoroutineFromSuspendFunction.functionSymbol()
    }

    val createCoroutineUninterceptedIntrinsics: List<IrSimpleFunctionSymbol> by run {
        val createCoroutineUninterceptedIntrinsic0 by CallableIds.createCoroutineUninterceptedIntrinsic0.functionSymbol()
        val createCoroutineUninterceptedIntrinsic1 by CallableIds.createCoroutineUninterceptedIntrinsic1.functionSymbol()
        lazyOf(
            listOf(
                createCoroutineUninterceptedIntrinsic0,
                createCoroutineUninterceptedIntrinsic1,
            )
        )
    }

    override val suspendCoroutineUninterceptedOrReturn by
    if (configuration.wasmUseStackSwitchingProposal)
        CallableIds.suspendCoroutineUninterceptedOrReturnStackSwitching.functionSymbol()
    else
        CallableIds.suspendCoroutineUninterceptedOrReturn.functionSymbol()

    // KProperty implementations
    val kLocalDelegatedPropertyImpl: IrClassSymbol = ClassIds.KLocalDelegatedPropertyImpl.classSymbol()
    val kLocalDelegatedMutablePropertyImpl: IrClassSymbol = ClassIds.KLocalDelegatedMutablePropertyImpl.classSymbol()
    val kProperty0Impl: IrClassSymbol = ClassIds.KProperty0Impl.classSymbol()
    val kProperty1Impl: IrClassSymbol = ClassIds.KProperty1Impl.classSymbol()
    val kProperty2Impl: IrClassSymbol = ClassIds.KProperty2Impl.classSymbol()
    val kMutableProperty0Impl: IrClassSymbol = ClassIds.KMutableProperty0Impl.classSymbol()
    val kMutableProperty1Impl: IrClassSymbol = ClassIds.KMutableProperty1Impl.classSymbol()
    val kMutableProperty2Impl: IrClassSymbol = ClassIds.KMutableProperty2Impl.classSymbol()

    val arraysCopyInto by CallableIds.copyInto.functionSymbols()

    private val contentToString: List<IrSimpleFunctionSymbol> by CallableIds.contentToString.functionSymbols()

    private val contentHashCode: List<IrSimpleFunctionSymbol> by CallableIds.contentHashCode.functionSymbols()

    private fun findNullableOverloadForReceiver(arrayType: IrType, overloadsList: List<IrSimpleFunctionSymbol>): IrSimpleFunctionSymbol =
        overloadsList.first {
            val receiverType = it.owner.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
            receiverType != null && receiverType.isNullable() && arrayType.classOrNull == receiverType.classOrNull
        }

    fun findContentToStringOverload(arrayType: IrType): IrSimpleFunctionSymbol = findNullableOverloadForReceiver(arrayType, contentToString)

    fun findContentHashCodeOverload(arrayType: IrType): IrSimpleFunctionSymbol = findNullableOverloadForReceiver(arrayType, contentHashCode)

    val wasmStructRefType by ClassIds.structref.defaultType()
    val wasmFuncRefType by ClassIds.funcref.defaultType()
    val wasmTypedFuncRefClass = ClassIds.typedfuncref.classSymbol()

    fun wasmTypedFuncRefType(functionType: IrType): IrSimpleType =
        wasmTypedFuncRefClass.typeWith(functionType)

    val wasmAnyRefClass = ClassIds.anyref.classSymbol()

    val checkStaticInitializationState by CallableIds.checkStaticInitializationState.functionSymbol()

    inner class JsInteropAdapters {
        val kotlinToJsStringAdapter by CallableIds.kotlinToJsStringAdapter.functionSymbol()
        val kotlinToJsAnyAdapter by CallableIds.kotlinToJsAnyAdapter.functionSymbol()
        val numberToDoubleAdapter by CallableIds.numberToDoubleAdapter.functionSymbol()

        val jsCheckIsNullOrUndefinedAdapter by CallableIds.jsCheckIsNullOrUndefinedAdapter.functionSymbol()

        val jsToKotlinStringAdapter by CallableIds.jsToKotlinStringAdapter.functionSymbol()
        val jsToKotlinAnyAdapter by CallableIds.jsToKotlinAnyAdapter.functionSymbol()

        val jsToKotlinByteAdapter by CallableIds.jsToKotlinByteAdapter.functionSymbol()
        val jsToKotlinShortAdapter by CallableIds.jsToKotlinShortAdapter.functionSymbol()
        val jsToKotlinCharAdapter by CallableIds.jsToKotlinCharAdapter.functionSymbol()

        val externRefToKotlinIntAdapter by CallableIds.externRefToKotlinIntAdapter.functionSymbol()
        val externRefToKotlinBooleanAdapter by CallableIds.externRefToKotlinBooleanAdapter.functionSymbol()
        val externRefToKotlinLongAdapter by CallableIds.externRefToKotlinLongAdapter.functionSymbol()
        val externRefToKotlinFloatAdapter by CallableIds.externRefToKotlinFloatAdapter.functionSymbol()
        val externRefToKotlinDoubleAdapter by CallableIds.externRefToKotlinDoubleAdapter.functionSymbol()

        val externRefToKotlinUByteAdapter by CallableIds.externRefToKotlinUByteAdapter.functionSymbol()
        val externRefToKotlinUShortAdapter by CallableIds.externRefToKotlinUShortAdapter.functionSymbol()
        val externRefToKotlinUIntAdapter by CallableIds.externRefToKotlinUIntAdapter.functionSymbol()
        val externRefToKotlinULongAdapter by CallableIds.externRefToKotlinULongAdapter.functionSymbol()

        val kotlinIntToExternRefAdapter by CallableIds.kotlinIntToExternRefAdapter.functionSymbol()
        val kotlinBooleanToExternRefAdapter by CallableIds.kotlinBooleanToExternRefAdapter.functionSymbol()
        val kotlinLongToExternRefAdapter by CallableIds.kotlinLongToExternRefAdapter.functionSymbol()
        val kotlinFloatToExternRefAdapter by CallableIds.kotlinFloatToExternRefAdapter.functionSymbol()
        val kotlinDoubleToExternRefAdapter by CallableIds.kotlinDoubleToExternRefAdapter.functionSymbol()
        val kotlinByteToExternRefAdapter by CallableIds.kotlinByteToExternRefAdapter.functionSymbol()
        val kotlinShortToExternRefAdapter by CallableIds.kotlinShortToExternRefAdapter.functionSymbol()
        val kotlinCharToExternRefAdapter by CallableIds.kotlinCharToExternRefAdapter.functionSymbol()

        val kotlinUByteToJsNumber by CallableIds.kotlinUByteToJsNumber.functionSymbol()
        val kotlinUShortToJsNumber by CallableIds.kotlinUShortToJsNumber.functionSymbol()
        val kotlinUIntToJsNumber by CallableIds.kotlinUIntToJsNumber.functionSymbol()
        val kotlinULongToJsBigInt by CallableIds.kotlinULongToJsBigInt.functionSymbol()

        val getCachedJsObject by CallableIds.getCachedJsObject.functionSymbol()
    }

    inner class JsRelatedSymbols {
        val jsInteropAdapters = JsInteropAdapters()

        val jsExport = ClassIds.JsExport.classSymbol()
        val jsExportConstructor by ClassIds.JsExport.primaryConstructorSymbol()
        val jsExportDefault = ClassIds.JsExportDefault.classSymbol()
        val jsExportDefaultConstructor by ClassIds.JsExportDefault.primaryConstructorSymbol()
        val jsNameConstructor by ClassIds.JsName.primaryConstructorSymbol()
        val jsFunConstructor by ClassIds.JsFun.primaryConstructorSymbol()

        val jsCode by CallableIds.js.functionSymbol()

        val jsAnyClass = ClassIds.JsAny.classSymbol()
        val jsReferenceClass = ClassIds.JsReference.classSymbol()

        val jsAnyType: IrType by ClassIds.JsAny.defaultType()
        val jsBooleanType: IrType by ClassIds.JsBoolean.defaultType()
        val jsStringType: IrType by ClassIds.JsString.defaultType()
        val jsNumberType: IrType by ClassIds.JsNumber.defaultType()
        val jsBigIntType: IrType by ClassIds.JsBigInt.defaultType()

        val newJsArray by CallableIds.newJsArray.functionSymbol()

        val jsArrayPush by CallableIds.jsArrayPush.functionSymbol()

        val externRefIsNull by CallableIds.wasm_externref_is_null.functionSymbol()

        val jsPromise = ClassIds.Promise.classSymbol()

        val kExternalClassImpl: IrClassSymbol = ClassIds.KExternalClassImpl.classSymbol()

        val jsException = ClassIds.JsException.classSymbol()

        val getKotlinException by CallableIds.getKotlinException.functionSymbol()

        val throwValue by CallableIds.throwValue.functionSymbol()

        val throw0 by CallableIds.throw0.functionSymbol()

        val jsConcat by CallableIds.jsConcat.functionSymbol()

        val getJsError by CallableIds.getJsError.functionSymbol()
    }

    val wasmExportConstructor by ClassIds.WasmExport.primaryConstructorSymbol()

    private val jsRelatedSymbolsIfNonWasi = if (configuration.wasmTarget == WasmTarget.JS) JsRelatedSymbols() else null

    val jsRelatedSymbols get() = jsRelatedSymbolsIfNonWasi ?: error("Cannot access to js related std in wasi mode")

    private val invokeOnExportedFunctionExitIfWasi: IrSimpleFunctionSymbol? by run {
        when (configuration.wasmTarget == WasmTarget.WASI) {
            true -> CallableIds.invokeOnExportedFunctionExit.functionSymbol()
            else -> lazyOf(null)
        }
    }

    val invokeOnExportedFunctionExit get() = invokeOnExportedFunctionExitIfWasi ?: error("Cannot access to wasi related std in js mode")

    val wasmMemoryInternalIfJsOrNull: IrSimpleFunctionSymbol? by run {
        when (configuration.wasmTarget == WasmTarget.JS) {
            true -> CallableIds.wasmMemoryInternal.functionSymbol()
            else -> lazyOf(null)
        }
    }
}

private object ClassIds {
    // WASM internal class
    private val String.wasmClassId get() = ClassId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE, Name.identifier(this))
    val TypeInfoData = "TypeInfoData".wasmClassId
    val KClassImpl = "KClassImpl".wasmClassId
    val KClassInterfaceImpl = "KClassInterfaceImpl".wasmClassId
    val KFunctionImpl = "KFunctionImpl".wasmClassId
    // XXX To be removed after bootstrap.
    val KFunctionImplNew = "KFunctionImplNew".wasmClassId
    val KFunctionErrorImpl = "KFunctionErrorImpl".wasmClassId
    val WasmLongImmutableArray = "WasmLongImmutableArray".wasmClassId
    val FunctionAdapter = "FunctionAdapter".wasmClassId
    val DefaultConstructorMarker = "DefaultConstructorMarker".wasmClassId
    val KLocalDelegatedPropertyImpl = "KLocalDelegatedPropertyImpl".wasmClassId
    val KLocalDelegatedMutablePropertyImpl = "KLocalDelegatedMutablePropertyImpl".wasmClassId
    val KProperty0Impl = "KProperty0Impl".wasmClassId
    val KProperty1Impl = "KProperty1Impl".wasmClassId
    val KProperty2Impl = "KProperty2Impl".wasmClassId
    val KMutableProperty0Impl = "KMutableProperty0Impl".wasmClassId
    val KMutableProperty1Impl = "KMutableProperty1Impl".wasmClassId
    val KMutableProperty2Impl = "KMutableProperty2Impl".wasmClassId
    val KExternalClassImpl = "KExternalClassImpl".wasmClassId
    val Void = "Void".wasmClassId

    // JS class
    private val String.jsClassId get() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
    val JsExport = "JsExport".jsClassId
    val JsName = "JsName".jsClassId
    val JsReference = "JsReference".jsClassId
    val JsAny = "JsAny".jsClassId
    val JsBoolean = "JsBoolean".jsClassId
    val JsString = "JsString".jsClassId
    val JsNumber = "JsNumber".jsClassId
    val JsBigInt = "JsBigInt".jsClassId
    val Promise = "Promise".jsClassId
    val JsException = "JsException".jsClassId

    val JsExportDefault = JsExport.createNestedClassId(Name.identifier("Default"))

    // Other
    val kTypeClass = ClassId(StandardNames.KOTLIN_REFLECT_FQ_NAME, Name.identifier("KClass"))
    val EagerInitialization = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("EagerInitialization"))
    val JsFun = ClassId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("JsFun"))
    val StringBuilder = ClassId(StandardClassIds.BASE_TEXT_PACKAGE, Name.identifier("StringBuilder"))
    val Appendable = ClassId(StandardClassIds.BASE_TEXT_PACKAGE, Name.identifier("Appendable"))
    val structref = ClassId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE.child(Name.identifier("reftypes")), Name.identifier("structref"))
    val funcref = ClassId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE.child(Name.identifier("reftypes")), Name.identifier("funcref"))
    val typedfuncref = ClassId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE.child(Name.identifier("reftypes")), Name.identifier("typedfuncref"))
    val anyref = ClassId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE.child(Name.identifier("reftypes")), Name.identifier("anyref"))
    val WasmExport = ClassId(WasmStandardClassIds.BASE_WASM_PACKAGE, Name.identifier("WasmExport"))
}

private val String.coroutinesIntrinsicsCallableId get() = CallableId(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, Name.identifier(this))

private val String.wasmCallableId get() = CallableId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE, Name.identifier(this))

private object CallableIds {
    // WASM internal functions
    val createKType = "createKType".wasmCallableId
    val getKClass = "getKClass".wasmCallableId
    val getKClassFromExpression = "getKClassFromExpression".wasmCallableId
    val createKTypeParameter = "createKTypeParameter".wasmCallableId
    val getStarKTypeProjection = "getStarKTypeProjection".wasmCallableId
    val createCovariantKTypeProjection = "createCovariantKTypeProjection".wasmCallableId
    val createInvariantKTypeProjection = "createInvariantKTypeProjection".wasmCallableId
    val createContravariantKTypeProjection = "createContravariantKTypeProjection".wasmCallableId
    val isSupportedInterface = "isSupportedInterface".wasmCallableId
    val getInterfaceVTable = "getInterfaceVTable".wasmCallableId
    val wasmGetInterfaceVTableBodyImpl = "wasmGetInterfaceVTableBodyImpl".wasmCallableId
    val tryGetAssociatedObject = "tryGetAssociatedObject".wasmCallableId
    val callAssociatedObjectGetter = "callAssociatedObjectGetter".wasmCallableId
    val registerModuleDescriptor = "registerModuleDescriptor".wasmCallableId
    val THROW_NPE = "THROW_NPE".wasmCallableId
    val THROW_ISE = "THROW_ISE".wasmCallableId
    val THROW_CCE = "THROW_CCE".wasmCallableId
    val THROW_CCE_WITH_INFO = "THROW_CCE_WITH_INFO".wasmCallableId
    val THROW_IAE = "THROW_IAE".wasmCallableId
    val throwNoBranchMatchedException = "throwNoBranchMatchedException".wasmCallableId
    val throwKotlinNothingValueException = "throwKotlinNothingValueException".wasmCallableId
    val getContinuation = "getContinuation".wasmCallableId
    val returnIfSuspended = "returnIfSuspended".wasmCallableId
    val enumValueOfIntrinsic = "enumValueOfIntrinsic".wasmCallableId
    val enumValuesIntrinsic = "enumValuesIntrinsic".wasmCallableId
    val wasm_unreachable = "wasm_unreachable".wasmCallableId
    val consumeAnyIntoVoid = "consumeAnyIntoVoid".wasmCallableId
    val consumeBooleanIntoVoid = "consumeBooleanIntoVoid".wasmCallableId
    val consumeByteIntoVoid = "consumeByteIntoVoid".wasmCallableId
    val consumeShortIntoVoid = "consumeShortIntoVoid".wasmCallableId
    val consumeCharIntoVoid = "consumeCharIntoVoid".wasmCallableId
    val consumeIntIntoVoid = "consumeIntIntoVoid".wasmCallableId
    val consumeLongIntoVoid = "consumeLongIntoVoid".wasmCallableId
    val consumeFloatIntoVoid = "consumeFloatIntoVoid".wasmCallableId
    val consumeDoubleIntoVoid = "consumeDoubleIntoVoid".wasmCallableId
    val wasm_i32_eq = "wasm_i32_eq".wasmCallableId
    val wasm_i64_eq = "wasm_i64_eq".wasmCallableId
    val wasm_f32_eq = "wasm_f32_eq".wasmCallableId
    val wasm_f64_eq = "wasm_f64_eq".wasmCallableId
    val wasm_i32_and = "wasm_i32_and".wasmCallableId
    val wasm_ref_eq = "wasm_ref_eq".wasmCallableId
    val wasm_ref_is_null = "wasm_ref_is_null".wasmCallableId
    val wasm_call_ref = "wasm_call_ref".wasmCallableId
    val wasm_ref_test = "wasm_ref_test".wasmCallableId
    val wasm_ref_cast_null = "wasm_ref_cast_null".wasmCallableId
    val wasm_array_copy = "wasm_array_copy".wasmCallableId
    val array_new_data0 = "array_new_data0".wasmCallableId
    val array_new_data = "array_new_data".wasmCallableId
    val array_new_data0_char_array = "array_new_data0_char_array".wasmCallableId
    val wasm_i64_extend_i32_s = "wasm_i64_extend_i32_s".wasmCallableId
    val rangeCheck = "rangeCheck".wasmCallableId
    val getBoxedBoolean = "getBoxedBoolean".wasmCallableId
    val boxBoolean = "boxBoolean".wasmCallableId
    val boxIntrinsic = "boxIntrinsic".wasmCallableId
    val unboxIntrinsic = "unboxIntrinsic".wasmCallableId
    val getWasmAbiVersion = "getWasmAbiVersion".wasmCallableId
    val wasmTypeId = "wasmTypeId".wasmCallableId
    val wasmGetTypeRtti = "wasmGetTypeRtti".wasmCallableId
    val likely = "likely".wasmCallableId
    val unlikely = "unlikely".wasmCallableId
    val wasmGetRttiSupportedInterfaces = "wasmGetRttiSupportedInterfaces".wasmCallableId
    val wasmGetRttiIntField = "wasmGetRttiIntField".wasmCallableId
    val wasmGetRttiLongField = "wasmGetRttiLongField".wasmCallableId
    val wasmGetRttiSuperClass = "wasmGetRttiSuperClass".wasmCallableId
    val wasmGetObjectRtti = "wasmGetObjectRtti".wasmCallableId
    val wasmGetQualifierImpl = "wasmGetQualifierImpl".wasmCallableId
    val wasmGetSimpleNameImpl = "wasmGetSimpleNameImpl".wasmCallableId
    val wasmArrayAnyIndexOfValue = "wasmArrayAnyIndexOfValue".wasmCallableId
    val wasmIsInterface = "wasmIsInterface".wasmCallableId
    val nullableEquals = "nullableEquals".wasmCallableId
    val anyNtoString = "anyNtoString".wasmCallableId
    val nullableFloatIeee754Equals = "nullableFloatIeee754Equals".wasmCallableId
    val nullableDoubleIeee754Equals = "nullableDoubleIeee754Equals".wasmCallableId
    val returnArgumentIfItIsKotlinAny = "returnArgumentIfItIsKotlinAny".wasmCallableId
    val wasmMemoryInternal = "wasmMemoryInternal".wasmCallableId

    val startCoroutineUninterceptedOrReturnIntrinsic0 = "startCoroutineUninterceptedOrReturnIntrinsic0".wasmCallableId
    val startCoroutineUninterceptedOrReturnIntrinsic1 = "startCoroutineUninterceptedOrReturnIntrinsic1".wasmCallableId
    val startCoroutineUninterceptedOrReturnIntrinsic2 = "startCoroutineUninterceptedOrReturnIntrinsic2".wasmCallableId

    val suspendCoroutineUninterceptedOrReturnStackSwitching = "suspendCoroutineUninterceptedOrReturnStackSwitching".wasmCallableId
    val suspendCoroutineUninterceptedOrReturn = "suspendCoroutineUninterceptedOrReturn".wasmCallableId

    val suspendFunctionToContref = (0..2).map { "suspendFunction${it}ToContref".wasmCallableId }
    val suspendFunctionToContrefImpl = (0..2).map { "suspendFunction${it}ToContrefImpl".wasmCallableId }
    val nullContrefIntrinsic = "nullContrefIntrinsic".wasmCallableId
    val suspendIntrinsic = "suspendIntrinsic".wasmCallableId
    val resumeThrowIntrinsic = "resumeThrowIntrinsic".wasmCallableId
    val resumeWithIntrinsic = "resumeWithIntrinsic".wasmCallableId
    val resumeWithImpl = "resumeWithImpl".wasmCallableId

    val kotlinToJsStringAdapter = "kotlinToJsStringAdapter".wasmCallableId
    val kotlinToJsAnyAdapter = "kotlinToJsAnyAdapter".wasmCallableId
    val numberToDoubleAdapter = "numberToDoubleAdapter".wasmCallableId
    val jsCheckIsNullOrUndefinedAdapter = "jsCheckIsNullOrUndefinedAdapter".wasmCallableId
    val jsToKotlinStringAdapter = "jsToKotlinStringAdapter".wasmCallableId
    val jsToKotlinAnyAdapter = "jsToKotlinAnyAdapter".wasmCallableId
    val jsToKotlinByteAdapter = "jsToKotlinByteAdapter".wasmCallableId
    val jsToKotlinShortAdapter = "jsToKotlinShortAdapter".wasmCallableId
    val jsToKotlinCharAdapter = "jsToKotlinCharAdapter".wasmCallableId
    val externRefToKotlinIntAdapter = "externRefToKotlinIntAdapter".wasmCallableId
    val externRefToKotlinBooleanAdapter = "externRefToKotlinBooleanAdapter".wasmCallableId
    val externRefToKotlinLongAdapter = "externRefToKotlinLongAdapter".wasmCallableId
    val externRefToKotlinFloatAdapter = "externRefToKotlinFloatAdapter".wasmCallableId
    val externRefToKotlinDoubleAdapter = "externRefToKotlinDoubleAdapter".wasmCallableId
    val externRefToKotlinUByteAdapter = "externRefToKotlinUByteAdapter".wasmCallableId
    val externRefToKotlinUShortAdapter = "externRefToKotlinUShortAdapter".wasmCallableId
    val externRefToKotlinUIntAdapter = "externRefToKotlinUIntAdapter".wasmCallableId
    val externRefToKotlinULongAdapter = "externRefToKotlinULongAdapter".wasmCallableId
    val kotlinIntToExternRefAdapter = "kotlinIntToExternRefAdapter".wasmCallableId
    val kotlinBooleanToExternRefAdapter = "kotlinBooleanToExternRefAdapter".wasmCallableId
    val kotlinLongToExternRefAdapter = "kotlinLongToExternRefAdapter".wasmCallableId
    val kotlinFloatToExternRefAdapter = "kotlinFloatToExternRefAdapter".wasmCallableId
    val kotlinDoubleToExternRefAdapter = "kotlinDoubleToExternRefAdapter".wasmCallableId
    val kotlinByteToExternRefAdapter = "kotlinByteToExternRefAdapter".wasmCallableId
    val kotlinShortToExternRefAdapter = "kotlinShortToExternRefAdapter".wasmCallableId
    val kotlinCharToExternRefAdapter = "kotlinCharToExternRefAdapter".wasmCallableId
    val kotlinUByteToJsNumber = "kotlinUByteToJsNumber".wasmCallableId
    val kotlinUShortToJsNumber = "kotlinUShortToJsNumber".wasmCallableId
    val kotlinUIntToJsNumber = "kotlinUIntToJsNumber".wasmCallableId
    val kotlinULongToJsBigInt = "kotlinULongToJsBigInt".wasmCallableId
    val newJsArray = "newJsArray".wasmCallableId
    val jsArrayPush = "jsArrayPush".wasmCallableId
    val wasm_externref_is_null = "wasm_externref_is_null".wasmCallableId
    val getKotlinException = "getKotlinException".wasmCallableId
    val throwValue = "throwValue".wasmCallableId
    val throw0 = "throw0".wasmCallableId
    val invokeOnExportedFunctionExit = "invokeOnExportedFunctionExit".wasmCallableId

    val isNotFirstWasmExportCall = "isNotFirstWasmExportCall".wasmCallableId
    val EmptyContinuation = "EmptyContinuation".wasmCallableId

    val getCachedJsObject = "getCachedJsObject".wasmCallableId

    val jsConcat = "jsConcat".wasmCallableId

    val getJsError = "getJsError".wasmCallableId

    // Coroutines intrinsics
    val createSimpleCoroutineFromSuspendFunction =
        "createSimpleCoroutineFromSuspendFunction".coroutinesIntrinsicsCallableId

    val createCoroutineUninterceptedIntrinsic0 = "createCoroutineUninterceptedIntrinsic0".coroutinesIntrinsicsCallableId
    val createCoroutineUninterceptedIntrinsic1 = "createCoroutineUninterceptedIntrinsic1".coroutinesIntrinsicsCallableId

    val createCoroutineUninterceptedIntrinsic0StackSwitching =
        "createCoroutineUninterceptedIntrinsic0StackSwitching".coroutinesIntrinsicsCallableId
    val createCoroutineUninterceptedIntrinsic1StackSwitching =
        "createCoroutineUninterceptedIntrinsic1StackSwitching".coroutinesIntrinsicsCallableId

    // Collection functions
    private val String.collectionCallableId get() = CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier(this))
    val copyInto = "copyInto".collectionCallableId
    val contentToString = "contentToString".collectionCallableId
    val contentHashCode = "contentHashCode".collectionCallableId

    // Other
    val assert = CallableId(StandardClassIds.BASE_KOTLIN_PACKAGE, Name.identifier("assert"))
    val enumEntriesIntrinsic = CallableId(StandardClassIds.BASE_ENUMS_PACKAGE, Name.identifier("enumEntriesIntrinsic"))
    val throwIrLinkageError = CallableId(StandardClassIds.BASE_INTERNAL_PACKAGE, Name.identifier("throwIrLinkageError"))
    val createString = CallableId(StandardNames.BUILT_INS_PACKAGE_FQ_NAME, Name.identifier("createString"))
    val js = CallableId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier("js"))
    val registerRootSuiteBlock = CallableId(StandardClassIds.BASE_TEST_PACKAGE, Name.identifier("registerRootSuiteBlock"))
    val runRootSuites = CallableId(StandardClassIds.BASE_TEST_PACKAGE, Name.identifier("runRootSuites"))
    val checkStaticInitializationState = "checkStaticInitializationState".wasmCallableId
}

