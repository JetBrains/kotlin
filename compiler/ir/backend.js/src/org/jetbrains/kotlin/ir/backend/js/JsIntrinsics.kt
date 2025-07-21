/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.config.compileLongAsBigint
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class, InternalSymbolFinderAPI::class)
class JsIntrinsics(private val irBuiltIns: IrBuiltIns, private val configuration: CompilerConfiguration) {
    // TODO: Should we drop operator intrinsics in favor of IrDynamicOperatorExpression?
    val symbolFinder = irBuiltIns.symbolFinder

    // Modes
    val jsIsEs6 = getInternalFunction("jsIsEs6")

    // Global variables
    val void = getInternalProperty("VOID")
    val globalThis = getInternalProperty("globalThis")

    // Equality operations:

    val jsEqeq = getInternalFunction("jsEqeq")
    val jsNotEq = getInternalFunction("jsNotEq")
    val jsEqeqeq = getInternalFunction("jsEqeqeq")
    val jsNotEqeq = getInternalFunction("jsNotEqeq")

    val jsGt = getInternalFunction("jsGt")
    val jsGtEq = getInternalFunction("jsGtEq")
    val jsLt = getInternalFunction("jsLt")
    val jsLtEq = getInternalFunction("jsLtEq")


    // Unary operations:

    val jsNot = getInternalFunction("jsNot")

    val jsUnaryPlus = getInternalFunction("jsUnaryPlus")
    val jsUnaryMinus = getInternalFunction("jsUnaryMinus")

    val jsPrefixInc = getInternalFunction("jsPrefixInc")
    val jsPostfixInc = getInternalFunction("jsPostfixInc")
    val jsPrefixDec = getInternalFunction("jsPrefixDec")
    val jsPostfixDec = getInternalFunction("jsPostfixDec")

    val jsDelete = getInternalFunction("jsDelete")

    val longUnaryMinus = getLongHelper("negate")!!

    // Binary operations:

    val jsPlus = getInternalFunction("jsPlus")
    val jsMinus = getInternalFunction("jsMinus")
    val jsMult = getInternalFunction("jsMult")
    val jsDiv = getInternalFunction("jsDiv")
    val jsMod = getInternalFunction("jsMod")

    val jsPlusAssign = getInternalFunction("jsPlusAssign")
    val jsMinusAssign = getInternalFunction("jsMinusAssign")
    val jsMultAssign = getInternalFunction("jsMultAssign")
    val jsDivAssign = getInternalFunction("jsDivAssign")
    val jsModAssign = getInternalFunction("jsModAssign")

    val jsAnd = getInternalFunction("jsAnd")
    val jsOr = getInternalFunction("jsOr")

    val jsIn = getInternalFunction("jsInIntrinsic")

    val longAdd = getLongHelper("add")!!
    val longSubtract = getLongHelper("subtract")!!
    val longMultiply = getLongHelper("multiply")!!
    val longDivide = getLongHelper("divide")!!
    val longModulo = getLongHelper("modulo")!!

    // Bit operations:

    val jsBitAnd = getInternalFunction("jsBitAnd")
    val jsBitOr = getInternalFunction("jsBitOr")
    val jsBitXor = getInternalFunction("jsBitXor")
    val jsBitNot = getInternalFunction("jsBitNot")

    val jsBitShiftR = getInternalFunction("jsBitShiftR")
    val jsBitShiftRU = getInternalFunction("jsBitShiftRU")
    val jsBitShiftL = getInternalFunction("jsBitShiftL")

    val longAnd = getLongHelper("bitwiseAnd")
    val longOr = getLongHelper("bitwiseOr")
    val longXor = getLongHelper("bitwiseXor")
    val longInv = getLongHelper("invert")
    val longShiftLeft = getLongHelper("shiftLeft")!!
    val longShiftRight = getLongHelper("shiftRight")!!
    val longShiftRightUnsigned = getLongHelper("shiftRightUnsigned")!!

    // Type checks:

    val jsInstanceOf = getInternalFunction("jsInstanceOfIntrinsic")
    val jsTypeOf = getInternalFunction("jsTypeOf")
    val isExternalObject = getInternalFunction("isExternalObject")

    // Number conversions:

