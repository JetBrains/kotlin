/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.PreSerializationJsSymbols
import org.jetbrains.kotlin.backend.common.ir.PreSerializationWebSymbols
import org.jetbrains.kotlin.backend.common.ir.KlibSymbols
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.InternalSymbolFinderAPI
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.findDeclaration
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getPropertyGetter
import org.jetbrains.kotlin.ir.util.getPropertySetter
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.irError
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.util.EnumMap

// TODO KT-77388 rename to `BackendWebSymbolsImpl`
@OptIn(InternalSymbolFinderAPI::class)
abstract class JsCommonSymbols(
    irBuiltIns: IrBuiltIns,
) : PreSerializationWebSymbols, KlibSymbols(irBuiltIns) {
    override val coroutineImpl: IrClassSymbol = ClassIds.coroutineImpl.classSymbol()
    override val continuationClass = ClassIds.continuation.classSymbol()
    override val coroutineSuspendedGetter by CallableIds.coroutineSuspended.getterSymbol()

    val coroutineImplLabelPropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("state")!!.owner }
    val coroutineImplLabelPropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("state")!!.owner }
    val coroutineImplResultSymbolGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("result")!!.owner }
    val coroutineImplResultSymbolSetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("result")!!.owner }
    val coroutineImplExceptionPropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("exception")!!.owner }
    val coroutineImplExceptionPropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("exception")!!.owner }
    val coroutineImplExceptionStatePropertyGetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertyGetter("exceptionState")!!.owner }
    val coroutineImplExceptionStatePropertySetter by lazy(LazyThreadSafetyMode.NONE) { coroutineImpl.getPropertySetter("exceptionState")!!.owner }
}

