/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.util.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
class JsIntrinsics(private val irBuiltIns: IrBuiltIns, val context: JsIrBackendContext) {

    // TODO: Should we drop operator intrinsics in favor of IrDynamicOperatorExpression?

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

    // Bit operations:

    val jsBitAnd = getInternalFunction("jsBitAnd")
    val jsBitOr = getInternalFunction("jsBitOr")
    val jsBitXor = getInternalFunction("jsBitXor")
    val jsBitNot = getInternalFunction("jsBitNot")

    val jsBitShiftR = getInternalFunction("jsBitShiftR")
    val jsBitShiftRU = getInternalFunction("jsBitShiftRU")
    val jsBitShiftL = getInternalFunction("jsBitShiftL")

    // Type checks:

    val jsInstanceOf = getInternalFunction("jsInstanceOfIntrinsic")
    val jsTypeOf = getInternalFunction("jsTypeOf")
    val isExternalObject = getInternalFunction("isExternalObject")

    // Number conversions:

    val jsNumberToByte = getInternalFunction("numberToByte")
    val jsNumberToDouble = getInternalFunction("numberToDouble")
    val jsNumberToInt = getInternalFunction("numberToInt")
    val jsNumberToShort = getInternalFunction("numberToShort")
    val jsNumberToLong = getInternalFunction("numberToLong")
    val jsNumberToChar = getInternalFunction("numberToChar")
    val jsToByte = getInternalFunction("toByte")
    val jsToShort = getInternalFunction("toShort")
    val jsToLong = getInternalFunction("toLong")


    // RTTI:
    val implementSymbol = getInternalFunction("implement")
    val initMetadataForSymbol = getInternalFunction("initMetadataFor")
    val initMetadataForClassSymbol = getInternalFunction("initMetadataForClass")
    val initMetadataForObjectSymbol = getInternalFunction("initMetadataForObject")
    val initMetadataForInterfaceSymbol = getInternalFunction("initMetadataForInterface")
    val initMetadataForLambdaSymbol = getInternalFunction("initMetadataForLambda")
    val initMetadataForCoroutineSymbol = getInternalFunction("initMetadataForCoroutine")
    val initMetadataForFunctionReferenceSymbol = getInternalFunction("initMetadataForFunctionReference")
    val initMetadataForCompanionSymbol = getInternalFunction("initMetadataForCompanion")

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
    val jsToString = getInternalFunction("toString")
    val jsAnyToString = getInternalFunction("anyToString")
    val jsCompareTo = getInternalFunction("compareTo")
    val jsEquals = getInternalFunction("equals")
    val jsNewTarget = getInternalFunction("jsNewTarget")
    val jsEmptyObject = getInternalFunction("emptyObject")
    val jsOpenInitializerBox = getInternalFunction("openInitializerBox")

    val jsImul = getInternalFunction("imul")

    val jsUnreachableDeclarationLog = getInternalFunction("unreachableDeclarationLog")
    val jsUnreachableDeclarationException = getInternalFunction("unreachableDeclarationException")

    val jsNativeBoolean = getInternalFunction("nativeBoolean")
    val jsBooleanInExternalLog = getInternalFunction("booleanInExternalLog")
    val jsBooleanInExternalException = getInternalFunction("booleanInExternalException")

    val jsNewAnonymousClass = getInternalFunction("jsNewAnonymousClass")

    // Coroutines

    val jsCoroutineContext
        get() = context.ir.symbols.coroutineContextGetter

    val jsYieldFunctionSymbol = getInternalFunction("jsYield")

    val jsGetContinuation = getInternalFunction("getContinuation")
    val jsInvokeSuspendSuperType =
        getInternalWithoutPackage("kotlin.coroutines.intrinsics.invokeSuspendSuperType")
    val jsInvokeSuspendSuperTypeWithReceiver =
        getInternalWithoutPackage("kotlin.coroutines.intrinsics.invokeSuspendSuperTypeWithReceiver")
    val jsInvokeSuspendSuperTypeWithReceiverAndParam =
        getInternalWithoutPackage("kotlin.coroutines.intrinsics.invokeSuspendSuperTypeWithReceiverAndParam")

    val createCoroutineUnintercepted =
        getManyInternalWithoutPackage("kotlin.coroutines.intrinsics.createCoroutineUnintercepted")
    val startCoroutineUninterceptedOrReturn =
        getManyInternalWithoutPackage("kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn")

    val createCoroutineUninterceptedGeneratorVersion =
        getManyInternalWithoutPackage("kotlin.coroutines.intrinsics.createCoroutineUninterceptedGeneratorVersion")
    val startCoroutineUninterceptedOrReturnGeneratorVersion =
        getManyInternalWithoutPackage("kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturnGeneratorVersion")