    val jsNumberToByte = getInternalFunction("numberToByte")
    val jsNumberToDouble = getInternalFunction("numberToDouble")
    val jsNumberToInt = getInternalFunction("numberToInt")
    val jsNumberToShort = getInternalFunction("numberToShort")
    val jsNumberToLong = getLongHelper("numberToLong")!!
    val jsNumberToChar = getInternalFunction("numberToChar")
    val jsToByte = getInternalFunction("toByte")
    val jsToShort = getInternalFunction("toShort")

    val longFromInt = getLongHelper("fromInt")!!

    val longToByte = getLongHelper("convertToByte")!!
    val longToNumber = getLongHelper("toNumber")!!
    val longToShort = getLongHelper("convertToShort")!!
    val longToInt = getLongHelper("convertToInt")!!
    val longToChar = getLongHelper("convertToChar")!!

    val longFromTwoInts = getLongHelper("longFromTwoInts")
    val longLowBits = getLongHelper("lowBits")
    val longHighBits = getLongHelper("highBits")

    // RTTI:
    enum class RuntimeMetadataKind(val namePart: String, val isSpecial: Boolean = false) {
        CLASS("Class"),
        OBJECT("Object"),
        INTERFACE("Interface"),
        LAMBDA("Lambda", isSpecial = true),
        COROUTINE("Coroutine", isSpecial = true),
        FUNCTION_REFERENCE("FunctionReference", isSpecial = true),
        COMPANION_OBJECT("Companion", isSpecial = true)
    }

    private val initMetadataSymbols: Map<RuntimeMetadataKind, IrSimpleFunctionSymbol> = buildMap {
        for (kind in RuntimeMetadataKind.entries) {
            put(kind, getInternalFunction("initMetadataFor${kind.namePart}"))
        }
    }

    fun getInitMetadataSymbol(kind: RuntimeMetadataKind): IrSimpleFunctionSymbol? =
        initMetadataSymbols[kind]

    val makeAssociatedObjectMapES5 = getInternalInRootPackage("makeAssociatedObjectMapES5")!!
    val getAssociatedObjectId = getInternalInRootPackage("getAssociatedObjectId")!!
    val nextAssociatedObjectId = getInternalFunction("nextAssociatedObjectId")

    val isInterfaceSymbol = getInternalFunction("isInterface")
    val isArraySymbol = getInternalFunction("isArray")
    //    val isCharSymbol = getInternalFunction("isChar")
    val isSuspendFunctionSymbol = getInternalFunction("isSuspendFunction")

    val isNumberSymbol = getInternalFunction("isNumber")
    val isComparableSymbol = getInternalFunction("isComparable")
    val isCharSequenceSymbol = getInternalFunction("isCharSequence")

    val isPrimitiveArray = mapOf(
        PrimitiveType.BOOLEAN to getInternalFunction("isBooleanArray"),
        PrimitiveType.BYTE to getInternalFunction("isByteArray"),
        PrimitiveType.SHORT to getInternalFunction("isShortArray"),
        PrimitiveType.CHAR to getInternalFunction("isCharArray"),
        PrimitiveType.INT to getInternalFunction("isIntArray"),
        PrimitiveType.FLOAT to getInternalFunction("isFloatArray"),
        PrimitiveType.LONG to getInternalFunction("isLongArray"),
        PrimitiveType.DOUBLE to getInternalFunction("isDoubleArray")
    )


    // Enum

    val enumValueOfIntrinsic = getInternalFunction("enumValueOfIntrinsic")
    val enumValuesIntrinsic = getInternalFunction("enumValuesIntrinsic")
    val enumEntriesIntrinsic = getFunctionInEnumPackage("enumEntriesIntrinsic")


    // Other:

    val jsCode = getInternalFunction("js") // js("<code>")
    val jsHashCode = getInternalFunction("hashCode")
    val jsGetBooleanHashCode = getInternalFunction("getBooleanHashCode")
    val jsGetNumberHashCode = getInternalFunction("getNumberHashCode")
    val jsGetObjectHashCode = getInternalFunction("getObjectHashCode")
    val jsGetStringHashCode = getInternalFunction("getStringHashCode")
    val jsBigIntHashCode = getInternalFunction("getBigIntHashCode")
    val jsToString = getInternalFunction("toString")
    val jsAnyToString = getInternalFunction("anyToString")
    val jsCompareTo = getInternalFunction("compareTo")
    val jsEquals = getInternalFunction("equals")
    val jsNewTarget = getInternalFunction("jsNewTarget")
    val jsEmptyObject = getInternalFunction("emptyObject")
    val jsOpenInitializerBox = getInternalFunction("openInitializerBox")

