/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("NOTHING_TO_INLINE")

package org.jetbrains.kotlin.bir.backend.builders

import org.jetbrains.kotlin.bir.backend.BirBackendContext
import org.jetbrains.kotlin.bir.backend.utils.listOfNulls
import org.jetbrains.kotlin.bir.declarations.*
import org.jetbrains.kotlin.bir.expressions.*
import org.jetbrains.kotlin.bir.expressions.impl.*
import org.jetbrains.kotlin.bir.resetWithNulls
import org.jetbrains.kotlin.bir.symbols.*
import org.jetbrains.kotlin.bir.types.BirType
import org.jetbrains.kotlin.bir.types.utils.defaultType
import org.jetbrains.kotlin.bir.types.utils.typeWith
import org.jetbrains.kotlin.bir.util.constructedClass
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator

context(BirBackendContext, BirStatementBuilderScope)
inline fun birNull(type: BirType = birBuiltIns.nothingNType) = BirConst.constNull(sourceSpan, type)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Boolean) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Byte) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Short) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Int) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Long) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Float) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Double) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: Char) = BirConst(value, sourceSpan)
context(BirBackendContext, BirStatementBuilderScope)
inline fun birConst(value: String) = BirConst(value, sourceSpan)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birConstantPrimitive(
    value: BirConst<*>,
    block: BirConstantPrimitive.() -> Unit = {},
): BirConstantPrimitive =
    BirConstantPrimitiveImpl(sourceSpan, value.type, value).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birGetUnit(): BirGetObjectValue = BirGetObjectValueImpl(sourceSpan, birBuiltIns.unitType, birBuiltIns.unitClass)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birGet(
    variable: BirValueDeclaration,
    type: BirType = variable.type,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirGetValue.() -> Unit = {},
): BirGetValue =
    BirGetValueImpl(sourceSpan, variable.type, variable.symbol, origin).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birSet(
    variable: BirValueDeclaration,
    value: BirExpression,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirSetValue.() -> Unit = {},
): BirSetValue =
    BirSetValueImpl(sourceSpan, birBuiltIns.unitType, variable.symbol, origin, value).apply(block)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birGetField(
    receiver: BirExpression?,
    field: BirField,
    type: BirType = field.type,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirGetField.() -> Unit = {},
): BirGetField =
    BirGetFieldImpl(sourceSpan, type, field.symbol, null, receiver, origin).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birSetField(
    receiver: BirExpression?,
    field: BirFieldSymbol,
    value: BirExpression,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirSetField.() -> Unit = {},
): BirSetField =
    BirSetFieldImpl(sourceSpan, birBuiltIns.unitType, field, null, receiver, origin, value).apply(block).apply(block)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birGetObjectValue(
    clazz: BirClassSymbol,
    type: BirType = clazz.defaultType,
): BirGetObjectValue =
    BirGetObjectValueImpl(sourceSpan, type, clazz)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birWhen(
    type: BirType,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirWhen.() -> Unit = {},
): BirWhen =
    BirWhenImpl(sourceSpan, type, origin).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birBranch(
    condition: BirExpression,
    result: BirExpression,
    block: BirBranch.() -> Unit = {},
): BirBranch =
    BirBranchImpl(sourceSpan, condition, result).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birElseBranch(
    result: BirExpression,
    block: BirElseBranch.() -> Unit = {},
): BirElseBranch =
    BirElseBranchImpl(sourceSpan, birConst(true), result).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birReturn(
    value: BirExpression,
    returnTarget: BirReturnTargetSymbol = this@BirStatementBuilderScope.returnTarget ?: error("return target not specified"),
    block: BirReturn.() -> Unit = {},
): BirReturn =
    BirReturnImpl(sourceSpan, birBuiltIns.nothingType, value, returnTarget).apply(block)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birCall(
    function: BirSimpleFunction,
    type: BirType = function.returnType,
    typeArguments: List<BirType?> = listOfNulls(function.typeParameters.size),
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirCall.() -> Unit = {},
): BirCall =
    BirCallImpl(
        sourceSpan = sourceSpan,
        type = type,
        symbol = function.symbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = origin,
        typeArguments = typeArguments,
        contextReceiversCount = 0,
        superQualifierSymbol = null
    ).apply {
        valueArguments.resetWithNulls(function.valueParameters.size)
        block()
    }

context(BirBackendContext, BirStatementBuilderScope)
inline fun birCall(
    constructor: BirConstructor,
    type: BirType = constructor.returnType,
    typeArguments: List<BirType?> = listOfNulls(constructor.constructedClass.typeParameters.size + constructor.typeParameters.size),
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirConstructorCall.() -> Unit = {},
): BirConstructorCall =
    BirConstructorCallImpl(
        sourceSpan = sourceSpan,
        type = type,
        symbol = constructor.symbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = origin,
        typeArguments = typeArguments,
        contextReceiversCount = 0,
        source = SourceElement.NO_SOURCE,
        constructorTypeArgumentsCount = 0
    ).apply {
        valueArguments.resetWithNulls(constructor.valueParameters.size)
        block()
    }

