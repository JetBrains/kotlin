/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.ir.BackendKlibSymbols
import org.jetbrains.kotlin.backend.common.ir.PreSerializationJsSymbols
import org.jetbrains.kotlin.backend.common.ir.PreSerializationWebSymbols
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.hasShape
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.kotlinPackageFqn
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import java.util.*

abstract class BackendWebSymbols(
    irBuiltIns: IrBuiltIns,
) : PreSerializationWebSymbols, BackendKlibSymbols(irBuiltIns) {
    abstract val throwISE: IrSimpleFunctionSymbol
    abstract val throwIAE: IrSimpleFunctionSymbol

    open val coroutineImpl: IrClassSymbol = ClassIds.coroutineImpl.classSymbol()
    override val continuationClass = ClassIds.continuation.classSymbol()
    override val coroutineSuspendedGetter by CallableIds.coroutineSuspended.getterSymbol()

    val coroutineImplLabelPropertyGetter by CallableIds.coroutineState.getterSymbol()
    val coroutineImplLabelPropertySetter by CallableIds.coroutineState.setterSymbol()
    val coroutineImplResultSymbolGetter by CallableIds.coroutineResult.getterSymbol()
    val coroutineImplExceptionPropertyGetter by CallableIds.coroutineException.getterSymbol()
    val coroutineImplExceptionPropertySetter by CallableIds.coroutineException.setterSymbol()
    val coroutineImplExceptionStatePropertyGetter by CallableIds.coroutineExceptionState.getterSymbol()
    val coroutineImplExceptionStatePropertySetter by CallableIds.coroutineExceptionState.setterSymbol()

    val testFun by CallableIds.test.functionSymbolOrNull()
    val suiteFun by CallableIds.suite.functionSymbolOrNull()
    val enumEntries: IrClassSymbol = ClassIds.EnumEntries.classSymbol()
    val createEnumEntries: IrSimpleFunctionSymbol by CallableIds.enumEntries
        .functionSymbol { it.parameters.firstOrNull()?.type?.isFunctionOrKFunction() == false }
}

