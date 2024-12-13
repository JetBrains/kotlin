/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.symbols.impl.IrFunctionFakeOverrideSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.*

fun IrBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
) = IrBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)

fun IrBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
    statements: List<IrStatement>,
) = IrBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
).apply {
    this.statements.addAll(statements)
}

fun IrBranchImpl(
    startOffset: Int,
    endOffset: Int,
    condition: IrExpression,
    result: IrExpression,
) = IrBranchImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    condition = condition,
    result = result,
)

fun IrBranchImpl(
    condition: IrExpression,
    result: IrExpression,
) = IrBranchImpl(
    constructorIndicator = null,
    startOffset = condition.startOffset,
    endOffset = result.endOffset,
    condition = condition,
    result = result,
)

fun IrBreakImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    loop: IrLoop,
) = IrBreakImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    loop = loop,
)

fun IrCompositeImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
) = IrCompositeImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)

fun IrCatchImpl(
    startOffset: Int,
    endOffset: Int,
    catchParameter: IrVariable,
) = IrCatchImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    catchParameter = catchParameter,
    origin = null
)

fun IrCatchImpl(
    startOffset: Int,
    endOffset: Int,
    catchParameter: IrVariable,
    result: IrExpression,
    origin: IrStatementOrigin? = null
) = IrCatchImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    catchParameter = catchParameter,
    origin = origin
).apply {
    this.result = result
}

fun IrClassReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrClassifierSymbol,
    classType: IrType,
) = IrClassReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    classType = classType,
)

fun IrConstantArrayImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    initElements: List<IrConstantValue>,
) = IrConstantArrayImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
).apply {
    elements.addAll(initElements)
}

fun IrConstantObjectImpl(
    startOffset: Int,
    endOffset: Int,
    constructor: IrConstructorSymbol,
    initValueArguments: List<IrConstantValue>,
    initTypeArguments: List<IrType>,
    type: IrType = constructor.owner.constructedClassType,
) = IrConstantObjectImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    constructor = constructor,
    type = type,
).apply {
    valueArguments.addAll(initValueArguments)
    typeArguments.addAll(initTypeArguments)
}

fun IrConstantPrimitiveImpl(
    startOffset: Int,
    endOffset: Int,
    value: IrConst,
) = IrConstantPrimitiveImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    value = value,
    type = value.type,
)

fun <T> IrConstImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    kind: IrConstKind,
    value: T,
) = IrConstImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    kind = kind,
    value = value,
)

fun IrContinueImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    loop: IrLoop,
) = IrContinueImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    loop = loop,
)

fun IrDoWhileLoopImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
) = IrDoWhileLoopImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)

fun IrDynamicMemberExpressionImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    memberName: String,
    receiver: IrExpression,
) = IrDynamicMemberExpressionImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    memberName = memberName,
    receiver = receiver,
)

fun IrDynamicOperatorExpressionImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    operator: IrDynamicOperator,
) = IrDynamicOperatorExpressionImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    operator = operator,
)

fun IrElseBranchImpl(
    startOffset: Int,
    endOffset: Int,
    condition: IrExpression,
    result: IrExpression,
) = IrElseBranchImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    condition = condition,
    result = result,
)

fun IrElseBranchImpl(
    condition: IrExpression,
    result: IrExpression,
) = IrElseBranchImpl(
    constructorIndicator = null,
    startOffset = condition.startOffset,
    endOffset = result.endOffset,
    condition = condition,
    result = result,
)

fun IrErrorCallExpressionImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    description: String,
) = IrErrorCallExpressionImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    description = description,
)

fun IrErrorExpressionImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    description: String,
) = IrErrorExpressionImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    description = description,
)

fun IrFunctionExpressionImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    function: IrSimpleFunction,
    origin: IrStatementOrigin,
) = IrFunctionExpressionImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    function = function,
    origin = origin,
)

fun IrRichFunctionReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    reflectionTargetSymbol: IrFunctionSymbol?,
    overriddenFunctionSymbol: IrSimpleFunctionSymbol,
    invokeFunction: IrSimpleFunction,
    origin: IrStatementOrigin? = null,
    hasUnitConversion: Boolean = false,
    hasSuspendConversion: Boolean = false,
    hasVarargConversion: Boolean = false,
    isRestrictedSuspension: Boolean = false,
) = IrRichFunctionReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    reflectionTargetSymbol = reflectionTargetSymbol,
    overriddenFunctionSymbol = overriddenFunctionSymbol,
    invokeFunction = invokeFunction,
    origin = origin,
    hasUnitConversion = hasUnitConversion,
    hasSuspendConversion = hasSuspendConversion,
    hasVarargConversion = hasVarargConversion,
    isRestrictedSuspension = isRestrictedSuspension,
)