    val longEquals = getLongHelper("equalsLong")

    val jsImul = getInternalFunction("imul")

    val jsUnreachableDeclarationLog = getInternalFunction("unreachableDeclarationLog")
    val jsUnreachableDeclarationException = getInternalFunction("unreachableDeclarationException")

    val jsNativeBoolean = getInternalFunction("nativeBoolean")
    val jsBooleanInExternalLog = getInternalFunction("booleanInExternalLog")
    val jsBooleanInExternalException = getInternalFunction("booleanInExternalException")

    val jsNewAnonymousClass = getInternalFunction("jsNewAnonymousClass")

    val longBoxedOne = symbolFinder
        .findProperties(Name.identifier("ONE"), JsStandardClassIds.BOXED_LONG_PACKAGE).single()

    // Coroutines

    val jsYieldFunctionSymbol = getInternalFunction("jsYield")

    val jsInvokeSuspendSuperType: IrSimpleFunctionSymbol =
        getCoroutineIntrinsic("invokeSuspendSuperType").single()
    val jsInvokeSuspendSuperTypeWithReceiver: IrSimpleFunctionSymbol =
        getCoroutineIntrinsic("invokeSuspendSuperTypeWithReceiver").single()
    val jsInvokeSuspendSuperTypeWithReceiverAndParam: IrSimpleFunctionSymbol =
        getCoroutineIntrinsic("invokeSuspendSuperTypeWithReceiverAndParam").single()

    val createCoroutineUnintercepted: Set<IrSimpleFunctionSymbol> =
        getCoroutineIntrinsic("createCoroutineUnintercepted").toHashSet()
    val startCoroutineUninterceptedOrReturnNonGeneratorVersion: Set<IrSimpleFunctionSymbol> =
        getCoroutineIntrinsic("startCoroutineUninterceptedOrReturnNonGeneratorVersion").toHashSet()

    val createCoroutineUninterceptedGeneratorVersion: Set<IrSimpleFunctionSymbol> =
        getCoroutineIntrinsic("createCoroutineUninterceptedGeneratorVersion").toHashSet()
    val startCoroutineUninterceptedOrReturnGeneratorVersion: Set<IrSimpleFunctionSymbol> =
        getCoroutineIntrinsic("startCoroutineUninterceptedOrReturnGeneratorVersion").toHashSet()