class BackendJsSymbols(
    irBuiltIns: IrBuiltIns,
    private val stageController: StageController,
    private val compileLongAsBigint: Boolean
) : PreSerializationJsSymbols by PreSerializationJsSymbols.Impl(irBuiltIns), BackendWebSymbols(irBuiltIns) {
    val noWhenBranchMatchedException by CallableIds.noWhenBranchMatchedException.functionSymbol()

    override val throwNullPointerException by CallableIds.throwNpe.functionSymbol()

    override val throwTypeCastException by CallableIds.throwCce.functionSymbol()

    override val throwKotlinNothingValueException: IrSimpleFunctionSymbol by CallableIds.throwKotlinNothingValueException.functionSymbol()

    override val throwISE: IrSimpleFunctionSymbol by CallableIds.throwIse.functionSymbol()

    override val throwIAE: IrSimpleFunctionSymbol by CallableIds.throwIae.functionSymbol()

    private val _stringBuilder = ClassIds.StringBuilder.classSymbolOrNull()
    override val stringBuilder: IrClassSymbol
        get() = _stringBuilder ?: TODO("Not implemented")

    override val getContinuation by CallableIds.getContinuation.functionSymbol()

    val coroutineEmptyContinuation: IrPropertySymbol by CallableIds.EmptyContinuation.propertySymbol()

    override val returnIfSuspended by CallableIds.returnIfSuspended.functionSymbol()

    override val functionAdapter = ClassIds.FunctionAdapter.classSymbol()

    override val defaultConstructorMarker = ClassIds.DefaultConstructorMarker.classSymbol()

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
    val jsIsEs6 by CallableIds.jsIsEs6.functionSymbol()

    // Global variables
    val void by CallableIds.VOID.propertySymbol()
    val globalThis by CallableIds.globalThis.propertySymbol()

    // Equality operations:

    val jsEqeq by CallableIds.jsEqeq.functionSymbol()
    val jsNotEq by CallableIds.jsNotEq.functionSymbol()
    val jsEqeqeq by CallableIds.jsEqeqeq.functionSymbol()
    val jsNotEqeq by CallableIds.jsNotEqeq.functionSymbol()

    val jsGt by CallableIds.jsGt.functionSymbol()
    val jsGtEq by CallableIds.jsGtEq.functionSymbol()
    val jsLt by CallableIds.jsLt.functionSymbol()
    val jsLtEq by CallableIds.jsLtEq.functionSymbol()


    // Unary operations:

    val jsNot by CallableIds.jsNot.functionSymbol()

    val jsUnaryPlus by CallableIds.jsUnaryPlus.functionSymbol()
    val jsUnaryMinus by CallableIds.jsUnaryMinus.functionSymbol()

    val jsPrefixInc by CallableIds.jsPrefixInc.functionSymbol()
    val jsPostfixInc by CallableIds.jsPostfixInc.functionSymbol()
    val jsPrefixDec by CallableIds.jsPrefixDec.functionSymbol()
    val jsPostfixDec by CallableIds.jsPostfixDec.functionSymbol()

    val jsDelete by CallableIds.jsDelete.functionSymbol()

    val longUnaryMinus by CallableIds.negate(compileLongAsBigint).functionSymbol()

    // Binary operations:

    val jsPlus by CallableIds.jsPlus.functionSymbol()
    val jsMinus by CallableIds.jsMinus.functionSymbol()
    val jsMult by CallableIds.jsMult.functionSymbol()
    val jsDiv by CallableIds.jsDiv.functionSymbol()
    val jsMod by CallableIds.jsMod.functionSymbol()

    val jsPlusAssign by CallableIds.jsPlusAssign.functionSymbol()
    val jsMinusAssign by CallableIds.jsMinusAssign.functionSymbol()
    val jsMultAssign by CallableIds.jsMultAssign.functionSymbol()
    val jsDivAssign by CallableIds.jsDivAssign.functionSymbol()
    val jsModAssign by CallableIds.jsModAssign.functionSymbol()

    val jsAnd by CallableIds.jsAnd.functionSymbol()
    val jsOr by CallableIds.jsOr.functionSymbol()

    val jsIn by CallableIds.jsIn.functionSymbol()

    val longAdd by CallableIds.add(compileLongAsBigint).functionSymbol()
    val longSubtract by CallableIds.subtract(compileLongAsBigint).functionSymbol()
    val longMultiply by CallableIds.multiply(compileLongAsBigint).functionSymbol()
    val longDivide by CallableIds.divide(compileLongAsBigint).functionSymbol()
    val longModulo by CallableIds.modulo(compileLongAsBigint).functionSymbol()

    // Bit operations:

    val jsBitAnd by CallableIds.jsBitAnd.functionSymbol()
    val jsBitOr by CallableIds.jsBitOr.functionSymbol()
    val jsBitXor by CallableIds.jsBitXor.functionSymbol()
    val jsBitNot by CallableIds.jsBitNot.functionSymbol()

    val jsBitShiftR by CallableIds.jsBitShiftR.functionSymbol()
    val jsBitShiftRU by CallableIds.jsBitShiftRU.functionSymbol()
    val jsBitShiftL by CallableIds.jsBitShiftL.functionSymbol()

    val longAnd by CallableIds.bitwiseAnd(compileLongAsBigint).functionSymbolOrNull()
    val longOr by CallableIds.bitwiseOr(compileLongAsBigint).functionSymbolOrNull()
    val longXor by CallableIds.bitwiseXor(compileLongAsBigint).functionSymbolOrNull()
    val longInv by CallableIds.invert(compileLongAsBigint).functionSymbolOrNull()
    val longShiftLeft by CallableIds.shiftLeft(compileLongAsBigint).functionSymbol()
    val longShiftRight by CallableIds.shiftRight(compileLongAsBigint).functionSymbol()
    val longShiftRightUnsigned by CallableIds.shiftRightUnsigned(compileLongAsBigint).functionSymbol()

    // Type checks:

    val jsInstanceOf by CallableIds.jsInstanceOfIntrinsic.functionSymbol()
    val jsTypeOf by CallableIds.jsTypeOf.functionSymbol()
    val isExternalObject by CallableIds.isExternalObject.functionSymbol()

    val jsIsFunction by CallableIds.jsIsFunction.functionSymbol()
    val jsMethodReference by CallableIds.jsMethodReference.functionSymbol()

    // Number conversions:

    val jsNumberToByte by CallableIds.numberToByte.functionSymbol()
    val jsNumberToDouble by CallableIds.numberToDouble.functionSymbol()
    val jsNumberToInt by CallableIds.numberToInt.functionSymbol()
    val jsNumberToShort by CallableIds.numberToShort.functionSymbol()
    val jsNumberToLong by CallableIds.numberToLong(compileLongAsBigint).functionSymbol()
    val jsNumberToChar by CallableIds.numberToChar.functionSymbol()
    val jsToByte by CallableIds.toByte.functionSymbol()
    val jsToShort by CallableIds.toShort.functionSymbol()

    val longFromInt by CallableIds.fromInt(compileLongAsBigint).functionSymbol()

    val longToByte by CallableIds.convertToByte(compileLongAsBigint).functionSymbol()
    val longToNumber by CallableIds.toNumber(compileLongAsBigint).functionSymbol()
    val longToShort by CallableIds.convertToShort(compileLongAsBigint).functionSymbol()
    val longToInt by CallableIds.convertToInt(compileLongAsBigint).functionSymbol()
    val longToChar by CallableIds.convertToChar(compileLongAsBigint).functionSymbol()

    val longFromTwoInts by CallableIds.longFromTwoInts(compileLongAsBigint).functionSymbolOrNull()
    val longLowBits by CallableIds.lowBits(compileLongAsBigint).functionSymbolOrNull()
    val longHighBits by CallableIds.highBits(compileLongAsBigint).functionSymbolOrNull()

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

    private val initMetadataSymbols: Map<RuntimeMetadataKind, Lazy<IrSimpleFunctionSymbol>> = buildMap {
        for (kind in RuntimeMetadataKind.entries) {
            put(kind, "initMetadataFor${kind.namePart}".jsCallableId.functionSymbol())
        }
    }

    fun getInitMetadataSymbol(kind: RuntimeMetadataKind): IrSimpleFunctionSymbol? =
        initMetadataSymbols[kind]?.value

    val makeAssociatedObjectMapES5 by CallableIds.makeAssociatedObjectMapES5.functionSymbol()
    val getAssociatedObjectId by CallableIds.getAssociatedObjectId.functionSymbol()
    val nextAssociatedObjectId by CallableIds.nextAssociatedObjectId.functionSymbol()

    val isInterfaceSymbol by CallableIds.isInterface.functionSymbol()
    val isArraySymbol by CallableIds.isArray.functionSymbol()
    //    val isCharSymbol = CallableIds.isChar.functionSymbol()
    val isSuspendFunctionSymbol by CallableIds.isSuspendFunction.functionSymbol()

    val isNumberSymbol by CallableIds.isNumber.functionSymbol()
    val isComparableSymbol by CallableIds.isComparable.functionSymbol()
    val isCharSequenceSymbol by CallableIds.isCharSequence.functionSymbol()

    val longCopyOfRange by CallableIds.longCopyOfRange.functionSymbol()

    val longCopyOfRangeForBoxedLong by CallableIds.longCopyOfRange(compileLongAsBigint).functionSymbolOrNull()

    val isPrimitiveArray by run {
        val isBooleanArray by CallableIds.isBooleanArray.functionSymbol()
        val isByteArray by CallableIds.isByteArray.functionSymbol()
        val isShortArray by CallableIds.isShortArray.functionSymbol()
        val isCharArray by CallableIds.isCharArray.functionSymbol()
        val isIntArray by CallableIds.isIntArray.functionSymbol()
        val isFloatArray by CallableIds.isFloatArray.functionSymbol()
        val isLongArray by CallableIds.isLongArray(compileLongAsBigint).functionSymbolOrNull()
        val isDoubleArray by CallableIds.isDoubleArray.functionSymbol()
        lazy {
            mapOf(
                PrimitiveType.BOOLEAN to isBooleanArray,
                PrimitiveType.BYTE to isByteArray,
                PrimitiveType.SHORT to isShortArray,
                PrimitiveType.CHAR to isCharArray,
                PrimitiveType.INT to isIntArray,
                PrimitiveType.FLOAT to isFloatArray,
                PrimitiveType.LONG to isLongArray,
                PrimitiveType.DOUBLE to isDoubleArray,
            )
        }
    }

    // Enum

    val enumValueOfIntrinsic by CallableIds.enumValueOfIntrinsic.functionSymbol()
    val enumValuesIntrinsic by CallableIds.enumValuesIntrinsic.functionSymbol()
    val enumEntriesIntrinsic by CallableIds.enumEntriesIntrinsic.functionSymbol()

    // Other:

    override val jsCode by CallableIds.js.functionSymbol()
    val jsHashCode by CallableIds.hashCode.functionSymbol()
    val jsGetBooleanHashCode by CallableIds.getBooleanHashCode.functionSymbol()
    val jsGetNumberHashCode by CallableIds.getNumberHashCode.functionSymbol()
    val jsGetObjectHashCode by CallableIds.getObjectHashCode.functionSymbol()
    val jsGetStringHashCode by CallableIds.getStringHashCode.functionSymbol()
    val jsBigIntHashCode by CallableIds.bigIntHashCode.functionSymbol()
    val jsToString by CallableIds.toString.functionSymbol()
    val jsAnyToString by CallableIds.anyToString.functionSymbol()
    val jsCompareTo by CallableIds.compareTo.functionSymbol()
    val jsEquals by CallableIds.equals.functionSymbol()
    val jsNewTarget by CallableIds.jsNewTarget.functionSymbol()
    val jsEmptyObject by CallableIds.emptyObject.functionSymbol()
    val jsOpenInitializerBox by CallableIds.openInitializerBox.functionSymbol()

    val longEquals by CallableIds.equalsLong(compileLongAsBigint).functionSymbolOrNull()

    val jsImul by CallableIds.imul.functionSymbol()

    val jsUnreachableDeclarationLog by CallableIds.unreachableDeclarationLog.functionSymbol()
    val jsUnreachableDeclarationException by CallableIds.unreachableDeclarationException.functionSymbol()

    val jsNativeBoolean by CallableIds.nativeBoolean.functionSymbol()
    val jsBooleanInExternalLog by CallableIds.booleanInExternalLog.functionSymbol()
    val jsBooleanInExternalException by CallableIds.booleanInExternalException.functionSymbol()

    val jsNewAnonymousClass by CallableIds.jsNewAnonymousClass.functionSymbol()

    val longBoxedOne by CallableIds.longBoxedOne.propertySymbol()

    val jsGenerateInterfaceSymbol by CallableIds.jsGenerateInterfaceSymbol.functionSymbol()
    val generateInterfaceSymbolById by CallableIds.generateInterfaceSymbolById.functionSymbol()

    // Coroutines

    val jsYieldFunctionSymbol by CallableIds.jsYield.functionSymbol()
    val jsYieldStarFunctionSymbol by CallableIds.jsYieldStar.functionSymbol()

    val jsInvokeSuspendSuperType: IrSimpleFunctionSymbol by CallableIds.invokeSuspendSuperType.functionSymbol()
    val jsInvokeSuspendSuperTypeWithReceiver: IrSimpleFunctionSymbol by CallableIds.invokeSuspendSuperTypeWithReceiver.functionSymbol()
    val jsInvokeSuspendSuperTypeWithReceiverAndParam: IrSimpleFunctionSymbol by CallableIds.invokeSuspendSuperTypeWithReceiverAndParam.functionSymbol()

    val createCoroutineUnintercepted: Set<IrSimpleFunctionSymbol> by run {
        val list by CallableIds.createCoroutineUnintercepted.functionSymbols()
        lazy { list.toHashSet() }
    }
    val startCoroutineUninterceptedOrReturnNonGeneratorVersion: Set<IrSimpleFunctionSymbol> by run {
        val list by CallableIds.startCoroutineUninterceptedOrReturnNonGeneratorVersion.functionSymbols()
        lazy { list.toHashSet() }
    }

    val createCoroutineUninterceptedGeneratorVersion: Set<IrSimpleFunctionSymbol> by run {
        val list by CallableIds.createCoroutineUninterceptedGeneratorVersion.functionSymbols()
        lazy { list.toHashSet() }
    }
    val startCoroutineUninterceptedOrReturnGeneratorVersion: Set<IrSimpleFunctionSymbol> by run {
        val list by CallableIds.startCoroutineUninterceptedOrReturnGeneratorVersion.functionSymbols()
        lazy { list.toHashSet() }
    }

    val startCoroutineUninterceptedOrReturnGeneratorVersion1 by lazy(LazyThreadSafetyMode.NONE) {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.hasShape(extensionReceiver = true, regularParameters = 1) }
    }
    val startCoroutineUninterceptedOrReturnGeneratorVersion2 by lazy(LazyThreadSafetyMode.NONE) {
        startCoroutineUninterceptedOrReturnGeneratorVersion.single { it.owner.hasShape(extensionReceiver = true, regularParameters = 2) }
    }

    val awaitFunctionSymbol by CallableIds.await.functionSymbol()
    val promisifyFunctionSymbol by CallableIds.promisify.functionSymbol()
    val suspendOrReturnFunctionSymbol: IrSimpleFunctionSymbol by CallableIds.suspendOrReturn.functionSymbol()
    val orPromiseFunctionSymbol by CallableIds.orPromise.functionSymbol()
    val suspendLambdaRunFunctionSymbol by CallableIds.suspendLambdaRun.functionSymbol()

    val jsNumberRangeToNumber by CallableIds.numberRangeToNumber.functionSymbol()
    val jsNumberRangeToLong by CallableIds.numberRangeToLong.functionSymbol()
    val jsLongRangeToNumber by CallableIds.longRangeToNumber.functionSymbol()
    val jsLongRangeToLong by CallableIds.longRangeToLong.functionSymbol()

    val rangeUntilFunctions: Map<Pair<IrType, IrType>, IrSimpleFunctionSymbol> by CallableIds.until.functionSymbolAssociatedBy(
        condition = { it.hasShape(extensionReceiver = true, regularParameters = 1) },
        getKey = { it.parameters[0].type to it.parameters[1].type }
    )

    val promiseClassSymbol: IrClassSymbol = JsStandardClassIds.Promise.classSymbol()

    val longCompareToLong: IrSimpleFunctionSymbol? by CallableIds.compare(compileLongAsBigint).functionSymbolOrNull()

    val jsLongToString: IrSimpleFunctionSymbol by CallableIds.jsLongToString.functionSymbol()
    val longToStringImpl: IrSimpleFunctionSymbol by CallableIds.toStringImpl(compileLongAsBigint).functionSymbol()

    val isLongCompiledToBigInt: IrSimpleFunctionSymbol by CallableIds.isLongCompiledToBigInt.functionSymbol()

    val stringConstructorSymbol by StandardClassIds.String.primaryConstructorSymbol()

    val anyConstructorSymbol by StandardClassIds.Any.primaryConstructorSymbol()

    val kFunctionImplConstructorSymbol by ClassIds.KFunctionImpl.primaryConstructorSymbol()

    val jsObjectConstructorSymbol by JsStandardClassIds.JsObject.primaryConstructorSymbol()

    val unreachable by CallableIds.unreachable.functionSymbol()

    val jsArguments by CallableIds.jsArguments.functionSymbol()

    val jsEnsureNonNull by CallableIds.ensureNotNull.functionSymbol()

    // Arrays:
    val jsArrayLength by CallableIds.jsArrayLength.functionSymbol()
    val jsArrayGet by CallableIds.jsArrayGet.functionSymbol()
    val jsArraySet by CallableIds.jsArraySet.functionSymbol()

    val jsArrayIteratorFunction by CallableIds.arrayIterator.functionSymbol()

    val jsPrimitiveArrayIteratorFunctions: Map<PrimitiveType, IrSimpleFunctionSymbol> by run {
        val lazyValues = PrimitiveType.entries.map { "${it.typeName.asString().toLowerCaseAsciiOnly()}ArrayIterator".jsCallableId.functionSymbol() }
        lazy { PrimitiveType.entries.zip(lazyValues) { type, lazy -> type to lazy.value }.toMap() }
    }

    val jsClass by CallableIds.jsClassIntrinsic.functionSymbol()
    val arrayLiteral: IrSimpleFunctionSymbol by CallableIds.arrayLiteral.functionSymbol()

    // The following 3 functions are all lowered into [].slice.call(...), they only differ
    // in the number of arguments.
    // See https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
    val jsArrayLike2Array by CallableIds.jsArrayLike2Array.functionSymbol()
    val jsSliceArrayLikeFromIndex by CallableIds.jsSliceArrayLikeFromIndex.functionSymbol()
    val jsSliceArrayLikeFromIndexToIndex by CallableIds.jsSliceArrayLikeFromIndexToIndex.functionSymbol()

    internal inner class JsReflectionSymbols : ReflectionSymbols {
        override val createKType: IrSimpleFunctionSymbol by CallableIds.createKType.functionSymbol()
        override val createDynamicKType: IrSimpleFunctionSymbol by CallableIds.createDynamicKType.functionSymbol()
        override val createKTypeParameter: IrSimpleFunctionSymbol by CallableIds.createKTypeParameter.functionSymbol()
        override val getStarKTypeProjection: IrSimpleFunctionSymbol by CallableIds.getStarKTypeProjection.functionSymbol()
        override val createCovariantKTypeProjection: IrSimpleFunctionSymbol by CallableIds.createCovariantKTypeProjection.functionSymbol()
        override val createInvariantKTypeProjection: IrSimpleFunctionSymbol by CallableIds.createInvariantKTypeProjection.functionSymbol()
        override val createContravariantKTypeProjection: IrSimpleFunctionSymbol by CallableIds.createContravariantKTypeProjection.functionSymbol()
        override val getKClass: IrSimpleFunctionSymbol by CallableIds.getKClass.functionSymbol()
        override val getKClassFromExpression: IrSimpleFunctionSymbol by CallableIds.getKClassFromExpression.functionSymbol()
        override val kTypeClass: IrClassSymbol = StandardClassIds.KType.classSymbol()

        val kFunctionImpl: IrClassSymbol = ClassIds.KFunctionImpl.classSymbol()
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

    val primitiveToSizeConstructor: Map<PrimitiveType, IrSimpleFunctionSymbol> by run {
        val lazyValues = PrimitiveType.entries.map { type ->
            (primitiveToTypedArrayMap[type]?.let { "${it.toLowerCaseAsciiOnly()}Array".jsCallableId.functionSymbol() }
                ?: "${type.typeName.asString().toLowerCaseAsciiOnly()}Array".jsCallableId.functionSymbol())
        }
        lazy { PrimitiveType.entries.zip(lazyValues) { type, lazy -> type to lazy.value }.toMap() }
    }

    val primitiveToLiteralConstructor: Map<PrimitiveType, IrSimpleFunctionSymbol> by run {
        val lazyValues = PrimitiveType.entries.map { type ->
            (primitiveToTypedArrayMap[type]?.let { "${it.toLowerCaseAsciiOnly()}ArrayOf".jsCallableId.functionSymbol() }
                ?: "${type.typeName.asString().toLowerCaseAsciiOnly()}ArrayOf".jsCallableId.functionSymbol())
        }
        lazy { PrimitiveType.entries.zip(lazyValues) { type, lazy -> type to lazy.value }.toMap() }
    }

    val arrayConcat: IrSimpleFunctionSymbol by CallableIds.arrayConcat.functionSymbol()
    val primitiveArrayConcat: IrSimpleFunctionSymbol by CallableIds.primitiveArrayConcat.functionSymbol()
    val taggedArrayCopy: IrSimpleFunctionSymbol by CallableIds.taggedArrayCopy.functionSymbol()

    val jsArraySlice by CallableIds.slice.functionSymbol()

    val jsCall by CallableIds.jsCall.functionSymbol()
    val jsBind by CallableIds.jsBind.functionSymbol()

    val jsNameAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsName.classSymbol()
    val jsStaticAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsStatic.classSymbol()
    val jsExportAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsExport.classSymbol()

    val jsExportIgnoreAnnotationSymbol = JsStandardClassIds.Annotations.JsExportIgnore.classSymbol()

    val jsImplicitExportAnnotationSymbol: IrClassSymbol = JsStandardClassIds.Annotations.JsImplicitExport.classSymbol()

    val charSequenceLengthPropertyGetterSymbol by CallableIds.charSequenceClassLength.getterSymbol()
    val charSequenceGetFunctionSymbol by CallableIds.charSequenceClassGet.functionSymbol()
    val charSequenceSubSequenceFunctionSymbol by CallableIds.charSequenceClassSubSequence.functionSymbol()

    val jsCharSequenceGet by CallableIds.charSequenceGet.functionSymbol()
    val jsCharCodeAt by CallableIds.charCodeAt.functionSymbol()
    val jsCharSequenceLength by CallableIds.charSequenceLength.functionSymbol()
    val jsCharSequenceSubSequence by CallableIds.charSequenceSubSequence.functionSymbol()

    val jsContexfulRef by CallableIds.jsContextfulRef.functionSymbol()
    val jsBoxIntrinsic by CallableIds.boxIntrinsic.functionSymbol()
    val jsUnboxIntrinsic by CallableIds.unboxIntrinsic.functionSymbol()

    val captureStack by CallableIds.captureStack.functionSymbol()

    val linkageErrorSymbol by CallableIds.throwIrLinkageError.functionSymbol()

    val jsPrototypeOfSymbol by CallableIds.protoOf.functionSymbol()
    val jsDefinePropertySymbol by CallableIds.defineProp.functionSymbol()
    val jsObjectCreateSymbol by CallableIds.objectCreate.functionSymbol()
    val jsCreateThisSymbol by CallableIds.createThis.functionSymbol()
    val jsBoxApplySymbol by CallableIds.boxApply.functionSymbol()
    val jsCreateExternalThisSymbol by CallableIds.createExternalThis.functionSymbol()

    val throwableClass = StandardClassIds.Throwable.classSymbol()
    val primitiveCompanionObjects = primitivesWithImplicitCompanionObject().associateWith {
        ClassId(JsStandardClassIds.BASE_JS_INTERNAL_PACKAGE, Name.identifier("${it.identifier}CompanionObject")).classSymbol()
    }

    private fun primitivesWithImplicitCompanionObject(): List<Name> {
        val numbers = PrimitiveType.NUMBER_TYPES
            .filter { it.name != "LONG" && it.name != "CHAR" } // skip due to they have own explicit companions
            .map { it.typeName }

        return numbers + listOf(Name.identifier("String"), Name.identifier("Boolean"))
    }

    val newThrowableSymbol by CallableIds.newThrowable.functionSymbol()
    val extendThrowableSymbol by CallableIds.extendThrowable.functionSymbol()
    val setupCauseParameterSymbol by CallableIds.setupCauseParameter.functionSymbol()
    val setPropertiesToThrowableInstanceSymbol by CallableIds.setPropertiesToThrowableInstance.functionSymbol()

    val kpropertyBuilder by CallableIds.getPropertyCallableRef.functionSymbol()
    val klocalDelegateBuilder by CallableIds.getLocalDelegateReference.functionSymbol()

    val eagerInitialization: IrClassSymbol = ClassIds.EagerInitialization.classSymbol()

    val signatureIdSymbol by CallableIds.signatureIdIntrinsic.functionSymbol()

    val constructCallableReferenceSymbol by CallableIds.constructCallableReference.functionSymbol()

    val checkStaticInitializationState by CallableIds.checkStaticInitializationState.functionSymbol()
}