@OptIn(ObsoleteDescriptorBasedAPI::class, InternalSymbolFinderAPI::class)
class JsSymbols(
    irBuiltIns: IrBuiltIns,
    private val stageController: StageController,
    private val compileLongAsBigint: Boolean
) : PreSerializationJsSymbols by PreSerializationJsSymbols.Impl(irBuiltIns), JsCommonSymbols(irBuiltIns) {
    override val throwNullPointerException = CallableIds.throwNpe.functionSymbol()

    // TODO investigate and drop if not required
    init {
        CallableIds.noWhenBranchMatchedException.functionSymbol()
    }

    override val throwTypeCastException = CallableIds.throwCce.functionSymbol()

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol = CallableIds.throwKotlinNothingValueException.functionSymbol()

    override val throwISE: IrSimpleFunctionSymbol = CallableIds.throwIse.functionSymbol()

    override val throwIAE: IrSimpleFunctionSymbol = CallableIds.throwIae.functionSymbol()

    override val stringBuilder
        get() = TODO("not implemented")

    private val _arraysContentEquals = CallableIds.contentEquals.functionSymbols().filter {
        it.descriptor.extensionReceiverParameter?.type?.isMarkedNullable == true
    }

    // Can't use .owner until ExternalStubGenerator is invoked, hence get() = here.
    override val arraysContentEquals: Map<IrType, IrSimpleFunctionSymbol>
        get() = _arraysContentEquals.associateBy { it.owner.parameters[0].type.makeNotNull() }

    override val getContinuation = CallableIds.getContinuation.functionSymbol()

    override val returnIfSuspended = CallableIds.returnIfSuspended.functionSymbol()

    override val functionAdapter = ClassIds.FunctionAdapter.classSymbol()

    override val defaultConstructorMarker = ClassIds.DefaultConstructorMarker.classSymbol()

    override fun functionN(n: Int): IrClassSymbol {
        return stageController.withInitialIr { super.functionN(n) }
    }

    override fun suspendFunctionN(n: Int): IrClassSymbol {
        return stageController.withInitialIr { super.suspendFunctionN(n) }
    }

    internal val subStringFunction: IrSimpleFunctionSymbol by CallableIds.subString.functionSymbol {
        it.hasShape(
            extensionReceiver = true,
            regularParameters = 2,
            parameterTypes = listOf(irBuiltIns.stringType, irBuiltIns.intType, irBuiltIns.intType)
        )
    }

    override fun isSideEffectFree(call: IrCall): Boolean =
        call.symbol in primitiveToLiteralConstructor.values ||
                call.symbol == arrayLiteral ||
                call.symbol == arrayConcat ||
                call.symbol == jsBoxIntrinsic ||
                call.symbol == jsUnboxIntrinsic

    // --- JS symbols ---
    // Modes
    val jsIsEs6 = CallableIds.jsIsEs6.functionSymbol()

    // Global variables
    val void = CallableIds.VOID.propertySymbols().single()
    val globalThis = CallableIds.globalThis.propertySymbols().single()

    // Equality operations:

    val jsEqeq = CallableIds.jsEqeq.functionSymbol()
    val jsNotEq = CallableIds.jsNotEq.functionSymbol()
    val jsEqeqeq = CallableIds.jsEqeqeq.functionSymbol()
    val jsNotEqeq = CallableIds.jsNotEqeq.functionSymbol()

    val jsGt = CallableIds.jsGt.functionSymbol()
    val jsGtEq = CallableIds.jsGtEq.functionSymbol()
    val jsLt = CallableIds.jsLt.functionSymbol()
    val jsLtEq = CallableIds.jsLtEq.functionSymbol()


    // Unary operations:

    val jsNot = CallableIds.jsNot.functionSymbol()

    val jsUnaryPlus = CallableIds.jsUnaryPlus.functionSymbol()
    val jsUnaryMinus = CallableIds.jsUnaryMinus.functionSymbol()

    val jsPrefixInc = CallableIds.jsPrefixInc.functionSymbol()
    val jsPostfixInc = CallableIds.jsPostfixInc.functionSymbol()
    val jsPrefixDec = CallableIds.jsPrefixDec.functionSymbol()
    val jsPostfixDec = CallableIds.jsPostfixDec.functionSymbol()

    val jsDelete = CallableIds.jsDelete.functionSymbol()

    val longUnaryMinus = CallableIds.negate(compileLongAsBigint).functionSymbol()

    // Binary operations:

    val jsPlus = CallableIds.jsPlus.functionSymbol()
    val jsMinus = CallableIds.jsMinus.functionSymbol()
    val jsMult = CallableIds.jsMult.functionSymbol()
    val jsDiv = CallableIds.jsDiv.functionSymbol()
    val jsMod = CallableIds.jsMod.functionSymbol()

    val jsPlusAssign = CallableIds.jsPlusAssign.functionSymbol()
    val jsMinusAssign = CallableIds.jsMinusAssign.functionSymbol()
    val jsMultAssign = CallableIds.jsMultAssign.functionSymbol()
    val jsDivAssign = CallableIds.jsDivAssign.functionSymbol()
    val jsModAssign = CallableIds.jsModAssign.functionSymbol()

    val jsAnd = CallableIds.jsAnd.functionSymbol()
    val jsOr = CallableIds.jsOr.functionSymbol()

    val jsIn = CallableIds.jsIn.functionSymbol()

    val longAdd = CallableIds.add(compileLongAsBigint).functionSymbol()
    val longSubtract = CallableIds.subtract(compileLongAsBigint).functionSymbol()
    val longMultiply = CallableIds.multiply(compileLongAsBigint).functionSymbol()
    val longDivide = CallableIds.divide(compileLongAsBigint).functionSymbol()
    val longModulo = CallableIds.modulo(compileLongAsBigint).functionSymbol()

    // Bit operations:

    val jsBitAnd = CallableIds.jsBitAnd.functionSymbol()
    val jsBitOr = CallableIds.jsBitOr.functionSymbol()
    val jsBitXor = CallableIds.jsBitXor.functionSymbol()
    val jsBitNot = CallableIds.jsBitNot.functionSymbol()

    val jsBitShiftR = CallableIds.jsBitShiftR.functionSymbol()
    val jsBitShiftRU = CallableIds.jsBitShiftRU.functionSymbol()
    val jsBitShiftL = CallableIds.jsBitShiftL.functionSymbol()

    val longAnd = CallableIds.bitwiseAnd(compileLongAsBigint).functionSymbols().singleOrNull()
    val longOr = CallableIds.bitwiseOr(compileLongAsBigint).functionSymbols().singleOrNull()
    val longXor = CallableIds.bitwiseXor(compileLongAsBigint).functionSymbols().singleOrNull()
    val longInv = CallableIds.invert(compileLongAsBigint).functionSymbols().singleOrNull()
    val longShiftLeft = CallableIds.shiftLeft(compileLongAsBigint).functionSymbol()
    val longShiftRight = CallableIds.shiftRight(compileLongAsBigint).functionSymbol()
    val longShiftRightUnsigned = CallableIds.shiftRightUnsigned(compileLongAsBigint).functionSymbol()

    // Type checks:

    val jsInstanceOf = CallableIds.jsInstanceOfIntrinsic.functionSymbol()
    val jsTypeOf = CallableIds.jsTypeOf.functionSymbol()
    val isExternalObject = CallableIds.isExternalObject.functionSymbol()

    // Number conversions:

    val jsNumberToByte = CallableIds.numberToByte.functionSymbol()
    val jsNumberToDouble = CallableIds.numberToDouble.functionSymbol()
    val jsNumberToInt = CallableIds.numberToInt.functionSymbol()
    val jsNumberToShort = CallableIds.numberToShort.functionSymbol()
    val jsNumberToLong = CallableIds.numberToLong(compileLongAsBigint).functionSymbol()
    val jsNumberToChar = CallableIds.numberToChar.functionSymbol()
    val jsToByte = CallableIds.toByte.functionSymbol()
    val jsToShort = CallableIds.toShort.functionSymbol()

    val longFromInt = CallableIds.fromInt(compileLongAsBigint).functionSymbol()

    val longToByte = CallableIds.convertToByte(compileLongAsBigint).functionSymbol()
    val longToNumber = CallableIds.toNumber(compileLongAsBigint).functionSymbol()
    val longToShort = CallableIds.convertToShort(compileLongAsBigint).functionSymbol()
    val longToInt = CallableIds.convertToInt(compileLongAsBigint).functionSymbol()
    val longToChar = CallableIds.convertToChar(compileLongAsBigint).functionSymbol()

    val longFromTwoInts = CallableIds.longFromTwoInts(compileLongAsBigint).functionSymbols().singleOrNull()
    val longLowBits = CallableIds.lowBits(compileLongAsBigint).functionSymbols().singleOrNull()
    val longHighBits = CallableIds.highBits(compileLongAsBigint).functionSymbols().singleOrNull()

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
            put(kind, "initMetadataFor${kind.namePart}".jsCallableId.functionSymbol())
        }
    }

    fun getInitMetadataSymbol(kind: RuntimeMetadataKind): IrSimpleFunctionSymbol? =
        initMetadataSymbols[kind]

    val makeAssociatedObjectMapES5 = CallableIds.makeAssociatedObjectMapES5.functionSymbol()
    val getAssociatedObjectId = CallableIds.getAssociatedObjectId.functionSymbol()
    val nextAssociatedObjectId = CallableIds.nextAssociatedObjectId.functionSymbol()

    val isInterfaceSymbol = CallableIds.isInterface.functionSymbol()
    val isArraySymbol = CallableIds.isArray.functionSymbol()
    //    val isCharSymbol = CallableIds.isChar.functionSymbol()
    val isSuspendFunctionSymbol = CallableIds.isSuspendFunction.functionSymbol()

    val isNumberSymbol = CallableIds.isNumber.functionSymbol()
    val isComparableSymbol = CallableIds.isComparable.functionSymbol()
    val isCharSequenceSymbol = CallableIds.isCharSequence.functionSymbol()

    val longArrayClass by CallableIds.longArrayClass(compileLongAsBigint).getterSymbol()
    val longCopyOfRange = CallableIds.longCopyOfRange.functionSymbol()

    val longCopyOfRangeForBoxedLong = CallableIds.longCopyOfRange(compileLongAsBigint).functionSymbols().singleOrNull()

    val isPrimitiveArray = mapOf(
        PrimitiveType.BOOLEAN to CallableIds.isBooleanArray.functionSymbol(),
        PrimitiveType.BYTE to CallableIds.isByteArray.functionSymbol(),
        PrimitiveType.SHORT to CallableIds.isShortArray.functionSymbol(),
        PrimitiveType.CHAR to CallableIds.isCharArray.functionSymbol(),
        PrimitiveType.INT to CallableIds.isIntArray.functionSymbol(),
        PrimitiveType.FLOAT to CallableIds.isFloatArray.functionSymbol(),
        PrimitiveType.LONG to CallableIds.isLongArray(compileLongAsBigint).functionSymbols().singleOrNull(),
        PrimitiveType.DOUBLE to CallableIds.isDoubleArray.functionSymbol()
    )

    // Enum

    val enumValueOfIntrinsic = CallableIds.enumValueOfIntrinsic.functionSymbol()
    val enumValuesIntrinsic = CallableIds.enumValuesIntrinsic.functionSymbol()
    val enumEntriesIntrinsic = CallableIds.enumEntriesIntrinsic.functionSymbol()

    // Other:

    override val jsCode = CallableIds.js.functionSymbol()
    val jsHashCode = CallableIds.hashCode.functionSymbol()
    val jsGetBooleanHashCode = CallableIds.getBooleanHashCode.functionSymbol()
    val jsGetNumberHashCode = CallableIds.getNumberHashCode.functionSymbol()
    val jsGetObjectHashCode = CallableIds.getObjectHashCode.functionSymbol()
    val jsGetStringHashCode = CallableIds.getStringHashCode.functionSymbol()
    val jsBigIntHashCode = CallableIds.bigIntHashCode.functionSymbol()
    val jsToString = CallableIds.toString.functionSymbol()
    val jsAnyToString = CallableIds.anyToString.functionSymbol()
    val jsCompareTo = CallableIds.compareTo.functionSymbol()
    val jsEquals = CallableIds.equals.functionSymbol()
    val jsNewTarget = CallableIds.jsNewTarget.functionSymbol()
    val jsEmptyObject = CallableIds.emptyObject.functionSymbol()
    val jsOpenInitializerBox = CallableIds.openInitializerBox.functionSymbol()

    val longEquals = CallableIds.equalsLong(compileLongAsBigint).functionSymbols().singleOrNull()

    val jsImul = CallableIds.imul.functionSymbol()

    val jsUnreachableDeclarationLog = CallableIds.unreachableDeclarationLog.functionSymbol()
    val jsUnreachableDeclarationException = CallableIds.unreachableDeclarationException.functionSymbol()

    val jsNativeBoolean = CallableIds.nativeBoolean.functionSymbol()
    val jsBooleanInExternalLog = CallableIds.booleanInExternalLog.functionSymbol()
    val jsBooleanInExternalException = CallableIds.booleanInExternalException.functionSymbol()

    val jsNewAnonymousClass = CallableIds.jsNewAnonymousClass.functionSymbol()

    val longBoxedOne = CallableIds.longBoxedOne.propertySymbols().single()

    // Coroutines

    val jsYieldFunctionSymbol = CallableIds.jsYield.functionSymbol()

    val jsInvokeSuspendSuperType: IrSimpleFunctionSymbol = CallableIds.invokeSuspendSuperType.functionSymbol()
    val jsInvokeSuspendSuperTypeWithReceiver: IrSimpleFunctionSymbol = CallableIds.invokeSuspendSuperTypeWithReceiver.functionSymbol()
    val jsInvokeSuspendSuperTypeWithReceiverAndParam: IrSimpleFunctionSymbol = CallableIds.invokeSuspendSuperTypeWithReceiverAndParam.functionSymbol()

    val createCoroutineUnintercepted: Set<IrSimpleFunctionSymbol> = CallableIds.createCoroutineUnintercepted.functionSymbols().toHashSet()
    val startCoroutineUninterceptedOrReturnNonGeneratorVersion: Set<IrSimpleFunctionSymbol> = CallableIds.startCoroutineUninterceptedOrReturnNonGeneratorVersion.functionSymbols().toHashSet()

    val createCoroutineUninterceptedGeneratorVersion: Set<IrSimpleFunctionSymbol> = CallableIds.createCoroutineUninterceptedGeneratorVersion.functionSymbols().toHashSet()
    val startCoroutineUninterceptedOrReturnGeneratorVersion: Set<IrSimpleFunctionSymbol> = CallableIds.startCoroutineUninterceptedOrReturnGeneratorVersion.functionSymbols().toHashSet()

    val startCoroutineUninterceptedOrReturnGeneratorVersion1 by lazy(LazyThreadSafetyMode.NONE) {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.hasShape(extensionReceiver = true, regularParameters = 1) }
    }
    val startCoroutineUninterceptedOrReturnGeneratorVersion2 by lazy(LazyThreadSafetyMode.NONE) {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.hasShape(extensionReceiver = true, regularParameters = 2) }
    }

    val awaitFunctionSymbol = CallableIds.await.functionSymbol()
    val promisifyFunctionSymbol = CallableIds.promisify.functionSymbol()
    val suspendOrReturnFunctionSymbol: IrSimpleFunctionSymbol = CallableIds.suspendOrReturn.functionSymbol()

    val jsNumberRangeToNumber = CallableIds.numberRangeToNumber.functionSymbol()
    val jsNumberRangeToLong = CallableIds.numberRangeToLong.functionSymbol()
    val jsLongRangeToNumber = CallableIds.longRangeToNumber.functionSymbol()
    val jsLongRangeToLong = CallableIds.longRangeToLong.functionSymbol()

    private val _rangeUntilFunctions = CallableIds.until.functionSymbols()
    val rangeUntilFunctions: Map<Pair<IrType, IrType>, IrSimpleFunctionSymbol> by lazy(LazyThreadSafetyMode.NONE) {
        _rangeUntilFunctions
            .filter { it.owner.hasShape(extensionReceiver = true, regularParameters = 1) }
            .associateBy { it.owner.parameters[0].type to it.owner.parameters[1].type }
    }

    val longClassSymbol = irBuiltIns.longClass

    val promiseClassSymbol: IrClassSymbol = JsStandardClassIds.Promise.classSymbol()

    val longCompareToLong: IrSimpleFunctionSymbol? = CallableIds.compare(compileLongAsBigint).functionSymbols().singleOrNull()

    val jsLongToString: IrSimpleFunctionSymbol = CallableIds.jsLongToString.functionSymbol()
    val longToStringImpl: IrSimpleFunctionSymbol = CallableIds.toStringImpl(compileLongAsBigint).functionSymbol()

    val charClassSymbol = irBuiltIns.charClass

    val stringConstructorSymbol by StandardClassIds.String.primaryConstructorSymbol()

    val anyClassSymbol = irBuiltIns.anyClass
    val anyConstructorSymbol by StandardClassIds.Any.primaryConstructorSymbol()

    val jsObjectConstructorSymbol by JsStandardClassIds.JsObject.primaryConstructorSymbol()

    val uByteClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { StandardClassIds.UByte.classSymbol() }
    val uShortClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { StandardClassIds.UShort.classSymbol() }
    val uIntClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { StandardClassIds.UInt.classSymbol() }
    val uLongClassSymbol: IrClassSymbol by lazy(LazyThreadSafetyMode.NONE) { StandardClassIds.ULong.classSymbol() }

    val unreachable = CallableIds.unreachable.functionSymbol()

    val jsArguments = CallableIds.jsArguments.functionSymbol()

    val jsEnsureNonNull = CallableIds.ensureNotNull.functionSymbol()

    // Arrays:
    val primitiveArrays get() = irBuiltIns.primitiveArraysToPrimitiveTypes

    val jsArrayLength = CallableIds.jsArrayLength.functionSymbol()
    val jsArrayGet = CallableIds.jsArrayGet.functionSymbol()
    val jsArraySet = CallableIds.jsArraySet.functionSymbol()

    val jsArrayIteratorFunction = CallableIds.arrayIterator.functionSymbol()

    val jsPrimitiveArrayIteratorFunctions =
        PrimitiveType.entries.associate { it to "${it.typeName.asString().toLowerCaseAsciiOnly()}ArrayIterator".jsCallableId.functionSymbol() }

    val jsClass = CallableIds.jsClassIntrinsic.functionSymbol()
    val arrayLiteral: IrSimpleFunctionSymbol = CallableIds.arrayLiteral.functionSymbol()

    // The following 3 functions are all lowered into [].slice.call(...), they only differ
    // in the number of arguments.
    // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
    val jsArrayLike2Array = CallableIds.jsArrayLike2Array.functionSymbol()
    val jsSliceArrayLikeFromIndex = CallableIds.jsSliceArrayLikeFromIndex.functionSymbol()
    val jsSliceArrayLikeFromIndexToIndex = CallableIds.jsSliceArrayLikeFromIndexToIndex.functionSymbol()

    internal inner class JsReflectionSymbols : ReflectionSymbols {
        override val createKType: IrSimpleFunctionSymbol = CallableIds.createKType.functionSymbol()
        override val createDynamicKType: IrSimpleFunctionSymbol = CallableIds.createDynamicKType.functionSymbol()
        override val createKTypeParameter: IrSimpleFunctionSymbol = CallableIds.createKTypeParameter.functionSymbol()
        override val getStarKTypeProjection: IrSimpleFunctionSymbol = CallableIds.getStarKTypeProjection.functionSymbol()
        override val createCovariantKTypeProjection: IrSimpleFunctionSymbol = CallableIds.createCovariantKTypeProjection.functionSymbol()
        override val createInvariantKTypeProjection: IrSimpleFunctionSymbol = CallableIds.createInvariantKTypeProjection.functionSymbol()
        override val createContravariantKTypeProjection: IrSimpleFunctionSymbol = CallableIds.createContravariantKTypeProjection.functionSymbol()
        override val getKClass: IrSimpleFunctionSymbol = CallableIds.getKClass.functionSymbol()
        override val getKClassFromExpression: IrSimpleFunctionSymbol = CallableIds.getKClassFromExpression.functionSymbol()
        override val kTypeClass: IrClassSymbol = StandardClassIds.KType.classSymbol()
    }

    val primitiveClassesObject: IrClassSymbol = ClassIds.PrimitiveClasses.classSymbol()

    internal val reflectionSymbols: JsReflectionSymbols = JsReflectionSymbols()

    val primitiveToTypedArrayMap = EnumMap(
        buildMap<PrimitiveType, String> {
            set(PrimitiveType.BYTE, "Int8")
            set(PrimitiveType.SHORT, "Int16")
            set(PrimitiveType.INT, "Int32")
            set(PrimitiveType.FLOAT, "Float32")
            set(PrimitiveType.DOUBLE, "Float64")
            if (compileLongAsBigint) {
                set(PrimitiveType.LONG, "BigInt64")
            }
        }
    )

    val primitiveToSizeConstructor =
        PrimitiveType.entries.associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                "${it.toLowerCaseAsciiOnly()}Array".jsCallableId.functionSymbol()
            } ?: "${type.typeName.asString().toLowerCaseAsciiOnly()}Array".jsCallableId.functionSymbol())
        }

    val primitiveToLiteralConstructor =
        PrimitiveType.entries.associate { type ->
            type to (primitiveToTypedArrayMap[type]?.let {
                "${it.toLowerCaseAsciiOnly()}ArrayOf".jsCallableId.functionSymbol()
            } ?: "${type.typeName.asString().toLowerCaseAsciiOnly()}ArrayOf".jsCallableId.functionSymbol())
        }

    val arrayConcat: IrSimpleFunctionSymbol = CallableIds.arrayConcat.functionSymbol()
    val primitiveArrayConcat: IrSimpleFunctionSymbol = CallableIds.primitiveArrayConcat.functionSymbol()
    val taggedArrayCopy: IrSimpleFunctionSymbol = CallableIds.taggedArrayCopy.functionSymbol()

    val jsArraySlice = CallableIds.slice.functionSymbol()

    val jsCall = CallableIds.jsCall.functionSymbol()
    val jsBind = CallableIds.jsBind.functionSymbol()

    val jsNameAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsName.classSymbol()
    val jsStaticAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsStatic.classSymbol()
    val jsExportAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsExport.classSymbol()
    val jsGeneratorAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsGenerator.classSymbol()

    val jsExportIgnoreAnnotationSymbol = JsStandardClassIds.Annotations.JsExportIgnore.classSymbol()

    val jsImplicitExportAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsImplicitExport.classSymbol()

    val charSequenceClassSymbol = StandardClassIds.CharSequence.classSymbol()
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


    val jsCharSequenceGet = CallableIds.charSequenceGet.functionSymbol()
    val jsCharCodeAt = CallableIds.charCodeAt.functionSymbol()
    val jsCharSequenceLength = CallableIds.charSequenceLength.functionSymbol()
    val jsCharSequenceSubSequence = CallableIds.charSequenceSubSequence.functionSymbol()

    val jsContexfulRef = CallableIds.jsContextfulRef.functionSymbol()
    val jsBoxIntrinsic = CallableIds.boxIntrinsic.functionSymbol()
    val jsUnboxIntrinsic = CallableIds.unboxIntrinsic.functionSymbol()

    val captureStack = CallableIds.captureStack.functionSymbol()

    val linkageErrorSymbol = CallableIds.throwIrLinkageError.functionSymbol()

    val jsPrototypeOfSymbol = CallableIds.protoOf.functionSymbol()
    val jsDefinePropertySymbol = CallableIds.defineProp.functionSymbol()
    val jsObjectCreateSymbol = CallableIds.objectCreate.functionSymbol()
    val jsCreateThisSymbol = CallableIds.createThis.functionSymbol()
    val jsBoxApplySymbol = CallableIds.boxApply.functionSymbol()
    val jsCreateExternalThisSymbol = CallableIds.createExternalThis.functionSymbol()

    // Collections interop:
    val jsCreateListFrom = CallableIds.createListFrom.functionSymbol()
    val jsCreateMutableListFrom = CallableIds.createMutableListFrom.functionSymbol()
    val jsCreateSetFrom = CallableIds.createSetFrom.functionSymbol()
    val jsCreateMutableSetFrom = CallableIds.createMutableSetFrom.functionSymbol()
    val jsCreateMapFrom = CallableIds.createMapFrom.functionSymbol()
    val jsCreateMutableMapFrom = CallableIds.createMutableMapFrom.functionSymbol()
}