fun IrRichPropertyReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    reflectionTargetSymbol: IrDeclarationWithAccessorsSymbol?,
    getterFunction: IrSimpleFunction,
    setterFunction: IrSimpleFunction?,
    origin: IrStatementOrigin? = null
) = IrRichPropertyReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    reflectionTargetSymbol = reflectionTargetSymbol,
    getterFunction = getterFunction,
    setterFunction = setterFunction,
    origin = origin
)

fun IrGetClassImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    argument: IrExpression,
) = IrGetClassImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    argument = argument,
)

fun IrGetEnumValueImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrEnumEntrySymbol,
) = IrGetEnumValueImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
)

fun IrGetFieldImpl(
    startOffset: Int,
    endOffset: Int,
    symbol: IrFieldSymbol,
    type: IrType,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
) = IrGetFieldImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    symbol = symbol,
    type = type,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
)

fun IrGetFieldImpl(
    startOffset: Int,
    endOffset: Int,
    symbol: IrFieldSymbol,
    type: IrType,
    receiver: IrExpression?,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
) = IrGetFieldImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    symbol = symbol,
    type = type,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
).apply {
    this.receiver = receiver
}

fun IrGetObjectValueImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrClassSymbol,
) = IrGetObjectValueImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
)

fun IrGetValueImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrValueSymbol,
    origin: IrStatementOrigin? = null,
) = IrGetValueImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
)

fun IrGetValueImpl(
    startOffset: Int,
    endOffset: Int,
    symbol: IrValueSymbol,
    origin: IrStatementOrigin? = null,
) = IrGetValueImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = symbol.owner.type,
    symbol = symbol,
    origin = origin,
)

fun IrInlinedFunctionBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    inlineFunctionSymbol: IrFunctionSymbol?,
    fileEntry: IrFileEntry,
    origin: IrStatementOrigin? = null,
) = IrInlinedFunctionBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    inlineFunctionSymbol = inlineFunctionSymbol,
    fileEntry = fileEntry,
    origin = origin,
)

fun IrInlinedFunctionBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    inlineFunctionSymbol: IrFunctionSymbol?,
    fileEntry: IrFileEntry,
    origin: IrStatementOrigin?,
    statements: List<IrStatement>,
) = IrInlinedFunctionBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    inlineFunctionSymbol = inlineFunctionSymbol,
    fileEntry = fileEntry,
    origin = origin,
).apply {
    this.statements.addAll(statements)
}

fun IrInstanceInitializerCallImpl(
    startOffset: Int,
    endOffset: Int,
    classSymbol: IrClassSymbol,
    type: IrType,
) = IrInstanceInitializerCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    classSymbol = classSymbol,
    type = type,
)

fun IrRawFunctionReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrFunctionSymbol,
) = IrRawFunctionReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
)

fun IrReturnableBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrReturnableBlockSymbol,
    origin: IrStatementOrigin? = null,
) = IrReturnableBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
)

fun IrReturnableBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrReturnableBlockSymbol,
    origin: IrStatementOrigin?,
    statements: List<IrStatement>,
) = IrReturnableBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
).apply {
    this.statements.addAll(statements)
}

fun IrSetFieldImpl(
    startOffset: Int,
    endOffset: Int,
    symbol: IrFieldSymbol,
    type: IrType,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
) = IrSetFieldImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    symbol = symbol,
    type = type,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
)

fun IrSetFieldImpl(
    startOffset: Int, endOffset: Int,
    symbol: IrFieldSymbol,
    receiver: IrExpression?,
    value: IrExpression,
    type: IrType,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
) = IrSetFieldImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    symbol = symbol,
    type = type,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
).apply {
    this.receiver = receiver
    this.value = value
}

fun IrSetValueImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrValueSymbol,
    value: IrExpression,
    origin: IrStatementOrigin?,
): IrSetValueImpl {
    if (symbol.isBound) {
        assert(symbol.owner.isAssignable) { "Only assignable IrValues can be set" }
    }
    return IrSetValueImpl(
        constructorIndicator = null,
        startOffset = startOffset,
        endOffset = endOffset,
        type = type,
        symbol = symbol,
        value = value,
        origin = origin,
    )
}

