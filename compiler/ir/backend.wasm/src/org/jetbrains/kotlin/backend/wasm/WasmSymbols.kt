/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.backend.common.ir.PreSerializationWasmSymbols
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.JsCommonSymbols
import org.jetbrains.kotlin.ir.backend.js.ReflectionSymbols
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.name.StandardClassIds.BASE_KOTLIN_PACKAGE
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.wasm.config.wasmTarget

@OptIn(ObsoleteDescriptorBasedAPI::class, InternalSymbolFinderAPI::class)
class WasmSymbols(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
) : PreSerializationWasmSymbols by PreSerializationWasmSymbols.Impl(irBuiltIns), JsCommonSymbols(irBuiltIns) {

    private val enumsInternalPackageFqName = FqName("kotlin.enums")
    private val kotlinJsPackageFqName = FqName("kotlin.js")
    private val kotlinTestPackageFqName = FqName("kotlin.test")

    internal inner class WasmReflectionSymbols : ReflectionSymbols {
        override val createKType: IrSimpleFunctionSymbol = getInternalWasmFunction("createKType")
        override val getKClass: IrSimpleFunctionSymbol = getInternalWasmFunction("getKClass")
        override val getKClassFromExpression: IrSimpleFunctionSymbol = getInternalWasmFunction("getKClassFromExpression")
        override val createDynamicKType: IrSimpleFunctionSymbol get() = error("Dynamic type is not supported by Wasm")
        override val createKTypeParameter: IrSimpleFunctionSymbol = getInternalWasmFunction("createKTypeParameter")
        override val getStarKTypeProjection = getInternalWasmFunction("getStarKTypeProjection")
        override val createCovariantKTypeProjection = getInternalWasmFunction("createCovariantKTypeProjection")
        override val createInvariantKTypeProjection = getInternalWasmFunction("createInvariantKTypeProjection")
        override val createContravariantKTypeProjection = getInternalWasmFunction("createContravariantKTypeProjection")
        override val kTypeClass: IrClassSymbol = getIrClass(FqName("kotlin.reflect.KClass"))

        val wasmTypeInfoData: IrClassSymbol = getInternalWasmClass("TypeInfoData")
        val kClassImpl: IrClassSymbol = getInternalWasmClass("KClassImpl")
        val kClassInterfaceImpl: IrClassSymbol = getInternalWasmClass("KClassInterfaceImpl")
        val isSupportedInterface = getInternalWasmFunction("isSupportedInterface")
        val getInterfaceVTable = getInternalWasmFunction("getInterfaceVTable")
        val wasmGetInterfaceVTableBodyImpl = getInternalWasmFunction("wasmGetInterfaceVTableBodyImpl")
        val kFunctionImpl: IrClassSymbol = getInternalWasmClass("KFunctionImpl")
        val kFunctionErrorImpl: IrClassSymbol = getInternalWasmClass("KFunctionErrorImpl")
    }

    internal val reflectionSymbols: WasmReflectionSymbols = WasmReflectionSymbols()

    internal val eagerInitialization: IrClassSymbol = getIrClass(FqName("kotlin.EagerInitialization"))

    internal val isNotFirstWasmExportCall: IrPropertySymbol = symbolFinder.topLevelProperty(
        FqName.fromSegments(listOf("kotlin", "wasm", "internal")),
        "isNotFirstWasmExportCall"
    )

    val tryGetAssociatedObject = getInternalWasmFunction("tryGetAssociatedObject")
    internal val callAssociatedObjectGetter = getInternalWasmFunction("callAssociatedObjectGetter")
    val registerModuleDescriptor = getInternalWasmFunction("registerModuleDescriptor")

    internal val wasmLongImmutableArray = getInternalWasmClass("WasmLongImmutableArray")

    val asserts: Iterable<IrSimpleFunctionSymbol> =
        CallableId(BASE_KOTLIN_PACKAGE, Name.identifier("assert")).functionSymbols()
    override val throwNullPointerException = getInternalWasmFunction("THROW_NPE")
    override val throwISE = getInternalWasmFunction("THROW_ISE")
    override val throwTypeCastException = getInternalWasmFunction("THROW_CCE")
    val throwTypeCastWithInfoException = getInternalWasmFunction("THROW_CCE_WITH_INFO")
    override val throwIAE = getInternalWasmFunction("THROW_IAE")
    val throwNoBranchMatchedException =
        getInternalWasmFunction("throwNoBranchMatchedException")
    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol
        get() = TODO()
    override val stringBuilder =
        getIrClass(FqName("kotlin.text.StringBuilder"))
    override val getContinuation =
        getInternalWasmFunction("getContinuation")
    override val returnIfSuspended =
        getInternalWasmFunction("returnIfSuspended")

    private val _arraysContentEquals = symbolFinder.topLevelFunctions(COLLECTIONS_PACKAGE_FQ_NAME, "contentEquals").filter {
        it.descriptor.extensionReceiverParameter?.type?.isMarkedNullable == true
    }
    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
        get() = _arraysContentEquals.associateBy { it.owner.parameters[0].type.makeNotNull() }

    val throwLinkageError = getFunction("throwIrLinkageError", StandardClassIds.BASE_INTERNAL_PACKAGE)

    val appendable = getIrClass(FqName.fromSegments(listOf("kotlin", "text", "Appendable")))

    val enumValueOfIntrinsic = getInternalWasmFunction("enumValueOfIntrinsic")
    val enumValuesIntrinsic = getInternalWasmFunction("enumValuesIntrinsic")
    val enumEntriesIntrinsic = getEnumsFunction("enumEntriesIntrinsic")

    val coroutineEmptyContinuation: IrPropertySymbol = symbolFinder.topLevelProperty(
        FqName.fromSegments(listOf("kotlin", "wasm", "internal")),
        "EmptyContinuation"
    )

    override val functionAdapter = getInternalWasmClass("FunctionAdapter")

    override val defaultConstructorMarker: IrClassSymbol = getInternalWasmClass("DefaultConstructorMarker")

    val wasmUnreachable = getInternalWasmFunction("wasm_unreachable")

    val voidClass = getIrClass(FqName("kotlin.wasm.internal.Void"))
    val voidType by lazy { voidClass.defaultType }

    val uByteType by lazy { getIrClass(FqName("kotlin.UByte")).defaultType }
    val uShortType by lazy { getIrClass(FqName("kotlin.UShort")).defaultType }
    val uIntType by lazy { getIrClass(FqName("kotlin.UInt")).defaultType }
    val uLongType by lazy { getIrClass(FqName("kotlin.ULong")).defaultType }

    private val consumeAnyIntoVoid = getInternalWasmFunction("consumeAnyIntoVoid")

    private val consumePrimitiveIntoVoid = mapOf(
        irBuiltIns.booleanType to getInternalWasmFunction("consumeBooleanIntoVoid"),
        irBuiltIns.byteType to getInternalWasmFunction("consumeByteIntoVoid"),
        irBuiltIns.shortType to getInternalWasmFunction("consumeShortIntoVoid"),
        irBuiltIns.charType to getInternalWasmFunction("consumeCharIntoVoid"),
        irBuiltIns.intType to getInternalWasmFunction("consumeIntIntoVoid"),
        irBuiltIns.longType to getInternalWasmFunction("consumeLongIntoVoid"),
        irBuiltIns.floatType to getInternalWasmFunction("consumeFloatIntoVoid"),
        irBuiltIns.doubleType to getInternalWasmFunction("consumeDoubleIntoVoid")
    )

    fun findVoidConsumer(type: IrType): IrSimpleFunctionSymbol =
        consumePrimitiveIntoVoid[type] ?: consumeAnyIntoVoid

    val equalityFunctions =
        mapOf(
            irBuiltIns.booleanType to getInternalWasmFunction("wasm_i32_eq"),
            irBuiltIns.byteType to getInternalWasmFunction("wasm_i32_eq"),
            irBuiltIns.shortType to getInternalWasmFunction("wasm_i32_eq"),
            uByteType to getInternalWasmFunction("wasm_i32_eq"),
            uShortType to getInternalWasmFunction("wasm_i32_eq"),
            irBuiltIns.charType to getInternalWasmFunction("wasm_i32_eq"),
            irBuiltIns.intType to getInternalWasmFunction("wasm_i32_eq"),
            uIntType to getInternalWasmFunction("wasm_i32_eq"),
            irBuiltIns.longType to getInternalWasmFunction("wasm_i64_eq"),
            uLongType to getInternalWasmFunction("wasm_i64_eq")
        )

    val floatEqualityFunctions = mapOf(
        irBuiltIns.floatType to getInternalWasmFunction("wasm_f32_eq"),
        irBuiltIns.doubleType to getInternalWasmFunction("wasm_f64_eq")
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
                builtin to getInternalWasmFunction("wasm_${wasmType}_$wasmOp$markSign")
            }
        }.flatten().toMap()
    }

    val booleanAnd = getInternalWasmFunction("wasm_i32_and")
    val refEq = getInternalWasmFunction("wasm_ref_eq")
    val refIsNull = getInternalWasmFunction("wasm_ref_is_null")
    val refTest = getInternalWasmFunction("wasm_ref_test")
    val refCastNull = getInternalWasmFunction("wasm_ref_cast_null")
    val wasmArrayCopy = getInternalWasmFunction("wasm_array_copy")
    val wasmArrayNewData0 = getInternalWasmFunction("array_new_data0")
    val wasmArrayNewData = getInternalWasmFunction("array_new_data")
    val wasmArrayNewData0CharArray = maybeGetFunction("array_new_data0_char_array", PreSerializationWasmSymbols.Impl.wasmInternalFqName)

    val intToLong = getInternalWasmFunction("wasm_i64_extend_i32_s")

    val rangeCheck = getInternalWasmFunction("rangeCheck")

    val getBoxedBoolean: IrSimpleFunctionSymbol = getInternalWasmFunction("getBoxedBoolean")
    val boxBoolean: IrSimpleFunctionSymbol = getInternalWasmFunction("boxBoolean")
    val boxIntrinsic: IrSimpleFunctionSymbol = getInternalWasmFunction("boxIntrinsic")
    val unboxIntrinsic: IrSimpleFunctionSymbol = getInternalWasmFunction("unboxIntrinsic")

    val createString = getFunction("createString", StandardNames.BUILT_INS_PACKAGE_FQ_NAME)

    val getWasmAbiVersion = getInternalWasmFunction("getWasmAbiVersion")

    val registerRootSuiteBlock = maybeGetFunction("registerRootSuiteBlock", kotlinTestPackageFqName)
    val runRootSuites = maybeGetFunction("runRootSuites", kotlinTestPackageFqName)

    val wasmTypeId = getInternalWasmFunction("wasmTypeId")
    val wasmGetTypeRtti = getInternalWasmFunction("wasmGetTypeRtti")
    val wasmGetRttiSupportedInterfaces = getInternalWasmFunction("wasmGetRttiSupportedInterfaces")
    val wasmGetRttiIntField = getInternalWasmFunction("wasmGetRttiIntField")
    val wasmGetRttiLongField = getInternalWasmFunction("wasmGetRttiLongField")
    val wasmGetRttiSuperClass = getInternalWasmFunction("wasmGetRttiSuperClass")
    val wasmGetObjectRtti = getInternalWasmFunction("wasmGetObjectRtti")
    val wasmGetQualifierImpl = getInternalWasmFunction("wasmGetQualifierImpl")
    val wasmGetSimpleNameImpl = getInternalWasmFunction("wasmGetSimpleNameImpl")
    val wasmArrayAnyIndexOfValue = getInternalWasmFunction("wasmArrayAnyIndexOfValue")

    val wasmIsInterface = getInternalWasmFunction("wasmIsInterface")

    val nullableEquals = getInternalWasmFunction("nullableEquals")
    val anyNtoString = getInternalWasmFunction("anyNtoString")

    val nullableFloatIeee754Equals = getInternalWasmFunction("nullableFloatIeee754Equals")
    val nullableDoubleIeee754Equals = getInternalWasmFunction("nullableDoubleIeee754Equals")

    val returnArgumentIfItIsKotlinAny = getInternalWasmFunction("returnArgumentIfItIsKotlinAny")

    val startCoroutineUninterceptedOrReturnIntrinsics =
        (0..2).map { getInternalWasmFunction("startCoroutineUninterceptedOrReturnIntrinsic$it") }

    // KProperty implementations
    val kLocalDelegatedPropertyImpl: IrClassSymbol = getInternalWasmClass("KLocalDelegatedPropertyImpl")
    val kLocalDelegatedMutablePropertyImpl: IrClassSymbol = getInternalWasmClass("KLocalDelegatedMutablePropertyImpl")
    val kProperty0Impl: IrClassSymbol = getInternalWasmClass("KProperty0Impl")
    val kProperty1Impl: IrClassSymbol = getInternalWasmClass("KProperty1Impl")
    val kProperty2Impl: IrClassSymbol = getInternalWasmClass("KProperty2Impl")
    val kMutableProperty0Impl: IrClassSymbol = getInternalWasmClass("KMutableProperty0Impl")
    val kMutableProperty1Impl: IrClassSymbol = getInternalWasmClass("KMutableProperty1Impl")
    val kMutableProperty2Impl: IrClassSymbol = getInternalWasmClass("KMutableProperty2Impl")

    val arraysCopyInto = symbolFinder.topLevelFunctions(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, "copyInto")

    private val contentToString: List<IrSimpleFunctionSymbol> =
        symbolFinder.topLevelFunctions(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, "contentToString").toList()

    private val contentHashCode: List<IrSimpleFunctionSymbol> =
        symbolFinder.topLevelFunctions(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, "contentHashCode").toList()

    private fun findNullableOverloadForReceiver(arrayType: IrType, overloadsList: List<IrSimpleFunctionSymbol>): IrSimpleFunctionSymbol =
        overloadsList.first {
            val receiverType = it.owner.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type
            receiverType != null && receiverType.isNullable() && arrayType.classOrNull == receiverType.classOrNull
        }

    fun findContentToStringOverload(arrayType: IrType): IrSimpleFunctionSymbol = findNullableOverloadForReceiver(arrayType, contentToString)

    fun findContentHashCodeOverload(arrayType: IrType): IrSimpleFunctionSymbol = findNullableOverloadForReceiver(arrayType, contentHashCode)

    private val wasmStructRefClass = getIrClass(FqName("kotlin.wasm.internal.reftypes.structref"))
    val wasmStructRefType by lazy { wasmStructRefClass.defaultType }

    val wasmAnyRefClass = getIrClass(FqName("kotlin.wasm.internal.reftypes.anyref"))

    inner class JsInteropAdapters {
        val kotlinToJsStringAdapter = getInternalWasmFunction("kotlinToJsStringAdapter")
        val kotlinToJsAnyAdapter = getInternalWasmFunction("kotlinToJsAnyAdapter")
        val numberToDoubleAdapter = getInternalWasmFunction("numberToDoubleAdapter")

        val jsCheckIsNullOrUndefinedAdapter = getInternalWasmFunction("jsCheckIsNullOrUndefinedAdapter")

        val jsToKotlinStringAdapter = getInternalWasmFunction("jsToKotlinStringAdapter")
        val jsToKotlinAnyAdapter = getInternalWasmFunction("jsToKotlinAnyAdapter")

        val jsToKotlinByteAdapter = getInternalWasmFunction("jsToKotlinByteAdapter")
        val jsToKotlinShortAdapter = getInternalWasmFunction("jsToKotlinShortAdapter")
        val jsToKotlinCharAdapter = getInternalWasmFunction("jsToKotlinCharAdapter")

        val externRefToKotlinIntAdapter = getInternalWasmFunction("externRefToKotlinIntAdapter")
        val externRefToKotlinBooleanAdapter = getInternalWasmFunction("externRefToKotlinBooleanAdapter")
        val externRefToKotlinLongAdapter = getInternalWasmFunction("externRefToKotlinLongAdapter")
        val externRefToKotlinFloatAdapter = getInternalWasmFunction("externRefToKotlinFloatAdapter")
        val externRefToKotlinDoubleAdapter = getInternalWasmFunction("externRefToKotlinDoubleAdapter")

        val externRefToKotlinUByteAdapter = getInternalWasmFunction("externRefToKotlinUByteAdapter")
        val externRefToKotlinUShortAdapter = getInternalWasmFunction("externRefToKotlinUShortAdapter")
        val externRefToKotlinUIntAdapter = getInternalWasmFunction("externRefToKotlinUIntAdapter")
        val externRefToKotlinULongAdapter = getInternalWasmFunction("externRefToKotlinULongAdapter")

        val kotlinIntToExternRefAdapter = getInternalWasmFunction("kotlinIntToExternRefAdapter")
        val kotlinBooleanToExternRefAdapter = getInternalWasmFunction("kotlinBooleanToExternRefAdapter")
        val kotlinLongToExternRefAdapter = getInternalWasmFunction("kotlinLongToExternRefAdapter")
        val kotlinFloatToExternRefAdapter = getInternalWasmFunction("kotlinFloatToExternRefAdapter")
        val kotlinDoubleToExternRefAdapter = getInternalWasmFunction("kotlinDoubleToExternRefAdapter")
        val kotlinByteToExternRefAdapter = getInternalWasmFunction("kotlinByteToExternRefAdapter")
        val kotlinShortToExternRefAdapter = getInternalWasmFunction("kotlinShortToExternRefAdapter")
        val kotlinCharToExternRefAdapter = getInternalWasmFunction("kotlinCharToExternRefAdapter")

        val kotlinUByteToJsNumber = getInternalWasmFunction("kotlinUByteToJsNumber")
        val kotlinUShortToJsNumber = getInternalWasmFunction("kotlinUShortToJsNumber")
        val kotlinUIntToJsNumber = getInternalWasmFunction("kotlinUIntToJsNumber")
        val kotlinULongToJsBigInt = getInternalWasmFunction("kotlinULongToJsBigInt")
    }

    inner class JsRelatedSymbols {
        val jsInteropAdapters = JsInteropAdapters()

        private val jsExportClass = getIrClass(FqName("kotlin.js.JsExport"))
        val jsExportConstructor by lazy { jsExportClass.constructors.single() }

        private val jsNameClass = getIrClass(FqName("kotlin.js.JsName"))
        val jsNameConstructor by lazy { jsNameClass.constructors.single() }

        private val jsFunClass = getIrClass(FqName("kotlin.JsFun"))
        val jsFunConstructor by lazy { jsFunClass.constructors.single() }

        val jsCode = getFunction("js", kotlinJsPackageFqName)

        val jsReferenceClass by lazy { getIrClass(FqName("kotlin.js.JsReference")) }

        val jsAnyType: IrType by lazy { getIrType("kotlin.js.JsAny") }
        val jsBooleanType: IrType by lazy { getIrType("kotlin.js.JsBoolean") }
        val jsStringType: IrType by lazy { getIrType("kotlin.js.JsString") }
        val jsNumberType: IrType by lazy { getIrType("kotlin.js.JsNumber") }
        val jsBigIntType: IrType by lazy { getIrType("kotlin.js.JsBigInt") }

        val newJsArray = getInternalWasmFunction("newJsArray")

        val jsArrayPush = getInternalWasmFunction("jsArrayPush")

        val externRefIsNull = getInternalWasmFunction("wasm_externref_is_null")

        val jsPromise = getIrClass(FqName("kotlin.js.Promise"))

        val kExternalClassImpl: IrClassSymbol = getInternalWasmClass("KExternalClassImpl")

        val jsException = getIrClass(FqName("kotlin.js.JsException"))

        val getKotlinException = getInternalWasmFunction("getKotlinException")

        val throwValue = getInternalWasmFunction("throwValue")

        val throw0 = getInternalWasmFunction("throw0")
    }

    private val wasmExportClass = getIrClass(FqName("kotlin.wasm.WasmExport"))
    val wasmExportConstructor by lazy { wasmExportClass.constructors.single() }

    private val jsRelatedSymbolsIfNonWasi = if (configuration.wasmTarget == WasmTarget.JS) JsRelatedSymbols() else null

    val jsRelatedSymbols get() = jsRelatedSymbolsIfNonWasi ?: error("Cannot access to js related std in wasi mode")


    private val invokeOnExportedFunctionExitIfWasi =
        when (configuration.wasmTarget == WasmTarget.WASI) {
            true -> getInternalWasmFunction("invokeOnExportedFunctionExit")
            else -> null
        }

    val invokeOnExportedFunctionExit get() = invokeOnExportedFunctionExitIfWasi ?: error("Cannot access to wasi related std in js mode")

    private fun getFunction(name: String, ownerPackage: FqName): IrSimpleFunctionSymbol {
        val callableId = CallableId(ownerPackage, Name.identifier(name))
        return callableId.functionSymbol()
    }

    private fun maybeGetFunction(name: String, ownerPackage: FqName): IrSimpleFunctionSymbol? {
        val callableId = CallableId(ownerPackage, Name.identifier(name))
        return callableId.functionSymbols().singleOrNull()
    }

    private fun getInternalWasmFunction(name: String): IrSimpleFunctionSymbol {
        val callableId = CallableId(PreSerializationWasmSymbols.Impl.wasmInternalFqName, Name.identifier(name))
        return callableId.functionSymbol()
    }

    private fun getEnumsFunction(name: String): IrSimpleFunctionSymbol {
        val callableId = CallableId(enumsInternalPackageFqName, Name.identifier(name))
        return callableId.functionSymbol()
    }

    private fun getIrClass(fqName: FqName): IrClassSymbol {
        val classId = ClassId(fqName.parent(), fqName.shortName())
        return classId.classSymbol()
    }

    private fun getIrType(fqName: String): IrType = getIrClass(FqName(fqName)).defaultType
    private fun getInternalWasmClass(name: String): IrClassSymbol = getIrClass(PreSerializationWasmSymbols.Impl.wasmInternalFqName.child(Name.identifier(name)))

}