private object ClassIds {
    private val String.jsClassId get() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
    val FunctionAdapter = "FunctionAdapter".jsClassId
    val DefaultConstructorMarker = "DefaultConstructorMarker".jsClassId

    // Coroutines classes
    private val String.coroutinesClassId get() = ClassId(StandardNames.COROUTINES_PACKAGE_FQ_NAME, Name.identifier(this))
    val coroutineImpl = "CoroutineImpl".coroutinesClassId
    val continuation = "Continuation".coroutinesClassId

    // Other
    val PrimitiveClasses = ClassId(JsStandardClassIds.BASE_REFLECT_JS_INTERNAL_PACKAGE, Name.identifier("PrimitiveClasses"))
}

private val String.jsCallableId get() = CallableId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))

private object CallableIds {
    // Kotlin functions
    private val String.kotlinCallableId get() = CallableId(kotlinPackageFqn, Name.identifier(this))
    val throwNpe = "THROW_NPE".kotlinCallableId
    val noWhenBranchMatchedException = "noWhenBranchMatchedException".kotlinCallableId
    val throwCce = "THROW_CCE".kotlinCallableId
    val throwKotlinNothingValueException = "throwKotlinNothingValueException".kotlinCallableId
    val throwIse = "THROW_ISE".kotlinCallableId
    val throwIae = "THROW_IAE".kotlinCallableId
    val ensureNotNull = "ensureNotNull".kotlinCallableId