context(BirBackendContext, BirStatementBuilderScope)
inline fun birCallFunctionOrConstructor(
    callee: BirFunctionSymbol,
    type: BirType = callee.owner.returnType,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirFunctionAccessExpression.() -> Unit = {},
): BirFunctionAccessExpression = when (callee) {
    is BirConstructorSymbol -> birCall(callee.owner, type, origin = origin).apply(block)
    is BirSimpleFunctionSymbol -> birCall(callee.owner, type, origin = origin).apply(block)
    else -> error("Unhandled symbol type: " + callee.javaClass)
}

context(BirBackendContext, BirStatementBuilderScope)
inline fun birDelegatingConstructorCall(
    constructor: BirConstructor,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirDelegatingConstructorCall.() -> Unit = {},
): BirDelegatingConstructorCall =
    BirDelegatingConstructorCallImpl(
        sourceSpan = sourceSpan,
        type = birBuiltIns.unitType,
        symbol = constructor.symbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = origin,
        typeArguments = listOfNulls(constructor.constructedClass.typeParameters.size),
        contextReceiversCount = 0,
    )

context(BirBackendContext, BirStatementBuilderScope)
inline fun birCallGetter(
    property: BirProperty,
    type: BirType = property.getter!!.returnType,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirCall.() -> Unit = {},
): BirCall =
    BirCallImpl(
        sourceSpan = sourceSpan,
        type = type,
        symbol = property.getter!!.symbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = origin,
        typeArguments = emptyList(),
        contextReceiversCount = 0,
        superQualifierSymbol = null,
    ).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birCallGetter(
    getter: BirSimpleFunctionSymbol,
    type: BirType = getter.owner.returnType,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirCall.() -> Unit = {},
): BirCall =
    BirCallImpl(
        sourceSpan = sourceSpan,
        type = type,
        symbol = getter,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = origin,
        typeArguments = emptyList(),
        contextReceiversCount = 0,
        superQualifierSymbol = null,
    ).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birCallSetter(
    property: BirProperty,
    value: BirExpression,
    type: BirType = property.setter!!.returnType,
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirCall.() -> Unit = {},
): BirCall =
    BirCallImpl(
        sourceSpan = sourceSpan,
        type = type,
        symbol = property.setter!!.symbol,
        dispatchReceiver = null,
        extensionReceiver = null,
        origin = origin,
        typeArguments = emptyList(),
        contextReceiversCount = 0,
        superQualifierSymbol = null
    ).apply {
        valueArguments += value
    }


context(BirBackendContext, BirStatementBuilderScope)
inline fun birFunctionReference(
    function: BirFunction,
    type: BirType,
    typeArguments: List<BirType?> = listOfNulls(function.typeParameters.size),
    origin: IrStatementOrigin? = this@BirStatementBuilderScope.origin,
    block: BirFunctionReference.() -> Unit = {},
): BirFunctionReference =
    BirFunctionReferenceImpl(sourceSpan, type, null, null, origin, typeArguments, function.symbol, null).apply {
        valueArguments.resetWithNulls(function.valueParameters.size)
        block()
    }

context(BirBackendContext, BirStatementBuilderScope)
inline fun birRafFunctionReference(
    function: BirFunctionSymbol,
    type: BirType,
    block: BirRawFunctionReference.() -> Unit = {},
): BirRawFunctionReference =
    BirRawFunctionReferenceImpl(sourceSpan, type, function).apply(block)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birTypeOperator(
    argument: BirExpression,
    resultType: BirType,
    typeOperator: IrTypeOperator,
    typeOperand: BirType,
    block: BirTypeOperatorCall.() -> Unit = {},
): BirTypeOperatorCall =
    BirTypeOperatorCallImpl(sourceSpan, resultType, typeOperator, argument, typeOperand).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birIs(
    argument: BirExpression,
    type: BirType,
    block: BirTypeOperatorCall.() -> Unit = {},
): BirTypeOperatorCall =
    BirTypeOperatorCallImpl(sourceSpan, birBuiltIns.booleanType, IrTypeOperator.INSTANCEOF, argument, type).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birNotIs(
    argument: BirExpression,
    type: BirType,
    block: BirTypeOperatorCall.() -> Unit = {},
): BirTypeOperatorCall =
    BirTypeOperatorCallImpl(sourceSpan, birBuiltIns.booleanType, IrTypeOperator.NOT_INSTANCEOF, argument, type).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birCast(
    argument: BirExpression,
    targetType: BirType,
    castType: IrTypeOperator = IrTypeOperator.CAST,
    block: BirTypeOperatorCall.() -> Unit = {},
): BirTypeOperatorCall =
    BirTypeOperatorCallImpl(sourceSpan, targetType, castType, argument, targetType).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birSamConversion(
    argument: BirExpression,
    targetType: BirType,
    block: BirTypeOperatorCall.() -> Unit = {},
): BirTypeOperatorCall =
    BirTypeOperatorCallImpl(sourceSpan, targetType, IrTypeOperator.SAM_CONVERSION, argument, targetType).apply(block)


context(BirBackendContext, BirStatementBuilderScope)
inline fun birConcat(
    block: BirStringConcatenation.() -> Unit = {},
): BirStringConcatenation =
    BirStringConcatenationImpl(sourceSpan, birBuiltIns.stringType).apply(block)

context(BirBackendContext, BirStatementBuilderScope)
inline fun birVararg(
    elementType: BirType,
    block: BirVararg.() -> Unit = {},
): BirVararg =
    BirVarargImpl(sourceSpan, birBuiltIns.arrayClass.typeWith(elementType), elementType).apply(block)