private object ClassIds {
    private val String.jsClassId get() = ClassId(JsStandardClassIds.BASE_JS_PACKAGE, Name.identifier(this))
    val FunctionAdapter = "FunctionAdapter".jsClassId
    val DefaultConstructorMarker = "DefaultConstructorMarker".jsClassId
    val EagerInitialization = "EagerInitialization".jsClassId

    // Coroutines classes
    private val String.coroutinesClassId get() = ClassId(StandardNames.COROUTINES_PACKAGE_FQ_NAME, Name.identifier(this))
    val coroutineImpl = "CoroutineImpl".coroutinesClassId
    val continuation = "Continuation".coroutinesClassId

    // Reflection
    val KFunctionImpl = ClassId(JsStandardClassIds.BASE_REFLECT_JS_INTERNAL_PACKAGE, Name.identifier("KFunctionImpl"))

    // Other
    val PrimitiveClasses = ClassId(JsStandardClassIds.BASE_REFLECT_JS_INTERNAL_PACKAGE, Name.identifier("PrimitiveClasses"))
    val EnumEntries = ClassId(StandardClassIds.BASE_ENUMS_PACKAGE, Name.identifier("EnumEntries"))
    val StringBuilder = ClassId(StandardClassIds.BASE_TEXT_PACKAGE, Name.identifier("StringBuilder"))
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
    val jsMethodReference = "jsMethodReference".jsCallableId
    val jsIsFunction = "jsIsFunction".jsCallableId
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
    val isLongCompiledToBigInt = "isLongCompiledToBigInt".jsCallableId
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
    val jsYieldStar = "jsYieldStar".jsCallableId
    val generateInterfaceSymbolById = "generateInterfaceSymbolById".jsCallableId
    val jsGenerateInterfaceSymbol = "jsGenerateInterfaceSymbol".jsCallableId
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
    val newThrowable = "newThrowable".jsCallableId
    val extendThrowable = "extendThrowable".jsCallableId
    val setupCauseParameter = "setupCauseParameter".jsCallableId
    val setPropertiesToThrowableInstance = "setPropertiesToThrowableInstance".jsCallableId
    val getPropertyCallableRef = "getPropertyCallableRef".jsCallableId
    val getLocalDelegateReference = "getLocalDelegateReference".jsCallableId
    val signatureIdIntrinsic = "signatureId".jsCallableId
    val constructCallableReference = "constructCallableReference".jsCallableId

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
    val orPromise = "orPromise".coroutinesCallableId
    val suspendLambdaRun = "suspendLambdaRun".coroutinesCallableId

