/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
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
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.wasm.config.wasmTarget

@OptIn(ObsoleteDescriptorBasedAPI::class, InternalSymbolFinderAPI::class)
class WasmSymbols(
    irBuiltIns: IrBuiltIns,
    configuration: CompilerConfiguration,
) : JsCommonSymbols(irBuiltIns) {

    private val enumsInternalPackageFqName = FqName("kotlin.enums")
    private val wasmInternalFqName = FqName("kotlin.wasm.internal")
    private val kotlinJsPackageFqName = FqName("kotlin.js")
    private val kotlinTestPackageFqName = FqName("kotlin.test")

    internal inner class WasmReflectionSymbols : ReflectionSymbols {
        override val createKType: IrSimpleFunctionSymbol = getInternalFunction("createKType")
        override val getKClass: IrSimpleFunctionSymbol = getInternalFunction("getKClass")
        override val getKClassFromExpression: IrSimpleFunctionSymbol = getInternalFunction("getKClassFromExpression")
        override val createDynamicKType: IrSimpleFunctionSymbol get() = error("Dynamic type is not supported by Wasm")
        override val createKTypeParameter: IrSimpleFunctionSymbol = getInternalFunction("createKTypeParameter")
        override val getStarKTypeProjection = getInternalFunction("getStarKTypeProjection")
        override val createCovariantKTypeProjection = getInternalFunction("createCovariantKTypeProjection")
        override val createInvariantKTypeProjection = getInternalFunction("createInvariantKTypeProjection")
        override val createContravariantKTypeProjection = getInternalFunction("createContravariantKTypeProjection")
        override val kTypeClass: IrClassSymbol = getIrClass(FqName("kotlin.reflect.KClass"))

        val wasmTypeInfoData: IrClassSymbol = getInternalClass("TypeInfoData")
        val kClassImpl: IrClassSymbol = getInternalClass("KClassImpl")
        val kClassInterfaceImpl: IrClassSymbol = getInternalClass("KClassInterfaceImpl")
        val isSupportedInterface = getInternalFunction("isSupportedInterface")
        val getInterfaceVTable = getInternalFunction("getInterfaceVTable")
        val wasmGetInterfaceVTableBodyImpl = getInternalFunction("wasmGetInterfaceVTableBodyImpl")
    }

    internal val reflectionSymbols: WasmReflectionSymbols = WasmReflectionSymbols()

    internal val eagerInitialization: IrClassSymbol = getIrClass(FqName("kotlin.EagerInitialization"))

    internal val isNotFirstWasmExportCall: IrPropertySymbol = symbolFinder.topLevelProperty(
        FqName.fromSegments(listOf("kotlin", "wasm", "internal")),
        "isNotFirstWasmExportCall"
    )

    internal val tryGetAssociatedObject = getInternalFunction("tryGetAssociatedObject")
    internal val wasmLongImmutableArray = getInternalClass("WasmLongImmutableArray")

    override val throwNullPointerException = getInternalFunction("THROW_NPE")
    override val throwISE = getInternalFunction("THROW_ISE")
    override val throwTypeCastException = getInternalFunction("THROW_CCE")
    override val throwIAE = getInternalFunction("THROW_IAE")
    val throwNoBranchMatchedException =
        getInternalFunction("throwNoBranchMatchedException")
    override val throwUninitializedPropertyAccessException =
        getInternalFunction("throwUninitializedPropertyAccessException")
    override val defaultConstructorMarker =
        getIrClass(FqName("kotlin.wasm.internal.DefaultConstructorMarker"))
    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol
        get() = TODO()
    override val stringBuilder =
        getIrClass(FqName("kotlin.text.StringBuilder"))
    override val coroutineImpl =
        coroutineSymbols.coroutineImpl
    override val coroutineSuspendedGetter =
        coroutineSymbols.coroutineSuspendedGetter
    override val getContinuation =
        getInternalFunction("getContinuation")
    override val continuationClass =
        coroutineSymbols.continuationClass
    override val coroutineContextGetter =
        coroutineSymbols.coroutineContextGetter
    override val suspendCoroutineUninterceptedOrReturn =
        getInternalFunction("suspendCoroutineUninterceptedOrReturn")
    override val coroutineGetContext =
        getInternalFunction("getCoroutineContext")
    override val returnIfSuspended =
        getInternalFunction("returnIfSuspended")

    private val _arraysContentEquals = symbolFinder.topLevelFunctions(COLLECTIONS_PACKAGE_FQ_NAME, "contentEquals").filter {
        it.descriptor.extensionReceiverParameter?.type?.isMarkedNullable == true
    }
    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
        get() = _arraysContentEquals.associateBy { it.owner.parameters[0].type.makeNotNull() }

    val throwLinkageError = getInternalFunction("throwLinkageError")

    val enumEntries = getIrClass(FqName.fromSegments(listOf("kotlin", "enums", "EnumEntries")))
    val createEnumEntries = symbolFinder.topLevelFunctions(enumsInternalPackageFqName, "enumEntries")
        .find { it.descriptor.valueParameters.firstOrNull()?.type?.isFunctionType == false }!!

    val appendable = getIrClass(FqName.fromSegments(listOf("kotlin", "text", "Appendable")))

    val enumValueOfIntrinsic = getInternalFunction("enumValueOfIntrinsic")
    val enumValuesIntrinsic = getInternalFunction("enumValuesIntrinsic")
    val enumEntriesIntrinsic = getEnumsFunction("enumEntriesIntrinsic")

    val coroutineEmptyContinuation: IrPropertySymbol = symbolFinder.topLevelProperty(
        FqName.fromSegments(listOf("kotlin", "wasm", "internal")),
        "EmptyContinuation"
    )

    override val functionAdapter = getInternalClass("FunctionAdapter")

    val wasmUnreachable = getInternalFunction("wasm_unreachable")

    val voidClass = getIrClass(FqName("kotlin.wasm.internal.Void"))
    val voidType by lazy { voidClass.defaultType }

    val uByteType by lazy { getIrClass(FqName("kotlin.UByte")).defaultType }
    val uShortType by lazy { getIrClass(FqName("kotlin.UShort")).defaultType }
    val uIntType by lazy { getIrClass(FqName("kotlin.UInt")).defaultType }
    val uLongType by lazy { getIrClass(FqName("kotlin.ULong")).defaultType }

    private val consumeAnyIntoVoid = getInternalFunction("consumeAnyIntoVoid")

    private val consumePrimitiveIntoVoid = mapOf(
        irBuiltIns.booleanType to getInternalFunction("consumeBooleanIntoVoid"),
        irBuiltIns.byteType to getInternalFunction("consumeByteIntoVoid"),
        irBuiltIns.shortType to getInternalFunction("consumeShortIntoVoid"),
        irBuiltIns.charType to getInternalFunction("consumeCharIntoVoid"),
        irBuiltIns.intType to getInternalFunction("consumeIntIntoVoid"),
        irBuiltIns.longType to getInternalFunction("consumeLongIntoVoid"),
        irBuiltIns.floatType to getInternalFunction("consumeFloatIntoVoid"),
        irBuiltIns.doubleType to getInternalFunction("consumeDoubleIntoVoid")
    )

    fun findVoidConsumer(type: IrType): IrSimpleFunctionSymbol =
        consumePrimitiveIntoVoid[type] ?: consumeAnyIntoVoid

    private val closureBoxAnyClass = getInternalClass("ClosureBoxAny")

    private val closureBoxClasses = mapOf(
        irBuiltIns.booleanType to getInternalClass("ClosureBoxBoolean"),
        irBuiltIns.byteType to getInternalClass("ClosureBoxByte"),
        irBuiltIns.shortType to getInternalClass("ClosureBoxShort"),
        irBuiltIns.charType to getInternalClass("ClosureBoxChar"),
        irBuiltIns.intType to getInternalClass("ClosureBoxInt"),
        irBuiltIns.longType to getInternalClass("ClosureBoxLong"),
        irBuiltIns.floatType to getInternalClass("ClosureBoxFloat"),
        irBuiltIns.doubleType to getInternalClass("ClosureBoxDouble")
    )

    fun findClosureBoxClass(type: IrType): IrClassSymbol =
        closureBoxClasses[type] ?: closureBoxAnyClass

    val equalityFunctions =
        mapOf(
            irBuiltIns.booleanType to getInternalFunction("wasm_i32_eq"),
            irBuiltIns.byteType to getInternalFunction("wasm_i32_eq"),
            irBuiltIns.shortType to getInternalFunction("wasm_i32_eq"),
            uByteType to getInternalFunction("wasm_i32_eq"),
            uShortType to getInternalFunction("wasm_i32_eq"),
            irBuiltIns.charType to getInternalFunction("wasm_i32_eq"),
            irBuiltIns.intType to getInternalFunction("wasm_i32_eq"),
            uIntType to getInternalFunction("wasm_i32_eq"),
            irBuiltIns.longType to getInternalFunction("wasm_i64_eq"),
            uLongType to getInternalFunction("wasm_i64_eq")
        )

    val floatEqualityFunctions = mapOf(
        irBuiltIns.floatType to getInternalFunction("wasm_f32_eq"),
        irBuiltIns.doubleType to getInternalFunction("wasm_f64_eq")
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
                builtin to getInternalFunction("wasm_${wasmType}_$wasmOp$markSign")
            }
        }.flatten().toMap()
    }

    val booleanAnd = getInternalFunction("wasm_i32_and")
    val refEq = getInternalFunction("wasm_ref_eq")
    val refIsNull = getInternalFunction("wasm_ref_is_null")
    val refTest = getInternalFunction("wasm_ref_test")
    val refCastNull = getInternalFunction("wasm_ref_cast_null")
    val wasmArrayCopy = getInternalFunction("wasm_array_copy")
    val wasmArrayNewData0 = getInternalFunction("array_new_data0")

    val intToLong = getInternalFunction("wasm_i64_extend_i32_s")

    val rangeCheck = getInternalFunction("rangeCheck")

    val getBoxedBoolean: IrSimpleFunctionSymbol = getInternalFunction("getBoxedBoolean")
    val boxBoolean: IrSimpleFunctionSymbol = getInternalFunction("boxBoolean")
    val boxIntrinsic: IrSimpleFunctionSymbol = getInternalFunction("boxIntrinsic")
    val unboxIntrinsic: IrSimpleFunctionSymbol = getInternalFunction("unboxIntrinsic")

    val stringGetLiteral = getFunction("stringLiteral", StandardNames.BUILT_INS_PACKAGE_FQ_NAME)
    val stringGetPoolSize = getInternalFunction("stringGetPoolSize")

    val testFun = maybeGetFunction("test", kotlinTestPackageFqName)
    val suiteFun = maybeGetFunction("suite", kotlinTestPackageFqName)
    val registerRootSuiteBlock = maybeGetFunction("registerRootSuiteBlock", kotlinTestPackageFqName)
    val runRootSuites = maybeGetFunction("runRootSuites", kotlinTestPackageFqName)

    val wasmTypeId = getInternalFunction("wasmTypeId")
    val wasmGetTypeRtti = getInternalFunction("wasmGetTypeRtti")
    val wasmGetRttiSupportedInterfaces = getInternalFunction("wasmGetRttiSupportedInterfaces")
    val wasmGetRttiIntField = getInternalFunction("wasmGetRttiIntField")
    val wasmGetRttiLongField = getInternalFunction("wasmGetRttiLongField")
    val wasmGetRttiSuperClass = getInternalFunction("wasmGetRttiSuperClass")
    val wasmGetObjectRtti = getInternalFunction("wasmGetObjectRtti")
    val wasmArrayAnyIndexOfValue = getInternalFunction("wasmArrayAnyIndexOfValue")

    val wasmIsInterface = getInternalFunction("wasmIsInterface")

    val nullableEquals = getInternalFunction("nullableEquals")
    val anyNtoString = getInternalFunction("anyNtoString")

    val nullableFloatIeee754Equals = getInternalFunction("nullableFloatIeee754Equals")
    val nullableDoubleIeee754Equals = getInternalFunction("nullableDoubleIeee754Equals")

    val returnArgumentIfItIsKotlinAny = getInternalFunction("returnArgumentIfItIsKotlinAny")

    val startCoroutineUninterceptedOrReturnIntrinsics =
        (0..2).map { getInternalFunction("startCoroutineUninterceptedOrReturnIntrinsic$it") }

    // KProperty implementations
    val kLocalDelegatedPropertyImpl: IrClassSymbol = getInternalClass("KLocalDelegatedPropertyImpl")
    val kLocalDelegatedMutablePropertyImpl: IrClassSymbol = getInternalClass("KLocalDelegatedMutablePropertyImpl")
    val kProperty0Impl: IrClassSymbol = getInternalClass("KProperty0Impl")
    val kProperty1Impl: IrClassSymbol = getInternalClass("KProperty1Impl")
    val kProperty2Impl: IrClassSymbol = getInternalClass("KProperty2Impl")
    val kMutableProperty0Impl: IrClassSymbol = getInternalClass("KMutableProperty0Impl")
    val kMutableProperty1Impl: IrClassSymbol = getInternalClass("KMutableProperty1Impl")
    val kMutableProperty2Impl: IrClassSymbol = getInternalClass("KMutableProperty2Impl")

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

    private val getProgressionLastElementSymbols =
        symbolFinder.findFunctions(Name.identifier("getProgressionLastElement"), "kotlin", "internal")

    override val getProgressionLastElementByReturnType: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        getProgressionLastElementSymbols.associateBy { it.owner.returnType.classifierOrFail }
    }

    private val toUIntSymbols = symbolFinder.findFunctions(Name.identifier("toUInt"), "kotlin")

    override val toUIntByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        toUIntSymbols.associateBy {
            it.owner.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classifierOrFail
                ?: error("Expected extension receiver for ${it.owner.render()}")
        }
    }

    private val toULongSymbols = symbolFinder.findFunctions(Name.identifier("toULong"), "kotlin")

    override val toULongByExtensionReceiver: Map<IrClassifierSymbol, IrSimpleFunctionSymbol> by lazy {
        toULongSymbols.associateBy {
            it.owner.parameters.firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }?.type?.classifierOrFail
                ?: error("Expected extension receiver for ${it.owner.render()}")
        }
    }

    private val wasmStructRefClass = getIrClass(FqName("kotlin.wasm.internal.reftypes.structref"))
    val wasmStructRefType by lazy { wasmStructRefClass.defaultType }

    val wasmAnyRefClass = getIrClass(FqName("kotlin.wasm.internal.reftypes.anyref"))

    inner class JsInteropAdapters {
        val kotlinToJsStringAdapter = getInternalFunction("kotlinToJsStringAdapter")
        val kotlinToJsAnyAdapter = getInternalFunction("kotlinToJsAnyAdapter")
        val numberToDoubleAdapter = getInternalFunction("numberToDoubleAdapter")

        val jsCheckIsNullOrUndefinedAdapter = getInternalFunction("jsCheckIsNullOrUndefinedAdapter")

        val jsToKotlinStringAdapter = getInternalFunction("jsToKotlinStringAdapter")
        val jsToKotlinAnyAdapter = getInternalFunction("jsToKotlinAnyAdapter")

        val jsToKotlinByteAdapter = getInternalFunction("jsToKotlinByteAdapter")
        val jsToKotlinShortAdapter = getInternalFunction("jsToKotlinShortAdapter")
        val jsToKotlinCharAdapter = getInternalFunction("jsToKotlinCharAdapter")

        val externRefToKotlinIntAdapter = getInternalFunction("externRefToKotlinIntAdapter")
        val externRefToKotlinBooleanAdapter = getInternalFunction("externRefToKotlinBooleanAdapter")
        val externRefToKotlinLongAdapter = getInternalFunction("externRefToKotlinLongAdapter")
        val externRefToKotlinFloatAdapter = getInternalFunction("externRefToKotlinFloatAdapter")
        val externRefToKotlinDoubleAdapter = getInternalFunction("externRefToKotlinDoubleAdapter")

        val externRefToKotlinUByteAdapter = getInternalFunction("externRefToKotlinUByteAdapter")
        val externRefToKotlinUShortAdapter = getInternalFunction("externRefToKotlinUShortAdapter")
        val externRefToKotlinUIntAdapter = getInternalFunction("externRefToKotlinUIntAdapter")
        val externRefToKotlinULongAdapter = getInternalFunction("externRefToKotlinULongAdapter")

        val kotlinIntToExternRefAdapter = getInternalFunction("kotlinIntToExternRefAdapter")
        val kotlinBooleanToExternRefAdapter = getInternalFunction("kotlinBooleanToExternRefAdapter")
        val kotlinLongToExternRefAdapter = getInternalFunction("kotlinLongToExternRefAdapter")
        val kotlinFloatToExternRefAdapter = getInternalFunction("kotlinFloatToExternRefAdapter")
        val kotlinDoubleToExternRefAdapter = getInternalFunction("kotlinDoubleToExternRefAdapter")
        val kotlinByteToExternRefAdapter = getInternalFunction("kotlinByteToExternRefAdapter")
        val kotlinShortToExternRefAdapter = getInternalFunction("kotlinShortToExternRefAdapter")
        val kotlinCharToExternRefAdapter = getInternalFunction("kotlinCharToExternRefAdapter")

        val kotlinUByteToJsNumber = getInternalFunction("kotlinUByteToJsNumber")
        val kotlinUShortToJsNumber = getInternalFunction("kotlinUShortToJsNumber")
        val kotlinUIntToJsNumber = getInternalFunction("kotlinUIntToJsNumber")
        val kotlinULongToJsBigInt = getInternalFunction("kotlinULongToJsBigInt")
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

        val newJsArray = getInternalFunction("newJsArray")

        val jsArrayPush = getInternalFunction("jsArrayPush")

        val externRefIsNull = getInternalFunction("wasm_externref_is_null")

        val jsPromise = getIrClass(FqName("kotlin.js.Promise"))

        internal val throwAsJsException: IrSimpleFunctionSymbol =
            getInternalFunction("throwAsJsException")

        val kExternalClassImpl: IrClassSymbol = getInternalClass("KExternalClassImpl")

        val jsException = getIrClass(FqName("kotlin.js.JsException"))
        val jsExceptionThrownValue
            get() = jsException.fields.single { it.owner.name == Name.identifier("thrownValue") }.owner

        val createJsException = getInternalFunction("createJsException")
    }

    private val wasmExportClass = getIrClass(FqName("kotlin.wasm.WasmExport"))
    val wasmExportConstructor by lazy { wasmExportClass.constructors.single() }

    private val jsRelatedSymbolsIfNonWasi = if (configuration.wasmTarget == WasmTarget.JS) JsRelatedSymbols() else null

    val jsRelatedSymbols get() = jsRelatedSymbolsIfNonWasi ?: error("Cannot access to js related std in wasi mode")


    private val invokeOnExportedFunctionExitIfWasi =
        when (configuration.wasmTarget == WasmTarget.WASI) {
            true -> getInternalFunction("invokeOnExportedFunctionExit")
            else -> null
        }

    val invokeOnExportedFunctionExit get() = invokeOnExportedFunctionExitIfWasi ?: error("Cannot access to wasi related std in js mode")

    private fun findProperty(memberScope: MemberScope, name: Name): List<PropertyDescriptor> =
        memberScope.getContributedVariables(name, NoLookupLocation.FROM_BACKEND).toList()

    private fun getFunction(name: String, ownerPackage: FqName): IrSimpleFunctionSymbol {
        return maybeGetFunction(name, ownerPackage) ?: throw IllegalArgumentException("Function $name not found")
    }

    private fun maybeGetFunction(name: String, ownerPackage: FqName): IrSimpleFunctionSymbol? {
        return symbolFinder.topLevelFunctions(ownerPackage, name).singleOrNull()
    }

    private fun getInternalFunction(name: String): IrSimpleFunctionSymbol = getFunction(name, wasmInternalFqName)

    private fun getEnumsFunction(name: String) = getFunction(name, enumsInternalPackageFqName)

    private fun getIrClassOrNull(fqName: FqName): IrClassSymbol? = symbolFinder.findClass(fqName.shortName(), fqName.parent())

    private fun getIrClass(fqName: FqName): IrClassSymbol =
        getIrClassOrNull(fqName)
            ?: error("Class \"${fqName.asString()}\" not found! Please make sure that your stdlib version is the same as the compiler.")

    private fun getIrType(fqName: String): IrType = getIrClass(FqName(fqName)).defaultType
    private fun getInternalClassOrNull(name: String): IrClassSymbol? = getIrClassOrNull(wasmInternalFqName.child(Name.identifier(name)))
    private fun getInternalClass(name: String): IrClassSymbol = getIrClass(wasmInternalFqName.child(Name.identifier(name)))
    fun getKFunctionType(type: IrType, list: List<IrType>): IrType {
        return irBuiltIns.functionN(list.size).typeWith(list + type)
    }
}
