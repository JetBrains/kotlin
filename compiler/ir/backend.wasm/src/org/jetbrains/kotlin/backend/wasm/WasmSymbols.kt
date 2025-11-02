/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.PreSerializationWasmSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.JsCommonSymbols
import org.jetbrains.kotlin.ir.backend.js.ReflectionSymbols
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget

@OptIn(InternalSymbolFinderAPI::class)
class WasmSymbols(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
) : PreSerializationWasmSymbols by PreSerializationWasmSymbols.Impl(irBuiltIns), JsCommonSymbols(irBuiltIns) {
    internal inner class WasmReflectionSymbols : ReflectionSymbols {
        override val createKType: IrSimpleFunctionSymbol = CallableIds.createKType.functionSymbol()
        override val getKClass: IrSimpleFunctionSymbol = CallableIds.getKClass.functionSymbol()
        override val getKClassFromExpression: IrSimpleFunctionSymbol = CallableIds.getKClassFromExpression.functionSymbol()
        override val createDynamicKType: IrSimpleFunctionSymbol get() = error("Dynamic type is not supported by Wasm")
        override val createKTypeParameter: IrSimpleFunctionSymbol = CallableIds.createKTypeParameter.functionSymbol()
        override val getStarKTypeProjection = CallableIds.getStarKTypeProjection.functionSymbol()
        override val createCovariantKTypeProjection = CallableIds.createCovariantKTypeProjection.functionSymbol()
        override val createInvariantKTypeProjection = CallableIds.createInvariantKTypeProjection.functionSymbol()
        override val createContravariantKTypeProjection = CallableIds.createContravariantKTypeProjection.functionSymbol()
        override val kTypeClass: IrClassSymbol = ClassIds.kTypeClass.classSymbol()

        val wasmTypeInfoData: IrClassSymbol = ClassIds.TypeInfoData.classSymbol()
        val kClassImpl: IrClassSymbol = ClassIds.KClassImpl.classSymbol()
        val kClassInterfaceImpl: IrClassSymbol = ClassIds.KClassInterfaceImpl.classSymbol()
        val isSupportedInterface = CallableIds.isSupportedInterface.functionSymbol()
        val getInterfaceVTable = CallableIds.getInterfaceVTable.functionSymbol()
        val wasmGetInterfaceVTableBodyImpl = CallableIds.wasmGetInterfaceVTableBodyImpl.functionSymbol()
        val kFunctionImpl: IrClassSymbol = ClassIds.KFunctionImpl.classSymbol()
        val kFunctionErrorImpl: IrClassSymbol = ClassIds.KFunctionErrorImpl.classSymbol()
    }

    internal val reflectionSymbols: WasmReflectionSymbols = WasmReflectionSymbols()

    internal val eagerInitialization: IrClassSymbol = ClassIds.EagerInitialization.classSymbol()

    internal val isNotFirstWasmExportCall: IrPropertySymbol = CallableIds.isNotFirstWasmExportCall.propertySymbol()

    val tryGetAssociatedObject = CallableIds.tryGetAssociatedObjectWithWrapper.functionSymbolOrNull()
        ?: CallableIds.tryGetAssociatedObject.functionSymbol()

    internal val callAssociatedObjectGetter = CallableIds.callAssociatedObjectGetter.functionSymbol()
    val registerModuleDescriptor = CallableIds.registerModuleDescriptorWithWrapper.functionSymbolOrNull()
        ?: CallableIds.registerModuleDescriptor.functionSymbol()

    internal val wasmLongImmutableArray = ClassIds.WasmLongImmutableArray.classSymbol()

    val asserts: Iterable<IrSimpleFunctionSymbol> = CallableIds.assert.functionSymbols()
    override val throwNullPointerException = CallableIds.THROW_NPE.functionSymbol()
    override val throwISE = CallableIds.THROW_ISE.functionSymbol()
    override val throwTypeCastException = CallableIds.THROW_CCE.functionSymbol()
    val throwTypeCastWithInfoException = CallableIds.THROW_CCE_WITH_INFO.functionSymbol()
    override val throwIAE = CallableIds.THROW_IAE.functionSymbol()
    val throwNoBranchMatchedException = CallableIds.throwNoBranchMatchedException.functionSymbol()
    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol
        get() = TODO()
    override val stringBuilder = ClassIds.StringBuilder.classSymbol()
    override val getContinuation = CallableIds.getContinuation.functionSymbol()
    override val returnIfSuspended = CallableIds.returnIfSuspended.functionSymbol()

    val throwLinkageError = CallableIds.throwIrLinkageError.functionSymbol()

    val appendable = ClassIds.Appendable.classSymbol()

    val enumValueOfIntrinsic = CallableIds.enumValueOfIntrinsic.functionSymbol()
    val enumValuesIntrinsic = CallableIds.enumValuesIntrinsic.functionSymbol()
    val enumEntriesIntrinsic = CallableIds.enumEntriesIntrinsic.functionSymbol()

    val coroutineEmptyContinuation: IrPropertySymbol = CallableIds.EmptyContinuation.propertySymbol()

    override val functionAdapter = ClassIds.FunctionAdapter.classSymbol()

    override val defaultConstructorMarker: IrClassSymbol = ClassIds.DefaultConstructorMarker.classSymbol()

    val wasmUnreachable = CallableIds.wasm_unreachable.functionSymbol()

    val voidType by ClassIds.Void.defaultType()

    private val consumeAnyIntoVoid = CallableIds.consumeAnyIntoVoid.functionSymbol()

    private val consumePrimitiveIntoVoid = mapOf(
        irBuiltIns.booleanType to CallableIds.consumeBooleanIntoVoid.functionSymbol(),
        irBuiltIns.byteType to CallableIds.consumeByteIntoVoid.functionSymbol(),
        irBuiltIns.shortType to CallableIds.consumeShortIntoVoid.functionSymbol(),
        irBuiltIns.charType to CallableIds.consumeCharIntoVoid.functionSymbol(),
        irBuiltIns.intType to CallableIds.consumeIntIntoVoid.functionSymbol(),
        irBuiltIns.longType to CallableIds.consumeLongIntoVoid.functionSymbol(),
        irBuiltIns.floatType to CallableIds.consumeFloatIntoVoid.functionSymbol(),
        irBuiltIns.doubleType to CallableIds.consumeDoubleIntoVoid.functionSymbol()
    )

    fun findVoidConsumer(type: IrType): IrSimpleFunctionSymbol =
        consumePrimitiveIntoVoid[type] ?: consumeAnyIntoVoid

    val equalityFunctions =
        mapOf(
            irBuiltIns.booleanType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.byteType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.shortType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.ubyteType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.ushortType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.charType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.intType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.uintType to CallableIds.wasm_i32_eq.functionSymbol(),
            irBuiltIns.longType to CallableIds.wasm_i64_eq.functionSymbol(),
            irBuiltIns.ulongType to CallableIds.wasm_i64_eq.functionSymbol()
        )

    val floatEqualityFunctions = mapOf(
        irBuiltIns.floatType to CallableIds.wasm_f32_eq.functionSymbol(),
        irBuiltIns.doubleType to CallableIds.wasm_f64_eq.functionSymbol()
    )

    private fun wasmPrimitiveTypeName(classifier: IrClassifierSymbol): String = with(irBuiltIns) {
        when (classifier) {
            booleanClass, byteClass, shortClass, charClass, intClass -> "i32"
            floatClass -> "f32"
            doubleClass -> "f64"
            longClass -> "i64"
            else -> error("Unknown primitive type")
        }
    }

    val comparisonBuiltInsToWasmIntrinsics = irBuiltIns.run {
        listOf(
            lessFunByOperandType to "lt",
            lessOrEqualFunByOperandType to "le",
            greaterOrEqualFunByOperandType to "ge",
            greaterFunByOperandType to "gt"
        ).map { (typeToBuiltIn, wasmOp) ->
            typeToBuiltIn.map { (type, builtin) ->
                val wasmType = wasmPrimitiveTypeName(type)
                val markSign = if (wasmType == "i32" || wasmType == "i64") "_s" else ""
                builtin to "wasm_${wasmType}_$wasmOp$markSign".wasmCallableId.functionSymbol()
            }
        }.flatten().toMap()
    }

    val booleanAnd = CallableIds.wasm_i32_and.functionSymbol()
    val refEq = CallableIds.wasm_ref_eq.functionSymbol()
    val refIsNull = CallableIds.wasm_ref_is_null.functionSymbol()
    val refTest = CallableIds.wasm_ref_test.functionSymbol()
    val refCastNull = CallableIds.wasm_ref_cast_null.functionSymbol()
    val wasmArrayCopy = CallableIds.wasm_array_copy.functionSymbol()
    val wasmArrayNewData0 = CallableIds.array_new_data0.functionSymbol()
    val wasmArrayNewData = CallableIds.array_new_data.functionSymbol()
    val wasmArrayNewData0CharArray = CallableIds.array_new_data0_char_array.functionSymbolOrNull()

    val intToLong = CallableIds.wasm_i64_extend_i32_s.functionSymbol()

    val rangeCheck = CallableIds.rangeCheck.functionSymbol()

    val getBoxedBoolean: IrSimpleFunctionSymbol = CallableIds.getBoxedBoolean.functionSymbol()
    val boxBoolean: IrSimpleFunctionSymbol = CallableIds.boxBoolean.functionSymbol()
    val boxIntrinsic: IrSimpleFunctionSymbol = CallableIds.boxIntrinsic.functionSymbol()
    val unboxIntrinsic: IrSimpleFunctionSymbol = CallableIds.unboxIntrinsic.functionSymbol()

    val createString = CallableIds.createString.functionSymbol()

    val getWasmAbiVersion = CallableIds.getWasmAbiVersion.functionSymbol()

    val registerRootSuiteBlock = CallableIds.registerRootSuiteBlock.functionSymbolOrNull()
    val runRootSuites = CallableIds.runRootSuites.functionSymbolOrNull()

    val wasmTypeId = CallableIds.wasmTypeId.functionSymbol()
    val wasmGetTypeRtti = CallableIds.wasmGetTypeRtti.functionSymbol()
    val wasmGetRttiSupportedInterfaces = CallableIds.wasmGetRttiSupportedInterfaces.functionSymbol()
    val wasmGetRttiIntField = CallableIds.wasmGetRttiIntField.functionSymbol()
    val wasmGetRttiLongField = CallableIds.wasmGetRttiLongField.functionSymbol()
    val wasmGetRttiSuperClass = CallableIds.wasmGetRttiSuperClass.functionSymbol()
    val wasmGetObjectRtti = CallableIds.wasmGetObjectRtti.functionSymbol()
    val wasmGetQualifierImpl = CallableIds.wasmGetQualifierImpl.functionSymbol()
    val wasmGetSimpleNameImpl = CallableIds.wasmGetSimpleNameImpl.functionSymbol()
    val wasmArrayAnyIndexOfValue = CallableIds.wasmArrayAnyIndexOfValue.functionSymbol()

    val wasmIsInterface = CallableIds.wasmIsInterface.functionSymbol()

    val nullableEquals = CallableIds.nullableEquals.functionSymbol()
    val anyNtoString = CallableIds.anyNtoString.functionSymbol()

    val nullableFloatIeee754Equals = CallableIds.nullableFloatIeee754Equals.functionSymbol()
    val nullableDoubleIeee754Equals = CallableIds.nullableDoubleIeee754Equals.functionSymbol()

    val returnArgumentIfItIsKotlinAny = CallableIds.returnArgumentIfItIsKotlinAny.functionSymbol()

    val startCoroutineUninterceptedOrReturnIntrinsics = listOf(
        CallableIds.startCoroutineUninterceptedOrReturnIntrinsics0.functionSymbol(),
        CallableIds.startCoroutineUninterceptedOrReturnIntrinsics1.functionSymbol(),
        CallableIds.startCoroutineUninterceptedOrReturnIntrinsics2.functionSymbol(),
    )

    // KProperty implementations
    val kLocalDelegatedPropertyImpl: IrClassSymbol = ClassIds.KLocalDelegatedPropertyImpl.classSymbol()
    val kLocalDelegatedMutablePropertyImpl: IrClassSymbol = ClassIds.KLocalDelegatedMutablePropertyImpl.classSymbol()
    val kProperty0Impl: IrClassSymbol = ClassIds.KProperty0Impl.classSymbol()
    val kProperty1Impl: IrClassSymbol = ClassIds.KProperty1Impl.classSymbol()
    val kProperty2Impl: IrClassSymbol = ClassIds.KProperty2Impl.classSymbol()
    val kMutableProperty0Impl: IrClassSymbol = ClassIds.KMutableProperty0Impl.classSymbol()
    val kMutableProperty1Impl: IrClassSymbol = ClassIds.KMutableProperty1Impl.classSymbol()
    val kMutableProperty2Impl: IrClassSymbol = ClassIds.KMutableProperty2Impl.classSymbol()

    val arraysCopyInto = CallableIds.copyInto.functionSymbols()

    private val contentToString: List<IrSimpleFunctionSymbol> = CallableIds.contentToString.functionSymbols()

    private val contentHashCode: List<IrSimpleFunctionSymbol> = CallableIds.contentHashCode.functionSymbols()

    private fun findNullableOverloadForReceiver(arrayType: IrType, overloadsList: List<IrSimpleFunctionSymbol>): IrSimpleFunctionSymbol =
        overloadsList.first {
            val receiverType = it.owner.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
            receiverType != null && receiverType.isNullable() && arrayType.classOrNull == receiverType.classOrNull
        }

    fun findContentToStringOverload(arrayType: IrType): IrSimpleFunctionSymbol = findNullableOverloadForReceiver(arrayType, contentToString)

    fun findContentHashCodeOverload(arrayType: IrType): IrSimpleFunctionSymbol = findNullableOverloadForReceiver(arrayType, contentHashCode)

    val wasmStructRefType by ClassIds.structref.defaultType()

    val wasmAnyRefClass = ClassIds.anyref.classSymbol()

    inner class JsInteropAdapters {
        val kotlinToJsStringAdapter = CallableIds.kotlinToJsStringAdapter.functionSymbol()
        val kotlinToJsAnyAdapter = CallableIds.kotlinToJsAnyAdapter.functionSymbol()
        val numberToDoubleAdapter = CallableIds.numberToDoubleAdapter.functionSymbol()

        val jsCheckIsNullOrUndefinedAdapter = CallableIds.jsCheckIsNullOrUndefinedAdapter.functionSymbol()

        val jsToKotlinStringAdapter = CallableIds.jsToKotlinStringAdapter.functionSymbol()
        val jsToKotlinAnyAdapter = CallableIds.jsToKotlinAnyAdapter.functionSymbol()

        val jsToKotlinByteAdapter = CallableIds.jsToKotlinByteAdapter.functionSymbol()
        val jsToKotlinShortAdapter = CallableIds.jsToKotlinShortAdapter.functionSymbol()
        val jsToKotlinCharAdapter = CallableIds.jsToKotlinCharAdapter.functionSymbol()

        val externRefToKotlinIntAdapter = CallableIds.externRefToKotlinIntAdapter.functionSymbol()
        val externRefToKotlinBooleanAdapter = CallableIds.externRefToKotlinBooleanAdapter.functionSymbol()
        val externRefToKotlinLongAdapter = CallableIds.externRefToKotlinLongAdapter.functionSymbol()
        val externRefToKotlinFloatAdapter = CallableIds.externRefToKotlinFloatAdapter.functionSymbol()
        val externRefToKotlinDoubleAdapter = CallableIds.externRefToKotlinDoubleAdapter.functionSymbol()

        val externRefToKotlinUByteAdapter = CallableIds.externRefToKotlinUByteAdapter.functionSymbol()
        val externRefToKotlinUShortAdapter = CallableIds.externRefToKotlinUShortAdapter.functionSymbol()
        val externRefToKotlinUIntAdapter = CallableIds.externRefToKotlinUIntAdapter.functionSymbol()
        val externRefToKotlinULongAdapter = CallableIds.externRefToKotlinULongAdapter.functionSymbol()

        val kotlinIntToExternRefAdapter = CallableIds.kotlinIntToExternRefAdapter.functionSymbol()
        val kotlinBooleanToExternRefAdapter = CallableIds.kotlinBooleanToExternRefAdapter.functionSymbol()
        val kotlinLongToExternRefAdapter = CallableIds.kotlinLongToExternRefAdapter.functionSymbol()
        val kotlinFloatToExternRefAdapter = CallableIds.kotlinFloatToExternRefAdapter.functionSymbol()
        val kotlinDoubleToExternRefAdapter = CallableIds.kotlinDoubleToExternRefAdapter.functionSymbol()
        val kotlinByteToExternRefAdapter = CallableIds.kotlinByteToExternRefAdapter.functionSymbol()
        val kotlinShortToExternRefAdapter = CallableIds.kotlinShortToExternRefAdapter.functionSymbol()
        val kotlinCharToExternRefAdapter = CallableIds.kotlinCharToExternRefAdapter.functionSymbol()

        val kotlinUByteToJsNumber = CallableIds.kotlinUByteToJsNumber.functionSymbol()
        val kotlinUShortToJsNumber = CallableIds.kotlinUShortToJsNumber.functionSymbol()
        val kotlinUIntToJsNumber = CallableIds.kotlinUIntToJsNumber.functionSymbol()
        val kotlinULongToJsBigInt = CallableIds.kotlinULongToJsBigInt.functionSymbol()
    }

    inner class JsRelatedSymbols {
        val jsInteropAdapters = JsInteropAdapters()

        val jsExportConstructor by ClassIds.JsExport.primaryConstructorSymbol()
        val jsExportDefaultConstructor by ClassIds.JsExportDefault.primaryConstructorSymbol()
        val jsNameConstructor by ClassIds.JsName.primaryConstructorSymbol()
        val jsFunConstructor by ClassIds.JsFun.primaryConstructorSymbol()

        val jsCode = CallableIds.js.functionSymbol()

        val jsReferenceClass = ClassIds.JsReference.classSymbol()

        val jsAnyType: IrType by ClassIds.JsAny.defaultType()
        val jsBooleanType: IrType by ClassIds.JsBoolean.defaultType()
        val jsStringType: IrType by ClassIds.JsString.defaultType()
        val jsNumberType: IrType by ClassIds.JsNumber.defaultType()
        val jsBigIntType: IrType by ClassIds.JsBigInt.defaultType()

        val newJsArray = CallableIds.newJsArray.functionSymbol()

        val jsArrayPush = CallableIds.jsArrayPush.functionSymbol()

        val externRefIsNull = CallableIds.wasm_externref_is_null.functionSymbol()

        val jsPromise = ClassIds.Promise.classSymbol()

        val kExternalClassImpl: IrClassSymbol = ClassIds.KExternalClassImpl.classSymbol()

        val jsException = ClassIds.JsException.classSymbol()

        val getKotlinException = CallableIds.getKotlinException.functionSymbol()

        val throwValue = CallableIds.throwValue.functionSymbol()

        val throw0 = CallableIds.throw0.functionSymbol()
    }

    val wasmExportConstructor by ClassIds.WasmExport.primaryConstructorSymbol()

    private val jsRelatedSymbolsIfNonWasi = if (configuration.wasmTarget == WasmTarget.JS) JsRelatedSymbols() else null

    val jsRelatedSymbols get() = jsRelatedSymbolsIfNonWasi ?: error("Cannot access to js related std in wasi mode")

    private val invokeOnExportedFunctionExitIfWasi =
        when (configuration.wasmTarget == WasmTarget.WASI) {
            true -> CallableIds.invokeOnExportedFunctionExit.functionSymbol()
            else -> null
        }

    val invokeOnExportedFunctionExit get() = invokeOnExportedFunctionExitIfWasi ?: error("Cannot access to wasi related std in js mode")
}