fun IrSpreadElementImpl(
    startOffset: Int,
    endOffset: Int,
    expression: IrExpression,
) = IrSpreadElementImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    expression = expression,
)

fun IrStringConcatenationImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
) = IrStringConcatenationImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
)

fun IrStringConcatenationImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    arguments: Collection<IrExpression>,
) = IrStringConcatenationImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
).apply {
    this.arguments.addAll(arguments)
}

fun IrSuspendableExpressionImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    suspensionPointId: IrExpression,
    result: IrExpression,
) = IrSuspendableExpressionImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    suspensionPointId = suspensionPointId,
    result = result,
)

fun IrSuspensionPointImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    suspensionPointIdParameter: IrVariable,
    result: IrExpression,
    resumeResult: IrExpression,
) = IrSuspensionPointImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    suspensionPointIdParameter = suspensionPointIdParameter,
    result = result,
    resumeResult = resumeResult,
)

fun IrSyntheticBodyImpl(
    startOffset: Int,
    endOffset: Int,
    kind: IrSyntheticBodyKind,
) = IrSyntheticBodyImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    kind = kind,
)

fun IrThrowImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    value: IrExpression,
) = IrThrowImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    value = value,
)

fun IrTryImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
) = IrTryImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
)

fun IrTryImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    tryResult: IrExpression,
    catches: List<IrCatch>,
    finallyExpression: IrExpression?,
) = IrTryImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
).apply {
    this.tryResult = tryResult
    this.catches.addAll(catches)
    this.finallyExpression = finallyExpression
}

fun IrTypeOperatorCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    operator: IrTypeOperator,
    typeOperand: IrType,
    argument: IrExpression,
) = IrTypeOperatorCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    operator = operator,
    typeOperand = typeOperand,
    argument = argument,
)

fun IrVarargImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    varargElementType: IrType,
) = IrVarargImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    varargElementType = varargElementType,
)

fun IrVarargImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    varargElementType: IrType,
    elements: List<IrVarargElement>,
) = IrVarargImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    varargElementType = varargElementType,
).apply {
    this.elements.addAll(elements)
}

fun IrWhenImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin? = null,
) = IrWhenImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)

fun IrWhenImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
    branches: List<IrBranch>,
) = IrWhenImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
).apply {
    this.branches.addAll(branches)
}

fun IrWhileLoopImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    origin: IrStatementOrigin?,
) = IrWhileLoopImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
)

private fun IrFunctionSymbol.getRealOwner(): IrFunction {
    var symbol = this
    if (this is IrFunctionFakeOverrideSymbol) {
        symbol = originalSymbol
    }
    return symbol.owner
}

/**
 * Note: This functions requires [symbol] to be bound.
 * If it may be not, use [IrCallImplWithShape].
 */