    val startCoroutineUninterceptedOrReturnGeneratorVersion1 by context.lazy2 {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.valueParameters.size == 1 }
    }
    val startCoroutineUninterceptedOrReturnGeneratorVersion2 by context.lazy2 {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.valueParameters.size == 2 }
    }

    val suspendOrReturnFunctionSymbol = getInternalWithoutPackage("kotlin.coroutines.intrinsics.suspendOrReturn")

    val jsNumberRangeToNumber = getInternalFunction("numberRangeToNumber")
    val jsNumberRangeToLong = getInternalFunction("numberRangeToLong")

    private val _rangeUntilFunctions = irBuiltIns.findFunctions(Name.identifier("until"), "kotlin", "ranges")
    val rangeUntilFunctions by lazy(LazyThreadSafetyMode.NONE) {
        _rangeUntilFunctions
            .filter { it.owner.extensionReceiverParameter != null && it.owner.valueParameters.size == 1 }
            .associateBy { it.owner.extensionReceiverParameter!!.type to it.owner.valueParameters[0].type }
    }

    val longClassSymbol = getInternalClassWithoutPackage("kotlin.Long")

    val promiseClassSymbol: IrClassSymbol by context.lazy2 {
        getInternalClassWithoutPackage("kotlin.js.Promise")
    }

    val longToDouble = context.symbolTable.descriptorExtension.referenceSimpleFunction(
        context.getClass(FqName("kotlin.Long")).unsubstitutedMemberScope.findSingleFunction(
            Name.identifier("toDouble")
        )
    )
    val longToFloat = context.symbolTable.descriptorExtension.referenceSimpleFunction(
        context.getClass(FqName("kotlin.Long")).unsubstitutedMemberScope.findSingleFunction(
            Name.identifier("toFloat")
        )
    )

    val longCompareToLong: IrSimpleFunction = longClassSymbol.owner.findDeclaration<IrSimpleFunction> {
        it.name == Name.identifier("compareTo") && it.valueParameters[0].type.isLong()
    }!!

    val charClassSymbol = getInternalClassWithoutPackage("kotlin.Char")

    val stringClassSymbol = getInternalClassWithoutPackage("kotlin.String")
    val stringConstructorSymbol = stringClassSymbol.constructors.single()

    val anyClassSymbol = getInternalClassWithoutPackage("kotlin.Any")
    val anyConstructorSymbol = anyClassSymbol.constructors.single()

    val jsObjectClassSymbol = getInternalClassWithoutPackage("kotlin.js.JsObject")
    val jsObjectConstructorSymbol by context.lazy2 { jsObjectClassSymbol.constructors.single() }

    val uByteClassSymbol by context.lazy2 { getInternalClassWithoutPackage("kotlin.UByte") }
    val uShortClassSymbol by context.lazy2 { getInternalClassWithoutPackage("kotlin.UShort") }
    val uIntClassSymbol by context.lazy2 { getInternalClassWithoutPackage("kotlin.UInt") }
    val uLongClassSymbol by context.lazy2 { getInternalClassWithoutPackage("kotlin.ULong") }

    val unreachable = getInternalFunction("unreachable")

    val jsArguments = getInternalFunction("jsArguments")

    val returnIfSuspended = getInternalFunction("returnIfSuspended")
    val getContinuation = getInternalFunction("getContinuation")

    val jsEnsureNonNull = getFunctionInKotlinPackage("ensureNotNull")

    // Arrays:
    val array get() = irBuiltIns.arrayClass

    val primitiveArrays get() = irBuiltIns.primitiveArraysToPrimitiveTypes

    val jsArray = getInternalFunction("arrayWithFun")
    val jsFillArray = getInternalFunction("fillArrayFun")

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
        override val createKType = getInternalWithoutPackageOrNull("createKType")
        override val createDynamicKType = getInternalWithoutPackageOrNull("createDynamicKType")
        override val createKTypeParameter = getInternalWithoutPackageOrNull("createKTypeParameter")
        override val getStarKTypeProjection = getInternalWithoutPackageOrNull("getStarKTypeProjection")
        override val createCovariantKTypeProjection = getInternalWithoutPackageOrNull("createCovariantKTypeProjection")
        override val createInvariantKTypeProjection = getInternalWithoutPackageOrNull("createInvariantKTypeProjection")
        override val createContravariantKTypeProjection = getInternalWithoutPackageOrNull("createContravariantKTypeProjection")
        override val getKClass = getInternalWithoutPackage("getKClass")
        override val getKClassFromExpression = getInternalWithoutPackage("getKClassFromExpression")
        override val kTypeClass: IrClassSymbol = context.getIrClass(FqName("kotlin.reflect.KType"))
    }

    val primitiveClassesObject = context.getIrClass(FqName("kotlin.reflect.js.internal.PrimitiveClasses"))

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

    val arrayConcat = getInternalWithoutPackage("arrayConcat")

    val primitiveArrayConcat = getInternalWithoutPackage("primitiveArrayConcat")
    val taggedArrayCopy = getInternalWithoutPackage("taggedArrayCopy")

    val jsArraySlice = getInternalFunction("slice")

    val jsCall = getInternalFunction("jsCall")
    val jsBind = getInternalFunction("jsBind")

    // TODO move to IntrinsifyCallsLowering
    val doNotIntrinsifyAnnotationSymbol =
        context.symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("DoNotIntrinsify"))
    val jsFunAnnotationSymbol = context.symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("JsFun"))
    val jsNameAnnotationSymbol = context.symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("JsName"))
    val jsExportAnnotationSymbol = context.symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("JsExport"))
    val jsGeneratorAnnotationSymbol = context.symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("JsGenerator"))

    val jsExportIgnoreAnnotationSymbol by context.lazy2 {
        jsExportAnnotationSymbol.owner
            .findDeclaration<IrClass> { it.fqNameWhenAvailable == FqName("kotlin.js.JsExport.Ignore") }
            ?.symbol ?: error("can't find kotlin.js.JsExport.Ignore annotation")
    }

    val jsImplicitExportAnnotationSymbol =
        context.symbolTable.descriptorExtension.referenceClass(context.getJsInternalClass("JsImplicitExport"))

    // TODO move CharSequence-related stiff to IntrinsifyCallsLowering
    val charSequenceClassSymbol = context.symbolTable.descriptorExtension.referenceClass(context.getClass(FqName("kotlin.CharSequence")))
    val charSequenceLengthPropertyGetterSymbol by context.lazy2 {
        with(charSequenceClassSymbol.owner.declarations) {
            filterIsInstance<IrProperty>().firstOrNull { it.name.asString() == "length" }?.getter ?:
            filterIsInstance<IrFunction>().first { it.name.asString() == "<get-length>" }
        }.symbol
    }
    val charSequenceGetFunctionSymbol by context.lazy2 {
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "get" }.symbol
    }
    val charSequenceSubSequenceFunctionSymbol by context.lazy2 {
        charSequenceClassSymbol.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "subSequence" }.symbol
    }


    val jsCharSequenceGet = getInternalFunction("charSequenceGet")
    val jsCharSequenceLength = getInternalFunction("charSequenceLength")
    val jsCharSequenceSubSequence = getInternalFunction("charSequenceSubSequence")

    val jsContexfulRef = getInternalFunction("jsContextfulRef")
    val jsBoxIntrinsic = getInternalFunction("boxIntrinsic")
    val jsUnboxIntrinsic = getInternalFunction("unboxIntrinsic")

    val captureStack = getInternalFunction("captureStack")

    val createSharedBox = getInternalFunction("sharedBoxCreate")
    val readSharedBox = getInternalFunction("sharedBoxRead")
    val writeSharedBox = getInternalFunction("sharedBoxWrite")

    val linkageErrorSymbol = getInternalFunction("throwLinkageError")

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
    private fun getInternalFunction(name: String) =
        context.symbolTable.descriptorExtension.referenceSimpleFunction(context.getJsInternalFunction(name))

    private fun getInternalCollectionFunction(name: String) =
        context.symbolTable.descriptorExtension.referenceSimpleFunction(context.getJsInternalCollectionFunction(name))

    private fun getInternalProperty(name: String) =
        context.symbolTable.descriptorExtension.referenceProperty(context.getJsInternalProperty(name))

    private fun getInternalWithoutPackage(name: String) =
        context.symbolTable.descriptorExtension.referenceSimpleFunction(context.getFunctions(FqName(name)).single())

    private fun getManyInternalWithoutPackage(name: String) =
        context.getFunctions(FqName(name)).mapTo(mutableSetOf()) { context.symbolTable.descriptorExtension.referenceSimpleFunction(it) }

    private fun getInternalWithoutPackageOrNull(name: String): IrSimpleFunctionSymbol? {
        val descriptor = context.getFunctions(FqName(name)).singleOrNull() ?: return null
        return context.symbolTable.descriptorExtension.referenceSimpleFunction(descriptor)
    }

    private fun getFunctionInEnumPackage(name: String) =
        context.symbolTable.descriptorExtension.referenceSimpleFunction(
            context.getFunctions(kotlinEnumsPackageFqn.child(Name.identifier(name))).single()
        )

    private fun getFunctionInKotlinPackage(name: String) =
        context.symbolTable.descriptorExtension.referenceSimpleFunction(
            context.getFunctions(kotlinPackageFqn.child(Name.identifier(name))).single()
        )

    private fun getInternalClassWithoutPackage(fqName: String) =
        context.symbolTable.descriptorExtension.referenceClass(context.getClass(FqName(fqName)))
}