private object ClassIds {
    // WASM internal class
    private val String.wasmClassId get() = ClassId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE, Name.identifier(this))
    val TypeInfoData = "TypeInfoData".wasmClassId
    val KClassImpl = "KClassImpl".wasmClassId
    val KClassInterfaceImpl = "KClassInterfaceImpl".wasmClassId
    val KFunctionImpl = "KFunctionImpl".wasmClassId
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
    val anyref = ClassId(WasmStandardClassIds.BASE_WASM_INTERNAL_PACKAGE.child(Name.identifier("reftypes")), Name.identifier("anyref"))
    val WasmExport = ClassId(WasmStandardClassIds.BASE_WASM_PACKAGE, Name.identifier("WasmExport"))
}

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
    val tryGetAssociatedObjectWithWrapper = "tryGetAssociatedObjectWithWrapper".wasmCallableId
    val callAssociatedObjectGetter = "callAssociatedObjectGetter".wasmCallableId
    val registerModuleDescriptor = "registerModuleDescriptor".wasmCallableId
    val registerModuleDescriptorWithWrapper = "registerModuleDescriptorWithWrapper".wasmCallableId
    val THROW_NPE = "THROW_NPE".wasmCallableId
    val THROW_ISE = "THROW_ISE".wasmCallableId
    val THROW_CCE = "THROW_CCE".wasmCallableId
    val THROW_CCE_WITH_INFO = "THROW_CCE_WITH_INFO".wasmCallableId
    val THROW_IAE = "THROW_IAE".wasmCallableId
    val throwNoBranchMatchedException = "throwNoBranchMatchedException".wasmCallableId
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
    val startCoroutineUninterceptedOrReturnIntrinsics0 = "startCoroutineUninterceptedOrReturnIntrinsic0".wasmCallableId
    val startCoroutineUninterceptedOrReturnIntrinsics1 = "startCoroutineUninterceptedOrReturnIntrinsic1".wasmCallableId
    val startCoroutineUninterceptedOrReturnIntrinsics2 = "startCoroutineUninterceptedOrReturnIntrinsic2".wasmCallableId
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
}