fun IrCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrSimpleFunctionSymbol,
    typeArgumentsCount: Int = symbol.getRealOwner().typeParameters.size,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
): IrCallImpl = IrCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
).apply {
    initializeTargetShapeFromSymbol()
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Prefer [IrCallImpl], unless [symbol] may be unbound.
 */
fun IrCallImplWithShape(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrSimpleFunctionSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
    contextParameterCount: Int,
    hasDispatchReceiver: Boolean,
    hasExtensionReceiver: Boolean,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
): IrCallImpl = IrCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
    superQualifierSymbol = superQualifierSymbol,
).apply {
    initializeTargetShapeExplicitly(
        hasDispatchReceiver = hasDispatchReceiver,
        hasExtensionReceiver = hasExtensionReceiver,
        contextParameterCount = contextParameterCount,
        regularParameterCount = valueArgumentsCount - contextParameterCount
    )
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Note: This functions requires [symbol] to be bound.
 * If it may be not, use [IrConstructorCallImplWithShape].
 */
fun IrConstructorCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    constructorTypeArgumentsCount: Int,
    origin: IrStatementOrigin? = null,
    source: SourceElement = SourceElement.NO_SOURCE,
): IrConstructorCallImpl = IrConstructorCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
    constructorTypeArgumentsCount = constructorTypeArgumentsCount,
    source = source,
).apply {
    initializeTargetShapeFromSymbol()
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Prefer [IrConstructorCallImpl], unless [symbol] may be unbound.
 */
fun IrConstructorCallImplWithShape(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    constructorTypeArgumentsCount: Int,
    valueArgumentsCount: Int,
    contextParameterCount: Int,
    hasDispatchReceiver: Boolean,
    hasExtensionReceiver: Boolean,
    origin: IrStatementOrigin? = null,
    source: SourceElement = SourceElement.NO_SOURCE,
): IrConstructorCallImpl = IrConstructorCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
    constructorTypeArgumentsCount = constructorTypeArgumentsCount,
    source = source,
).apply {
    initializeTargetShapeExplicitly(
        hasDispatchReceiver = hasDispatchReceiver,
        hasExtensionReceiver = hasExtensionReceiver,
        contextParameterCount = contextParameterCount,
        regularParameterCount = valueArgumentsCount - contextParameterCount
    )
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Note: This functions requires [symbol] to be bound.
 * If it may be not, use [IrDelegatingConstructorCallImplWithShape].
 */
fun IrDelegatingConstructorCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    origin: IrStatementOrigin? = null,
): IrDelegatingConstructorCallImpl = IrDelegatingConstructorCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
).apply {
    initializeTargetShapeFromSymbol()
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Prefer [IrDelegatingConstructorCallImpl], unless [symbol] may be unbound.
 */
fun IrDelegatingConstructorCallImplWithShape(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
    contextParameterCount: Int,
    hasDispatchReceiver: Boolean,
    hasExtensionReceiver: Boolean,
    origin: IrStatementOrigin? = null,
): IrDelegatingConstructorCallImpl = IrDelegatingConstructorCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
).apply {
    initializeTargetShapeExplicitly(
        hasDispatchReceiver = hasDispatchReceiver,
        hasExtensionReceiver = hasExtensionReceiver,
        contextParameterCount = contextParameterCount,
        regularParameterCount = valueArgumentsCount - contextParameterCount
    )
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Note: This functions requires [symbol] to be bound.
 * If it may be not, use [IrEnumConstructorCallImplWithShape].
 */
fun IrEnumConstructorCallImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    origin: IrStatementOrigin? = null,
): IrEnumConstructorCallImpl = IrEnumConstructorCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
).apply {
    initializeTargetShapeFromSymbol()
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Prefer [IrEnumConstructorCallImpl], unless [symbol] may be unbound.
 */
fun IrEnumConstructorCallImplWithShape(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
    contextParameterCount: Int,
    hasDispatchReceiver: Boolean,
    hasExtensionReceiver: Boolean,
    origin: IrStatementOrigin? = null,
): IrEnumConstructorCallImpl = IrEnumConstructorCallImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    origin = origin,
).apply {
    initializeTargetShapeExplicitly(
        hasDispatchReceiver = hasDispatchReceiver,
        hasExtensionReceiver = hasExtensionReceiver,
        contextParameterCount = contextParameterCount,
        regularParameterCount = valueArgumentsCount - contextParameterCount
    )
    initializeEmptyTypeArguments(typeArgumentsCount)
}


/**
 * Note: This functions requires [symbol] to be bound.
 * If it may be not, use [IrFunctionReferenceImplWithShape].
 */
fun IrFunctionReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrFunctionSymbol,
    typeArgumentsCount: Int,
    reflectionTarget: IrFunctionSymbol? = symbol,
    origin: IrStatementOrigin? = null,
): IrFunctionReferenceImpl = IrFunctionReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
    symbol = symbol,
    reflectionTarget = reflectionTarget,
).apply {
    initializeTargetShapeFromSymbol()
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Prefer [IrFunctionReferenceImpl], unless [symbol] may be unbound.
 */
fun IrFunctionReferenceImplWithShape(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrFunctionSymbol,
    typeArgumentsCount: Int,
    valueArgumentsCount: Int,
    contextParameterCount: Int,
    hasDispatchReceiver: Boolean,
    hasExtensionReceiver: Boolean,
    reflectionTarget: IrFunctionSymbol? = symbol,
    origin: IrStatementOrigin? = null,
): IrFunctionReferenceImpl = IrFunctionReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    origin = origin,
    symbol = symbol,
    reflectionTarget = reflectionTarget,
).apply {
    initializeTargetShapeExplicitly(
        hasDispatchReceiver = hasDispatchReceiver,
        hasExtensionReceiver = hasExtensionReceiver,
        contextParameterCount = contextParameterCount,
        regularParameterCount = valueArgumentsCount - contextParameterCount
    )
    initializeEmptyTypeArguments(typeArgumentsCount)
}

fun IrLocalDelegatedPropertyReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrLocalDelegatedPropertySymbol,
    delegate: IrVariableSymbol,
    getter: IrSimpleFunctionSymbol,
    setter: IrSimpleFunctionSymbol?,
    origin: IrStatementOrigin? = null,
): IrLocalDelegatedPropertyReferenceImpl = IrLocalDelegatedPropertyReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    delegate = delegate,
    getter = getter,
    setter = setter,
    origin = origin,
).apply {
    initializeTargetShapeExplicitly(
        hasDispatchReceiver = false,
        hasExtensionReceiver = false,
        contextParameterCount = 0,
        regularParameterCount = 0,
    )
}

/**
 * Note: This functions requires [symbol] to be bound.
 * If it may be not, use [IrPropertyReferenceImplWithShape].
 */
fun IrPropertyReferenceImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrPropertySymbol,
    typeArgumentsCount: Int,
    field: IrFieldSymbol?,
    getter: IrSimpleFunctionSymbol?,
    setter: IrSimpleFunctionSymbol?,
    origin: IrStatementOrigin? = null,
): IrPropertyReferenceImpl = IrPropertyReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    field = field,
    getter = getter,
    setter = setter,
    origin = origin,
).apply {
    initializeTargetShapeFromSymbol()
    initializeEmptyTypeArguments(typeArgumentsCount)
}

/**
 * Prefer [IrPropertyReferenceImpl], unless [symbol] may be unbound.
 */
fun IrPropertyReferenceImplWithShape(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrPropertySymbol,
    hasDispatchReceiver: Boolean,
    hasExtensionReceiver: Boolean,
    typeArgumentsCount: Int,
    field: IrFieldSymbol?,
    getter: IrSimpleFunctionSymbol?,
    setter: IrSimpleFunctionSymbol?,
    origin: IrStatementOrigin? = null,
): IrPropertyReferenceImpl = IrPropertyReferenceImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    symbol = symbol,
    field = field,
    getter = getter,
    setter = setter,
    origin = origin,
).apply {
    initializeTargetShapeExplicitly(
        hasDispatchReceiver = hasDispatchReceiver,
        hasExtensionReceiver = hasExtensionReceiver,
        contextParameterCount = 0,
        regularParameterCount = 0,
    )
    initializeEmptyTypeArguments(typeArgumentsCount)
}


@ObsoleteDescriptorBasedAPI
fun IrCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrSimpleFunctionSymbol,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
): IrCallImpl {
    val descriptor = symbol.descriptor
    return IrCallImplWithShape(
        startOffset, endOffset, type, symbol,
        typeArgumentsCount = descriptor.typeParametersCount,
        valueArgumentsCount = descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size,
        contextParameterCount = descriptor.contextReceiverParameters.size,
        hasDispatchReceiver = descriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
        origin = origin,
        superQualifierSymbol = superQualifierSymbol,
    )
}

fun IrCallImpl.Companion.fromSymbolOwner(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrSimpleFunctionSymbol,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
): IrCallImpl =
    IrCallImpl(startOffset, endOffset, type, symbol, origin = origin, superQualifierSymbol = superQualifierSymbol)

fun IrCallImpl.Companion.fromSymbolOwner(
    startOffset: Int,
    endOffset: Int,
    symbol: IrSimpleFunctionSymbol,
): IrCallImpl =
    IrCallImpl(
        startOffset,
        endOffset,
        symbol.owner.returnType,
        symbol,
        origin = null,
        superQualifierSymbol = null
    )


@ObsoleteDescriptorBasedAPI
fun IrConstructorCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    constructorSymbol: IrConstructorSymbol,
    origin: IrStatementOrigin? = null,
): IrConstructorCallImpl {
    val constructorDescriptor = constructorSymbol.descriptor
    val classTypeParametersCount = constructorDescriptor.constructedClass.original.declaredTypeParameters.size
    val totalTypeParametersCount = constructorDescriptor.typeParameters.size
    val valueParametersCount = constructorDescriptor.valueParameters.size + constructorDescriptor.contextReceiverParameters.size
    return IrConstructorCallImplWithShape(
        startOffset, endOffset,
        type,
        constructorSymbol,
        typeArgumentsCount = totalTypeParametersCount,
        constructorTypeArgumentsCount = totalTypeParametersCount - classTypeParametersCount,
        valueArgumentsCount = valueParametersCount,
        contextParameterCount = constructorDescriptor.contextReceiverParameters.size,
        hasDispatchReceiver = constructorDescriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = constructorDescriptor.extensionReceiverParameter != null,
        origin = origin,
    )
}