    // Enum functions
    private val String.enumCallableId get() = CallableId(StandardClassIds.BASE_ENUMS_PACKAGE, Name.identifier(this))
    val enumEntriesIntrinsic = "enumEntriesIntrinsic".enumCallableId

    // JS functions
    val getContinuation = "getContinuation".jsCallableId
    val returnIfSuspended = "returnIfSuspended".jsCallableId

    val jsIsEs6 = "jsIsEs6".jsCallableId
    val VOID = "VOID".jsCallableId
    val globalThis = "globalThis".jsCallableId
    val jsEqeq = "jsEqeq".jsCallableId
    val jsNotEq = "jsNotEq".jsCallableId
    val jsEqeqeq = "jsEqeqeq".jsCallableId
    val jsNotEqeq = "jsNotEqeq".jsCallableId
    val jsGt = "jsGt".jsCallableId
    val jsGtEq = "jsGtEq".jsCallableId
    val jsLt = "jsLt".jsCallableId
    val jsLtEq = "jsLtEq".jsCallableId
    val jsNot = "jsNot".jsCallableId
    val jsUnaryPlus = "jsUnaryPlus".jsCallableId
    val jsUnaryMinus = "jsUnaryMinus".jsCallableId
    val jsPrefixInc = "jsPrefixInc".jsCallableId
    val jsPostfixInc = "jsPostfixInc".jsCallableId
    val jsPrefixDec = "jsPrefixDec".jsCallableId
    val jsPostfixDec = "jsPostfixDec".jsCallableId
    val jsDelete = "jsDelete".jsCallableId
    val jsPlus = "jsPlus".jsCallableId
    val jsMinus = "jsMinus".jsCallableId
    val jsMult = "jsMult".jsCallableId
    val jsDiv = "jsDiv".jsCallableId
    val jsMod = "jsMod".jsCallableId
    val jsPlusAssign = "jsPlusAssign".jsCallableId
    val jsMinusAssign = "jsMinusAssign".jsCallableId
    val jsMultAssign = "jsMultAssign".jsCallableId
    val jsDivAssign = "jsDivAssign".jsCallableId
    val jsModAssign = "jsModAssign".jsCallableId
    val jsAnd = "jsAnd".jsCallableId
    val jsOr = "jsOr".jsCallableId
    val jsIn = "jsInIntrinsic".jsCallableId
    val jsBitAnd = "jsBitAnd".jsCallableId
    val jsBitOr = "jsBitOr".jsCallableId
    val jsBitXor = "jsBitXor".jsCallableId
    val jsBitNot = "jsBitNot".jsCallableId
    val jsBitShiftR = "jsBitShiftR".jsCallableId
    val jsBitShiftRU = "jsBitShiftRU".jsCallableId
    val jsBitShiftL = "jsBitShiftL".jsCallableId
    val jsInstanceOfIntrinsic = "jsInstanceOfIntrinsic".jsCallableId
    val jsTypeOf = "jsTypeOf".jsCallableId
    val isExternalObject = "isExternalObject".jsCallableId
    val numberToByte = "numberToByte".jsCallableId
    val numberToDouble = "numberToDouble".jsCallableId
    val numberToInt = "numberToInt".jsCallableId
    val numberToShort = "numberToShort".jsCallableId
    val numberToChar = "numberToChar".jsCallableId
    val toByte = "toByte".jsCallableId
    val toShort = "toShort".jsCallableId
    val nextAssociatedObjectId = "nextAssociatedObjectId".jsCallableId
    val isInterface = "isInterface".jsCallableId
    val isArray = "isArray".jsCallableId
    //    val isChar = "isChar".jsCallableId
    val isSuspendFunction = "isSuspendFunction".jsCallableId
    val isNumber = "isNumber".jsCallableId
    val isComparable = "isComparable".jsCallableId
    val isCharSequence = "isCharSequence".jsCallableId
    val longCopyOfRange = "longCopyOfRange".jsCallableId
    val isBooleanArray = "isBooleanArray".jsCallableId
    val isByteArray = "isByteArray".jsCallableId
    val isShortArray = "isShortArray".jsCallableId
    val isCharArray = "isCharArray".jsCallableId
    val isIntArray = "isIntArray".jsCallableId
    val isFloatArray = "isFloatArray".jsCallableId
    val isDoubleArray = "isDoubleArray".jsCallableId
    val enumValueOfIntrinsic = "enumValueOfIntrinsic".jsCallableId
    val enumValuesIntrinsic = "enumValuesIntrinsic".jsCallableId
    val js = "js".jsCallableId
    val hashCode = "hashCode".jsCallableId
    val getBooleanHashCode = "getBooleanHashCode".jsCallableId
    val getNumberHashCode = "getNumberHashCode".jsCallableId
    val getObjectHashCode = "getObjectHashCode".jsCallableId
    val getStringHashCode = "getStringHashCode".jsCallableId
    val bigIntHashCode = "getBigIntHashCode".jsCallableId
    val toString = "toString".jsCallableId
    val anyToString = "anyToString".jsCallableId
    val compareTo = "compareTo".jsCallableId
    val equals = "equals".jsCallableId
    val jsNewTarget = "jsNewTarget".jsCallableId
    val emptyObject = "emptyObject".jsCallableId
    val openInitializerBox = "openInitializerBox".jsCallableId
    val imul = "imul".jsCallableId
    val unreachableDeclarationLog = "unreachableDeclarationLog".jsCallableId
    val unreachableDeclarationException = "unreachableDeclarationException".jsCallableId
    val nativeBoolean = "nativeBoolean".jsCallableId
    val booleanInExternalLog = "booleanInExternalLog".jsCallableId
    val booleanInExternalException = "booleanInExternalException".jsCallableId
    val jsNewAnonymousClass = "jsNewAnonymousClass".jsCallableId
    val jsYield = "jsYield".jsCallableId
    val numberRangeToNumber = "numberRangeToNumber".jsCallableId
    val numberRangeToLong = "numberRangeToLong".jsCallableId
    val longRangeToNumber = "longRangeToNumber".jsCallableId
    val longRangeToLong = "longRangeToLong".jsCallableId
    val jsLongToString = "jsLongToString".jsCallableId
    val unreachable = "unreachable".jsCallableId
    val jsArguments = "jsArguments".jsCallableId
    val jsArrayLength = "jsArrayLength".jsCallableId
    val jsArrayGet = "jsArrayGet".jsCallableId
    val jsArraySet = "jsArraySet".jsCallableId
    val arrayIterator = "arrayIterator".jsCallableId
    val jsClassIntrinsic = "jsClassIntrinsic".jsCallableId
    val arrayLiteral = "arrayLiteral".jsCallableId
    val jsArrayLike2Array = "jsArrayLike2Array".jsCallableId
    val jsSliceArrayLikeFromIndex = "jsSliceArrayLikeFromIndex".jsCallableId
    val jsSliceArrayLikeFromIndexToIndex = "jsSliceArrayLikeFromIndexToIndex".jsCallableId
    val slice = "slice".jsCallableId
    val jsCall = "jsCall".jsCallableId
    val jsBind = "jsBind".jsCallableId
    val charSequenceGet = "charSequenceGet".jsCallableId
    val charCodeAt = "charCodeAt".jsCallableId
    val charSequenceLength = "charSequenceLength".jsCallableId
    val charSequenceSubSequence = "charSequenceSubSequence".jsCallableId
    val jsContextfulRef = "jsContextfulRef".jsCallableId
    val boxIntrinsic = "boxIntrinsic".jsCallableId
    val unboxIntrinsic = "unboxIntrinsic".jsCallableId
    val captureStack = "captureStack".jsCallableId
    val protoOf = "protoOf".jsCallableId
    val defineProp = "defineProp".jsCallableId
    val objectCreate = "objectCreate".jsCallableId
    val createThis = "createThis".jsCallableId
    val boxApply = "boxApply".jsCallableId
    val createExternalThis = "createExternalThis".jsCallableId