    val startCoroutineUninterceptedOrReturnGeneratorVersion1 by lazy(LazyThreadSafetyMode.NONE) {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.hasShape(extensionReceiver = true, regularParameters = 1) }
    }
    val startCoroutineUninterceptedOrReturnGeneratorVersion2 by lazy(LazyThreadSafetyMode.NONE) {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.hasShape(extensionReceiver = true, regularParameters = 2) }
    }

    val awaitFunctionSymbol = getCoroutineIntrinsic("await").single()
    val promisifyFunctionSymbol = getCoroutineIntrinsic("promisify").single()
    val suspendOrReturnFunctionSymbol: IrSimpleFunctionSymbol = getCoroutineIntrinsic("suspendOrReturn").single()

    val jsNumberRangeToNumber = getInternalFunction("numberRangeToNumber")
    val jsNumberRangeToLong = getInternalFunction("numberRangeToLong")
    val jsLongRangeToNumber = getInternalFunction("longRangeToNumber")
    val jsLongRangeToLong = getInternalFunction("longRangeToLong")

    private val _rangeUntilFunctions = symbolFinder.findFunctions(Name.identifier("until"), "kotlin", "ranges")
    val rangeUntilFunctions: Map<Pair<IrType, IrType>, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        _rangeUntilFunctions
            .filter { it.owner.hasShape(extensionReceiver = true, regularParameters = 1) }
            .associateBy { it.owner.parameters[0].type to it.owner.parameters[1].type }
    }

    val longClassSymbol = irBuiltIns.longClass

    val promiseClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) {
        symbolFinder.topLevelClass(JsStandardClassIds.Promise)
    }

    val longCompareToLong: IrSimpleFunctionSymbol? = getLongHelper("compare")

    val jsLongToString: IrSimpleFunctionSymbol = getInternalFunction("jsLongToString")
    val longToStringImpl: IrSimpleFunctionSymbol = getLongHelper("toStringImpl")!!

    val charClassSymbol = irBuiltIns.charClass

    val stringClassSymbol = irBuiltIns.stringClass
    val stringConstructorSymbol = stringClassSymbol.constructors.single()

    val anyClassSymbol = irBuiltIns.anyClass
    val anyConstructorSymbol = anyClassSymbol.constructors.single()

    val jsObjectClassSymbol = symbolFinder.topLevelClass(JsStandardClassIds.JsObject)
    val jsObjectConstructorSymbol by lazy(LazyThreadSafetyMode.NONE) { jsObjectClassSymbol.constructors.single() }

    val uByteClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { symbolFinder.topLevelClass(StandardClassIds.UByte) }
    val uShortClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { symbolFinder.topLevelClass(StandardClassIds.UShort) }
    val uIntClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { symbolFinder.topLevelClass(StandardClassIds.UInt) }
    val uLongClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { symbolFinder.topLevelClass(StandardClassIds.ULong) }

    val unreachable = getInternalFunction("unreachable")

    val jsArguments = getInternalFunction("jsArguments")

    val jsEnsureNonNull = getFunctionInKotlinPackage("ensureNotNull")

    // Arrays:
    val array get() = irBuiltIns.arrayClass

    val primitiveArrays get() = irBuiltIns.primitiveArraysToPrimitiveTypes

    val jsArrayLength = getInternalFunction("jsArrayLength")
    val jsArrayGet = getInternalFunction("jsArrayGet")
    val jsArraySet = getInternalFunction("jsArraySet")

    val jsArrayIteratorFunction = getInternalFunction("arrayIterator")

    val jsPrimitiveArrayIteratorFunctions =
        PrimitiveType.entries.associate { it to getInternalFunction("${it.typeName.asString().toLowerCaseAsciiOnly()}ArrayIterator") }

    val jsClass = getInternalFunction("jsClassIntrinsic")
    val arrayLiteral: IrSimpleFunctionSymbol = getInternalFunction("arrayLiteral")

    // The following 3 functions are all lowered into [].slice.call(...), they only differ
    // in the number of arguments.
    // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
    val jsArrayLike2Array = getInternalFunction("jsArrayLike2Array")
    val jsSliceArrayLikeFromIndex = getInternalFunction("jsSliceArrayLikeFromIndex")
    val jsSliceArrayLikeFromIndexToIndex = getInternalFunction("jsSliceArrayLikeFromIndexToIndex")

    internal inner class JsReflectionSymbols : ReflectionSymbols {
        override val createKType: IrSimpleFunctionSymbol = getInternalReflectionFunction("createKType")
        override val createDynamicKType: IrSimpleFunctionSymbol = getInternalReflectionFunction("createDynamicKType")
        override val createKTypeParameter: IrSimpleFunctionSymbol = getInternalReflectionFunction("createKTypeParameter")
        override val getStarKTypeProjection: IrSimpleFunctionSymbol = getInternalReflectionFunction("getStarKTypeProjection")
        override val createCovariantKTypeProjection: IrSimpleFunctionSymbol =
            getInternalReflectionFunction("createCovariantKTypeProjection")
        override val createInvariantKTypeProjection: IrSimpleFunctionSymbol =
            getInternalReflectionFunction("createInvariantKTypeProjection")
        override val createContravariantKTypeProjection: IrSimpleFunctionSymbol =
            getInternalReflectionFunction("createContravariantKTypeProjection")
        override val getKClass: IrSimpleFunctionSymbol = getInternalReflectionFunction("getKClass")
        override val getKClassFromExpression: IrSimpleFunctionSymbol = getInternalReflectionFunction("getKClassFromExpression")
        override val kTypeClass: IrClassSymbol =
            symbolFinder.findClass(StandardClassIds.KType.shortClassName, StandardClassIds.KType.packageFqName)!!
    }

    val primitiveClassesObject: IrClassSymbol =
        symbolFinder.topLevelClass(JsStandardClassIds.BASE_REFLECT_JS_INTERNAL_PACKAGE, "PrimitiveClasses")

    internal val reflectionSymbols: JsReflectionSymbols = JsReflectionSymbols()

    val primitiveToTypedArrayMap = EnumMap(
        mapOf(
            PrimitiveType.BYTE to "Int8",
            PrimitiveType.SHORT to "Int16",
            PrimitiveType.INT to "Int32",
            PrimitiveType.FLOAT to "Float32",
            PrimitiveType.DOUBLE to "Float64"
        )
    )

    val primitiveToSizeConstructor =
        PrimitiveType.entries.associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                getInternalFunction("${it.toLowerCaseAsciiOnly()}Array")
            } ?: getInternalFunction("${type.typeName.asString().toLowerCaseAsciiOnly()}Array"))
        }

    val primitiveToLiteralConstructor =
        PrimitiveType.entries.associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                getInternalFunction("${it.toLowerCaseAsciiOnly()}ArrayOf")
            } ?: getInternalFunction("${type.typeName.asString().toLowerCaseAsciiOnly()}ArrayOf"))
        }

    val arrayConcat: IrSimpleFunctionSymbol = getInternalInRootPackage("arrayConcat")!!

    val primitiveArrayConcat: IrSimpleFunctionSymbol = getInternalInRootPackage("primitiveArrayConcat")!!
    val taggedArrayCopy: IrSimpleFunctionSymbol = getInternalInRootPackage("taggedArrayCopy")!!

    val jsArraySlice = getInternalFunction("slice")

    val jsCall = getInternalFunction("jsCall")
    val jsBind = getInternalFunction("jsBind")

    val jsOutlinedFunctionAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(JsStandardClassIds.Annotations.JsOutlinedFunction)
    val jsNameAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(JsStandardClassIds.Annotations.JsName)
    val jsExportAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(JsStandardClassIds.Annotations.JsExport)
    val jsGeneratorAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(JsStandardClassIds.Annotations.JsGenerator)

    val jsExportIgnoreAnnotationSymbol by lazy(LazyThreadSafetyMode.NONE) {
        jsExportAnnotationSymbol.owner
            .findDeclaration<IrClass> { it.fqNameWhenAvailable == FqName("kotlin.js.JsExport.Ignore") }
            ?.symbol ?: irError("can't find kotlin.js.JsExport.Ignore annotation") {
            withIrEntry("jsExportAnnotationSymbol.owner", jsExportAnnotationSymbol.owner)
        }
    }

    val jsImplicitExportAnnotationSymbol: IrClassSymbol = symbolFinder.topLevelClass(JsStandardClassIds.Annotations.JsImplicitExport)

    val charSequenceClassSymbol = symbolFinder.topLevelClass(StandardClassIds.CharSequence)
    val charSequenceLengthPropertyGetterSymbol by lazy(LazyThreadSafetyMode.NONE) {
        with(charSequenceClassSymbol.owner.declarations) {
            filterIsInstance<IrProperty>().firstOrNull { it.name.asString() == "length" }?.getter
                ?: filterIsInstance<IrFunction>().first { it.name.asString() == "<get-length>" }
        }.symbol
    }
    val charSequenceGetFunctionSymbol by lazy(LazyThreadSafetyMode.NONE) {
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "get" }.symbol
    }
    val charSequenceSubSequenceFunctionSymbol by lazy(LazyThreadSafetyMode.NONE) {
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "subSequence" }.symbol
    }


    val jsCharSequenceGet = getInternalFunction("charSequenceGet")
    val jsCharCodeAt = getInternalFunction("charCodeAt")
    val jsCharSequenceLength = getInternalFunction("charSequenceLength")
    val jsCharSequenceSubSequence = getInternalFunction("charSequenceSubSequence")

    val jsContexfulRef = getInternalFunction("jsContextfulRef")
    val jsBoxIntrinsic = getInternalFunction("boxIntrinsic")
    val jsUnboxIntrinsic = getInternalFunction("unboxIntrinsic")

    val captureStack = getInternalFunction("captureStack")

    val linkageErrorSymbol = symbolFinder
        .findFunctions(Name.identifier("throwIrLinkageError"), StandardClassIds.BASE_INTERNAL_PACKAGE)
        .single()

    val jsPrototypeOfSymbol = getInternalFunction("protoOf")
    val jsDefinePropertySymbol = getInternalFunction("defineProp")
    val jsObjectCreateSymbol = getInternalFunction("objectCreate")                 // Object.create(x)
    val jsCreateThisSymbol = getInternalFunction("createThis")                     // Object.create(x.prototype)
    val jsBoxApplySymbol = getInternalFunction("boxApply")
    val jsCreateExternalThisSymbol = getInternalFunction("createExternalThis")

    // Collections interop:
    val jsCreateListFrom = getInternalCollectionFunction("createListFrom")
    val jsCreateMutableListFrom = getInternalCollectionFunction("createMutableListFrom")
    val jsCreateSetFrom = getInternalCollectionFunction("createSetFrom")
    val jsCreateMutableSetFrom = getInternalCollectionFunction("createMutableSetFrom")
    val jsCreateMapFrom = getInternalCollectionFunction("createMapFrom")
    val jsCreateMutableMapFrom = getInternalCollectionFunction("createMutableMapFrom")

    // Helpers:
    private fun getLongHelper(name: String): IrSimpleFunctionSymbol? {
        val packageName = if (configuration.compileLongAsBigint) {
            JsStandardClassIds.LONG_AS_BIGINT_PACKAGE
        } else {
            JsStandardClassIds.BOXED_LONG_PACKAGE
        }
        return symbolFinder.findFunctions(Name.identifier(name), packageName).singleOrNull()
    }

    private fun getInternalFunction(name: String): IrSimpleFunctionSymbol =
        symbolFinder.findFunctions(Name.identifier(name), JsStandardClassIds.BASE_JS_PACKAGE).single()

    private fun getInternalReflectionFunction(name: String): IrSimpleFunctionSymbol =
        symbolFinder.findFunctions(Name.identifier(name), JsStandardClassIds.BASE_REFLECT_JS_INTERNAL_PACKAGE).single()

    private fun getInternalCollectionFunction(name: String): IrSimpleFunctionSymbol =
        symbolFinder.findFunctions(Name.identifier(name), StandardClassIds.BASE_COLLECTIONS_PACKAGE).single()

    private fun getInternalProperty(name: String): IrPropertySymbol =
        symbolFinder.findProperties(Name.identifier(name), JsStandardClassIds.BASE_JS_PACKAGE).single()

    private fun getInternalInRootPackage(name: String): IrSimpleFunctionSymbol? =
        symbolFinder.findFunctions(Name.identifier(name), FqName.ROOT).singleOrNull()

    private fun getCoroutineIntrinsic(name: String): Iterable<IrSimpleFunctionSymbol> =
        symbolFinder.findFunctions(Name.identifier(name), StandardClassIds.BASE_COROUTINES_INTRINSICS_PACKAGE)

    // JS stdlib compilation needs `.single { !it.isBound || !it.owner.isExpect }`:
    //   - Expect declarations like `fun <T : Enum<T>> enumEntriesIntrinsic(): EnumEntries<T>` are removed from IR by Actualizer
    //   - However, they are not removed from FIR.
    //   - Intrinsics are needed for JsOutlineLowering, so they are initialized during pre-serialization
    //   - SymbolFinder finds in FIR both expect and actual for `enumEntriesIntrinsic`, and only actual one must remain
    private fun getFunctionInEnumPackage(name: String): IrSimpleFunctionSymbol =
        symbolFinder.findFunctions(Name.identifier(name), StandardClassIds.BASE_ENUMS_PACKAGE)
            .single { !it.isBound || !it.owner.isExpect }

    private fun getFunctionInKotlinPackage(name: String): IrSimpleFunctionSymbol =
        symbolFinder.findFunctions(Name.identifier(name), StandardClassIds.BASE_KOTLIN_PACKAGE).single()
}