fun IrConstructorCallImpl.Companion.fromSymbolOwner(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    constructorSymbol: IrConstructorSymbol,
    classTypeParametersCount: Int,
    origin: IrStatementOrigin? = null,
): IrConstructorCallImpl {
    val constructor = constructorSymbol.owner
    val constructorTypeParametersCount = constructor.typeParameters.size
    val totalTypeParametersCount = classTypeParametersCount + constructorTypeParametersCount

    return IrConstructorCallImpl(
        startOffset, endOffset,
        type,
        constructorSymbol,
        totalTypeParametersCount,
        constructorTypeParametersCount,
        origin = origin,
    )
}

fun IrConstructorCallImpl.Companion.fromSymbolOwner(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    constructorSymbol: IrConstructorSymbol,
    origin: IrStatementOrigin? = null,
): IrConstructorCallImpl {
    val constructedClass = constructorSymbol.owner.parentAsClass
    val classTypeParametersCount = constructedClass.typeParameters.size
    return fromSymbolOwner(startOffset, endOffset, type, constructorSymbol, classTypeParametersCount, origin)
}

fun IrConstructorCallImpl.Companion.fromSymbolOwner(
    type: IrType,
    constructorSymbol: IrConstructorSymbol,
    origin: IrStatementOrigin? = null,
): IrConstructorCallImpl =
    fromSymbolOwner(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, type, constructorSymbol, constructorSymbol.owner.parentAsClass.typeParameters.size,
        origin
    )


@ObsoleteDescriptorBasedAPI
fun IrEnumConstructorCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
): IrEnumConstructorCallImpl {
    val descriptor = symbol.descriptor
    return IrEnumConstructorCallImplWithShape(
        startOffset, endOffset, type, symbol,
        typeArgumentsCount = typeArgumentsCount,
        valueArgumentsCount = descriptor.valueParameters.size + descriptor.contextReceiverParameters.size,
        contextParameterCount = descriptor.contextReceiverParameters.size,
        hasDispatchReceiver = descriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
    )
}


@ObsoleteDescriptorBasedAPI
fun IrDelegatingConstructorCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
): IrDelegatingConstructorCallImpl {
    val descriptor = symbol.descriptor
    return IrDelegatingConstructorCallImplWithShape(
        startOffset, endOffset, type, symbol,
        typeArgumentsCount = descriptor.typeParametersCount,
        valueArgumentsCount = descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size,
        contextParameterCount = descriptor.contextReceiverParameters.size,
        hasDispatchReceiver = descriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
    )
}

@UnsafeDuringIrConstructionAPI
fun IrDelegatingConstructorCallImpl.Companion.fromSymbolOwner(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int = symbol.owner.allTypeParameters.size,
): IrDelegatingConstructorCallImpl =
    IrDelegatingConstructorCallImpl(startOffset, endOffset, type, symbol, typeArgumentsCount)


@ObsoleteDescriptorBasedAPI
fun IrFunctionReferenceImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrFunctionSymbol,
    reflectionTarget: IrFunctionSymbol?,
    origin: IrStatementOrigin? = null,
): IrFunctionReferenceImpl = IrFunctionReferenceImplWithShape(
    startOffset = startOffset, endOffset = endOffset,
    type = type,
    symbol = symbol,
    typeArgumentsCount = symbol.descriptor.typeParametersCount,
    valueArgumentsCount = symbol.descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size,
    contextParameterCount = symbol.descriptor.contextReceiverParameters.size,
    hasDispatchReceiver = symbol.descriptor.dispatchReceiverParameter != null,
    hasExtensionReceiver = symbol.descriptor.extensionReceiverParameter != null,
    reflectionTarget = reflectionTarget,
    origin = origin
)

fun IrFunctionReferenceImpl.Companion.fromSymbolOwner(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrFunctionSymbol,
    typeArgumentsCount: Int,
    reflectionTarget: IrFunctionSymbol?,
    origin: IrStatementOrigin? = null,
): IrFunctionReferenceImpl = IrFunctionReferenceImpl(
    startOffset, endOffset,
    type,
    symbol,
    typeArgumentsCount,
    reflectionTarget,
    origin
)