    // JS Long functions
    private val String.jsBoxedLongId get() = CallableId(JsStandardClassIds.BOXED_LONG_PACKAGE, Name.identifier(this))
    private val String.jsLongAsBigIntId get() = CallableId(JsStandardClassIds.LONG_AS_BIGINT_PACKAGE, Name.identifier(this))
    private fun String.jsLongId(compileLongAsBigint: Boolean) = if (compileLongAsBigint) this.jsLongAsBigIntId else this.jsBoxedLongId

    val longBoxedOne = "ONE".jsBoxedLongId

    fun negate(compileLongAsBigint: Boolean) = "negate".jsLongId(compileLongAsBigint)
    fun add(compileLongAsBigint: Boolean) = "add".jsLongId(compileLongAsBigint)
    fun subtract(compileLongAsBigint: Boolean) = "subtract".jsLongId(compileLongAsBigint)
    fun multiply(compileLongAsBigint: Boolean) = "multiply".jsLongId(compileLongAsBigint)
    fun divide(compileLongAsBigint: Boolean) = "divide".jsLongId(compileLongAsBigint)
    fun modulo(compileLongAsBigint: Boolean) = "modulo".jsLongId(compileLongAsBigint)
    fun bitwiseAnd(compileLongAsBigint: Boolean) = "bitwiseAnd".jsLongId(compileLongAsBigint)
    fun bitwiseOr(compileLongAsBigint: Boolean) = "bitwiseOr".jsLongId(compileLongAsBigint)
    fun bitwiseXor(compileLongAsBigint: Boolean) = "bitwiseXor".jsLongId(compileLongAsBigint)
    fun invert(compileLongAsBigint: Boolean) = "invert".jsLongId(compileLongAsBigint)
    fun shiftLeft(compileLongAsBigint: Boolean) = "shiftLeft".jsLongId(compileLongAsBigint)
    fun shiftRight(compileLongAsBigint: Boolean) = "shiftRight".jsLongId(compileLongAsBigint)
    fun shiftRightUnsigned(compileLongAsBigint: Boolean) = "shiftRightUnsigned".jsLongId(compileLongAsBigint)
    fun numberToLong(compileLongAsBigint: Boolean) = "numberToLong".jsLongId(compileLongAsBigint)
    fun fromInt(compileLongAsBigint: Boolean) = "fromInt".jsLongId(compileLongAsBigint)
    fun convertToByte(compileLongAsBigint: Boolean) = "convertToByte".jsLongId(compileLongAsBigint)
    fun toNumber(compileLongAsBigint: Boolean) = "toNumber".jsLongId(compileLongAsBigint)
    fun convertToShort(compileLongAsBigint: Boolean) = "convertToShort".jsLongId(compileLongAsBigint)
    fun convertToInt(compileLongAsBigint: Boolean) = "convertToInt".jsLongId(compileLongAsBigint)
    fun convertToChar(compileLongAsBigint: Boolean) = "convertToChar".jsLongId(compileLongAsBigint)
    fun longFromTwoInts(compileLongAsBigint: Boolean) = "longFromTwoInts".jsLongId(compileLongAsBigint)
    fun lowBits(compileLongAsBigint: Boolean) = "lowBits".jsLongId(compileLongAsBigint)
    fun highBits(compileLongAsBigint: Boolean) = "highBits".jsLongId(compileLongAsBigint)
    fun longArrayClass(compileLongAsBigint: Boolean) = "longArrayClass".jsLongId(compileLongAsBigint)
    fun longCopyOfRange(compileLongAsBigint: Boolean) = "longCopyOfRange".jsLongId(compileLongAsBigint)
    fun isLongArray(compileLongAsBigint: Boolean) = "isLongArray".jsLongId(compileLongAsBigint)
    fun equalsLong(compileLongAsBigint: Boolean) = "equalsLong".jsLongId(compileLongAsBigint)
    fun compare(compileLongAsBigint: Boolean) = "compare".jsLongId(compileLongAsBigint)
    fun toStringImpl(compileLongAsBigint: Boolean) = "toStringImpl".jsLongId(compileLongAsBigint)