    // Custom properties
    val coroutineState = CallableId(Name.identifier("state")).withClassId(ClassIds.coroutineImpl)
    val coroutineResult = CallableId(Name.identifier("result")).withClassId(ClassIds.coroutineImpl)
    val coroutineException = CallableId(Name.identifier("exception")).withClassId(ClassIds.coroutineImpl)
    val coroutineExceptionState = CallableId(Name.identifier("exceptionState")).withClassId(ClassIds.coroutineImpl)
    val charSequenceClassLength = CallableId(Name.identifier("length")).withClassId(StandardClassIds.CharSequence)
    val charSequenceClassGet = CallableId(Name.identifier("get")).withClassId(StandardClassIds.CharSequence)
    val charSequenceClassSubSequence = CallableId(Name.identifier("subSequence")).withClassId(StandardClassIds.CharSequence)

    // Others
    val coroutineSuspended = CallableId(StandardNames.COROUTINES_INTRINSICS_PACKAGE_FQ_NAME, StandardNames.COROUTINE_SUSPENDED_NAME)
    val subString = CallableId(StandardNames.TEXT_PACKAGE_FQ_NAME, Name.identifier("substring"))
    val until = CallableId(StandardNames.RANGES_PACKAGE_FQ_NAME, Name.identifier("until"))
    val throwIrLinkageError = CallableId(StandardClassIds.BASE_INTERNAL_PACKAGE, Name.identifier("throwIrLinkageError"))
    val enumEntries = CallableId(StandardClassIds.BASE_ENUMS_PACKAGE, Name.identifier("enumEntries"))
    val test = CallableId(StandardClassIds.BASE_TEST_PACKAGE, Name.identifier("test"))
    val suite = CallableId(StandardClassIds.BASE_TEST_PACKAGE, Name.identifier("suite"))
    val EmptyContinuation = CallableId(FqName.fromSegments(listOf("kotlin", "coroutines", "js", "internal")), Name.identifier("EmptyContinuation"))
    val checkStaticInitializationState = "checkStaticInitializationState".jsCallableId
}
