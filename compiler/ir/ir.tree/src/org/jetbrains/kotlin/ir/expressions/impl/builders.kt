/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.ir.util.isAssignable

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
)

fun IrCatchImpl(
    startOffset: Int,
    endOffset: Int,
    catchParameter: IrVariable,
    result: IrExpression,
) = IrCatchImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    catchParameter = catchParameter,
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
    value: IrConst<*>,
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
    kind: IrConstKind<T>,
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
    inlineCall: IrFunctionAccessExpression,
    inlinedElement: IrElement,
    origin: IrStatementOrigin? = null,
) = IrInlinedFunctionBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    inlineCall = inlineCall,
    inlinedElement = inlinedElement,
    origin = origin,
)

fun IrInlinedFunctionBlockImpl(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    inlineCall: IrFunctionAccessExpression,
    inlinedElement: IrElement,
    origin: IrStatementOrigin?,
    statements: List<IrStatement>,
) = IrInlinedFunctionBlockImpl(
    constructorIndicator = null,
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    inlineCall = inlineCall,
    inlinedElement = inlinedElement,
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