    // Root functions
    private val String.rootId get() = CallableId(FqName.ROOT, Name.identifier(this))
    val makeAssociatedObjectMapES5 = "makeAssociatedObjectMapES5".rootId
    val getAssociatedObjectId = "getAssociatedObjectId".rootId
    val arrayConcat = "arrayConcat".rootId
    val primitiveArrayConcat = "primitiveArrayConcat".rootId
    val taggedArrayCopy = "taggedArrayCopy".rootId

    // Collections functions
    private val String.collectionsCallableId get() = CallableId(StandardNames.COLLECTIONS_PACKAGE_FQ_NAME, Name.identifier(this))
    val contentEquals = "contentEquals".collectionsCallableId
    val createListFrom = "createListFrom".collectionsCallableId
    val createMutableListFrom = "createMutableListFrom".collectionsCallableId
    val createSetFrom = "createSetFrom".collectionsCallableId
    val createMutableSetFrom = "createMutableSetFrom".collectionsCallableId
    val createMapFrom = "createMapFrom".collectionsCallableId
    val createMutableMapFrom = "createMutableMapFrom".collectionsCallableId

    // JS reflection functions
    private val String.reflectionCallableId get() = CallableId(JsStandardClassIds.BASE_REFLECT_JS_INTERNAL_PACKAGE, Name.identifier(this))
    val createKType = "createKType".reflectionCallableId
    val createDynamicKType = "createDynamicKType".reflectionCallableId
    val createKTypeParameter = "createKTypeParameter".reflectionCallableId
    val getStarKTypeProjection = "getStarKTypeProjection".reflectionCallableId
    val createCovariantKTypeProjection = "createCovariantKTypeProjection".reflectionCallableId
    val createInvariantKTypeProjection = "createInvariantKTypeProjection".reflectionCallableId
    val createContravariantKTypeProjection = "createContravariantKTypeProjection".reflectionCallableId
    val getKClass = "getKClass".reflectionCallableId
    val getKClassFromExpression = "getKClassFromExpression".reflectionCallableId

    // Coroutines functions
    private val String.coroutinesCallableId get() = CallableId(StandardClassIds.BASE_COROUTINES_INTRINSICS_PACKAGE, Name.identifier(this))
    val invokeSuspendSuperType = "invokeSuspendSuperType".coroutinesCallableId
    val invokeSuspendSuperTypeWithReceiver = "invokeSuspendSuperTypeWithReceiver".coroutinesCallableId
    val invokeSuspendSuperTypeWithReceiverAndParam = "invokeSuspendSuperTypeWithReceiverAndParam".coroutinesCallableId
    val createCoroutineUnintercepted = "createCoroutineUnintercepted".coroutinesCallableId
    val startCoroutineUninterceptedOrReturnNonGeneratorVersion = "startCoroutineUninterceptedOrReturnNonGeneratorVersion".coroutinesCallableId
    val createCoroutineUninterceptedGeneratorVersion = "createCoroutineUninterceptedGeneratorVersion".coroutinesCallableId
    val startCoroutineUninterceptedOrReturnGeneratorVersion = "startCoroutineUninterceptedOrReturnGeneratorVersion".coroutinesCallableId
    val await = "await".coroutinesCallableId
    val promisify = "promisify".coroutinesCallableId
    val suspendOrReturn = "suspendOrReturn".coroutinesCallableId

    // Others
    val coroutineSuspended = CallableId(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, StandardNames.COROUTINE_SUSPENDED_NAME)
    val subString = CallableId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier("substring"))
    val until = CallableId(StandardNames.RANGES_PACKAGE_FQ_NAME, Name.identifier("until"))
    val throwIrLinkageError = CallableId(StandardClassIds.BASE_INTERNAL_PACKAGE, Name.identifier("throwIrLinkageError"))
}
