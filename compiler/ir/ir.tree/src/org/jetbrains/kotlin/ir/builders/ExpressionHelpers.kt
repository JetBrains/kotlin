/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.*

val IrBuilderWithScope.parent get() = scope.getLocalDeclarationParent()

inline fun IrBuilderWithScope.irLetS(
    value: IrExpression,
    origin: IrStatementOrigin? = null,
    nameHint: String? = null,
    irType: IrType? = null,
    body: (IrValueSymbol) -> IrExpression
): IrExpression {
    val irTemporary: IrVariable?
    val valueSymbol: IrValueSymbol
    if (value is IrGetValue && value.symbol.owner.isImmutable) {
        irTemporary = null
        valueSymbol = value.symbol
    } else {
        irTemporary = scope.createTemporaryVariable(value, nameHint, irType = irType)
        valueSymbol = irTemporary.symbol
    }
    val irResult = body(valueSymbol)
    if (irTemporary == null) return irResult
    val irBlock = IrBlockImpl(startOffset, endOffset, irResult.type, origin)
    irBlock.statements.add(irTemporary)
    if (irResult is IrStatementContainer) {
        irBlock.statements.addAll(irResult.statements)
    } else {
        irBlock.statements.add(irResult)
    }
    return irBlock
}

fun <T : IrElement> IrStatementsBuilder<T>.irTemporary(
    value: IrExpression? = null,
    nameHint: String? = null,
    irType: IrType = value?.type!!, // either value or irType should be supplied at callsite
    isMutable: Boolean = false,
    origin: IrDeclarationOrigin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE,
): IrVariable {
    val temporary = scope.createTemporaryVariableDeclaration(
        irType, nameHint, isMutable,
        startOffset = startOffset,
        endOffset = endOffset,
        origin = origin,
    )
    value?.let { temporary.initializer = it }
    +temporary
    return temporary
}

fun IrBuilder.irExprBody(value: IrExpression) =
    context.irFactory.createExpressionBody(startOffset, endOffset, value)

fun IrBuilder.irWhen(type: IrType, branches: List<IrBranch>) =
    IrWhenImpl(startOffset, endOffset, type, null, branches)

fun IrBuilderWithScope.irReturn(value: IrExpression) =
    IrReturnImpl(
        startOffset, endOffset,
        context.irBuiltIns.nothingType,
        scope.scopeOwnerSymbol as? IrReturnTargetSymbol
            ?: throw AssertionError("Function scope expected: ${scope.scopeOwnerSymbol.owner.render()}"),
        value
    )

fun IrBuilder.irBoolean(value: Boolean) =
    IrConstImpl(startOffset, endOffset, context.irBuiltIns.booleanType, IrConstKind.Boolean, value)

fun IrBuilder.irUnit() =
    irGetObjectValue(context.irBuiltIns.unitType, context.irBuiltIns.unitClass)

fun IrBuilder.irTrue() = irBoolean(true)
fun IrBuilder.irFalse() = irBoolean(false)
fun IrBuilderWithScope.irReturnTrue() = irReturn(irTrue())
fun IrBuilderWithScope.irReturnFalse() = irReturn(irFalse())
fun IrBuilderWithScope.irReturnUnit() = irReturn(irUnit())

fun IrBuilder.irBranch(condition: IrExpression, result: IrExpression) =
    IrBranchImpl(startOffset, endOffset, condition, result)

fun IrBuilder.irElseBranch(expression: IrExpression) =
    IrElseBranchImpl(startOffset, endOffset, irTrue(), expression)

fun IrBuilder.irIfThen(type: IrType, condition: IrExpression, thenPart: IrExpression, origin: IrStatementOrigin? = null) =
    IrWhenImpl(startOffset, endOffset, type, origin).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, condition, thenPart))
    }

fun IrBuilder.irIfThenElse(
    type: IrType,
    condition: IrExpression,
    thenPart: IrExpression,
    elsePart: IrExpression,
    origin: IrStatementOrigin? = null
) =
    IrWhenImpl(startOffset, endOffset, type, origin).apply {
        branches.add(IrBranchImpl(startOffset, endOffset, condition, thenPart))
        branches.add(irElseBranch(elsePart))
    }

fun IrBuilder.irIfThenMaybeElse(
    type: IrType,
    condition: IrExpression,
    thenPart: IrExpression,
    elsePart: IrExpression?,
    origin: IrStatementOrigin? = null
) =
    if (elsePart != null)
        irIfThenElse(type, condition, thenPart, elsePart, origin)
    else
        irIfThen(type, condition, thenPart, origin)

fun IrBuilder.irIfNull(type: IrType, subject: IrExpression, thenPart: IrExpression, elsePart: IrExpression) =
    irIfThenElse(type, irEqualsNull(subject), thenPart, elsePart)

fun IrBuilderWithScope.irIfThenReturnTrue(condition: IrExpression) =
    irIfThen(context.irBuiltIns.unitType, condition, irReturnTrue())

fun IrBuilderWithScope.irIfThenReturnFalse(condition: IrExpression) =
    irIfThen(context.irBuiltIns.unitType, condition, irReturnFalse())

fun IrBuilder.irGet(type: IrType, variable: IrValueSymbol) =
    IrGetValueImpl(startOffset, endOffset, type, variable)

fun IrBuilder.irGet(variable: IrValueDeclaration) = irGet(variable.type, variable.symbol)

fun IrBuilder.irGet(variable: IrValueDeclaration, type: IrType) = irGet(type, variable.symbol)

fun IrBuilder.irSet(variable: IrValueSymbol, value: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQ) =
    IrSetValueImpl(startOffset, endOffset, context.irBuiltIns.unitType, variable, value, origin)

fun IrBuilder.irSet(variable: IrValueDeclaration, value: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQ) =
    irSet(variable.symbol, value, origin)

fun IrBuilder.irGetField(receiver: IrExpression?, field: IrField, type: IrType = field.type) =
    IrGetFieldImpl(startOffset, endOffset, field.symbol, type, receiver)

fun IrBuilder.irSetField(receiver: IrExpression?, field: IrField, value: IrExpression, origin: IrStatementOrigin? = null) =
    IrSetFieldImpl(startOffset, endOffset, field.symbol, receiver, value, context.irBuiltIns.unitType, origin = origin)

fun IrBuilder.irGetObjectValue(type: IrType, classSymbol: IrClassSymbol) =
    IrGetObjectValueImpl(startOffset, endOffset, type, classSymbol)

fun IrBuilder.irEqeqeq(arg1: IrExpression, arg2: IrExpression) =
    context.eqeqeq(startOffset, endOffset, arg1, arg2)

fun IrBuilder.irEqeqeqWithoutBox(arg1: IrExpression, arg2: IrExpression) =
    primitiveOp2(
        startOffset,
        endOffset,
        context.irBuiltIns.eqeqeqSymbol,
        context.irBuiltIns.booleanType,
        IrStatementOrigin.SYNTHETIC_NOT_AUTOBOXED_CHECK,
        arg1,
        arg2
    )

fun IrBuilder.irNull() =
    irNull(context.irBuiltIns.nothingNType)

fun IrBuilder.irNull(irType: IrType) =
    IrConstImpl.constNull(startOffset, endOffset, irType)

fun IrBuilder.irEqualsNull(argument: IrExpression) =
    irEquals(argument, irNull())

fun IrBuilder.irEquals(arg1: IrExpression, arg2: IrExpression, origin: IrStatementOrigin = IrStatementOrigin.EQEQ) =
    primitiveOp2(
        startOffset, endOffset, context.irBuiltIns.eqeqSymbol, context.irBuiltIns.booleanType, origin,
        arg1, arg2
    )

fun IrBuilder.irNotEquals(arg1: IrExpression, arg2: IrExpression) =
    primitiveOp1(
        startOffset, endOffset, context.irBuiltIns.booleanNotSymbol, context.irBuiltIns.booleanType, IrStatementOrigin.EXCLEQ,
        irEquals(arg1, arg2, origin = IrStatementOrigin.EXCLEQ)
    )

fun IrBuilder.irGet(type: IrType, receiver: IrExpression?, getterSymbol: IrFunctionSymbol): IrCall =
    IrCallImpl(
        startOffset, endOffset,
        type,
        getterSymbol as IrSimpleFunctionSymbol,
        typeArgumentsCount = getterSymbol.owner.typeParameters.size,
        origin = IrStatementOrigin.GET_PROPERTY
    ).apply {
        if (receiver != null) {
            arguments[0] = receiver
        }
    }

fun IrBuilder.irSet(type: IrType, receiver: IrExpression?, setterSymbol: IrFunctionSymbol, value: IrExpression): IrCall =
    IrCallImpl(
        startOffset, endOffset,
        type,
        setterSymbol as IrSimpleFunctionSymbol,
        typeArgumentsCount = setterSymbol.owner.typeParameters.size,
        origin = IrStatementOrigin.EQ
    ).apply {
        if (receiver != null) {
            arguments[0] = receiver
            arguments[1] = value
        } else {
            arguments[0] = value
        }
    }

fun IrBuilder.irCall(
    callee: IrFunctionSymbol,
    type: IrType,
    typeArguments: List<IrType>
): IrMemberAccessExpression<*> =
    irCall(callee, type).apply {
        typeArguments.forEachIndexed { index, irType ->
            this.typeArguments[index] = irType
        }
    }

fun IrBuilder.irAnnotation(callee: IrConstructorSymbol, typeArguments: List<IrType>): IrAnnotation =
    IrAnnotationImpl.fromSymbolOwner(
        startOffset,
        endOffset,
        callee.owner.returnType,
        callee,
        typeArguments.size - callee.owner.typeParameters.size
    ).apply {
        typeArguments.forEachIndexed { index, irType ->
            this.typeArguments[index] = irType
        }
    }

fun IrBuilder.irCallConstructor(callee: IrConstructorSymbol, typeArguments: List<IrType>): IrConstructorCall =
    IrConstructorCallImpl.fromSymbolOwner(
        startOffset,
        endOffset,
        callee.owner.returnType,
        callee,
        typeArguments.size - callee.owner.typeParameters.size
    ).apply {
        typeArguments.forEachIndexed { index, irType ->
            this.typeArguments[index] = irType
        }
    }

fun IrBuilder.irCall(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    typeArgumentsCount: Int = callee.owner.typeParameters.size,
    origin: IrStatementOrigin? = null
): IrCall =
    IrCallImpl(
        startOffset, endOffset, type, callee,
        typeArgumentsCount = typeArgumentsCount,
        origin = origin
    )

fun IrBuilder.irAnnotation(
    callee: IrConstructorSymbol,
    type: IrType,
    constructedClass: IrClass = callee.owner.parentAsClass,
): IrAnnotation =
    IrAnnotationImpl(
        startOffset,
        endOffset,
        type,
        callee,
        typeArgumentsCount = callee.owner.typeParameters.size + constructedClass.typeParameters.size,
        constructorTypeArgumentsCount = callee.owner.typeParameters.size
    )

fun IrBuilder.irCall(
    callee: IrConstructorSymbol,
    type: IrType,
    constructedClass: IrClass = callee.owner.parentAsClass
): IrConstructorCall =
    IrConstructorCallImpl(
        startOffset, endOffset, type, callee,
        typeArgumentsCount = callee.owner.typeParameters.size + constructedClass.typeParameters.size,
        constructorTypeArgumentsCount = callee.owner.typeParameters.size
    )

fun IrBuilder.irCall(callee: IrFunctionSymbol, type: IrType): IrFunctionAccessExpression =
    when (callee) {
        is IrConstructorSymbol -> irCall(callee, type)
        is IrSimpleFunctionSymbol -> irCall(callee, type)
    }

fun IrBuilder.irCall(callee: IrSimpleFunctionSymbol): IrCall =
    irCall(callee, callee.owner.returnType)

fun IrBuilder.irAnnotation(callee: IrConstructorSymbol): IrAnnotation =
    irAnnotation(callee, callee.owner.returnType)

fun IrBuilder.irCall(callee: IrConstructorSymbol): IrConstructorCall =
    irCall(callee, callee.owner.returnType)

fun IrBuilder.irCall(callee: IrFunctionSymbol): IrFunctionAccessExpression =
    irCall(callee, callee.owner.returnType)

fun IrBuilder.irCall(callee: IrFunction): IrFunctionAccessExpression =
    irCall(callee.symbol)

fun IrBuilder.irCall(callee: IrFunction, origin: IrStatementOrigin? = null, superQualifierSymbol: IrClassSymbol? = null): IrCall =
    IrCallImpl(
        startOffset, endOffset, callee.returnType,
        callee.symbol as IrSimpleFunctionSymbol,
        callee.typeParameters.size,
        origin, superQualifierSymbol
    )

fun IrBuilder.irCallWithSubstitutedType(callee: IrFunction, typeArguments: List<IrType>): IrMemberAccessExpression<*> {
    val argsMap = callee.typeParameters.map { it.symbol }.zip(typeArguments).toMap()
    return irCall(callee.symbol, callee.returnType.substitute(argsMap), typeArguments)
}

fun IrBuilder.irCallWithSubstitutedType(callee: IrFunctionSymbol, typeArguments: List<IrType>): IrMemberAccessExpression<*> {
    return irCallWithSubstitutedType(callee.owner, typeArguments)
}

fun IrBuilder.irDelegatingConstructorCall(callee: IrConstructor): IrDelegatingConstructorCall =
    IrDelegatingConstructorCallImpl(
        startOffset, endOffset, context.irBuiltIns.unitType, callee.symbol,
        callee.parentAsClass.typeParameters.size
    )

fun IrBuilder.irCallOp(
    callee: IrSimpleFunctionSymbol,
    type: IrType,
    dispatchReceiver: IrExpression,
    argument: IrExpression? = null,
    origin: IrStatementOrigin? = null
): IrMemberAccessExpression<*> =
    irCall(callee, type, typeArgumentsCount = 0, origin = origin).apply {
        arguments[0] = dispatchReceiver
        if (argument != null)
            arguments[1] = argument
    }

fun IrBuilder.typeOperator(
    resultType: IrType,
    argument: IrExpression,
    typeOperator: IrTypeOperator,
    typeOperand: IrType
) =
    IrTypeOperatorCallImpl(startOffset, endOffset, resultType, typeOperator, typeOperand, argument)

fun IrBuilder.irIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.INSTANCEOF, type)

fun IrBuilder.irNotIs(argument: IrExpression, type: IrType) =
    typeOperator(context.irBuiltIns.booleanType, argument, IrTypeOperator.NOT_INSTANCEOF, type)

fun IrBuilder.irAs(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.CAST, type, argument)

fun IrBuilder.irImplicitCast(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.IMPLICIT_CAST, type, argument)

fun IrBuilder.irReinterpretCast(argument: IrExpression, type: IrType) =
    IrTypeOperatorCallImpl(startOffset, endOffset, type, IrTypeOperator.REINTERPRET_CAST, type, argument)

fun IrBuilder.irSamConversion(argument: IrExpression, type: IrType) =
    typeOperator(type, argument, IrTypeOperator.SAM_CONVERSION, type)

fun IrBuilder.irByte(value: Byte) =
    IrConstImpl.byte(startOffset, endOffset, context.irBuiltIns.byteType, value)

fun IrBuilder.irShort(value: Short) =
    IrConstImpl.short(startOffset, endOffset, context.irBuiltIns.shortType, value)

fun IrBuilder.irInt(value: Int, type: IrType = context.irBuiltIns.intType) =
    IrConstImpl.int(startOffset, endOffset, type, value)

fun IrBuilder.irLong(value: Long, type: IrType = context.irBuiltIns.longType) =
    IrConstImpl.long(startOffset, endOffset, type, value)

fun IrBuilder.irChar(value: Char) =
    IrConstImpl.char(startOffset, endOffset, context.irBuiltIns.charType, value)

fun IrBuilder.irString(value: String) =
    IrConstImpl.string(startOffset, endOffset, context.irBuiltIns.stringType, value)

fun IrBuilder.irConcat() =
    IrStringConcatenationImpl(startOffset, endOffset, context.irBuiltIns.stringType)

fun IrBuilder.irVararg(elementType: IrType, values: List<IrExpression>) =
    IrVarargImpl(startOffset, endOffset, context.irBuiltIns.arrayClass.typeWith(elementType), elementType, values)

fun IrBuilder.irRawFunctionReference(type: IrType, symbol: IrFunctionSymbol) =
    IrRawFunctionReferenceImpl(startOffset, endOffset, type, symbol)

fun IrBuilder.irTry(type: IrType, tryResult: IrExpression, catches: List<IrCatch>, finallyExpression: IrExpression?) =
    IrTryImpl(startOffset, endOffset, type, tryResult, catches, finallyExpression)

inline fun IrBuilderWithScope.irBlock(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    body: IrBlockBuilder.() -> Unit
): IrContainerExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType
    ).block(body)

inline fun IrBuilderWithScope.irBlockOrSingleExpression(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    body: IrBlockBuilder.() -> Unit
): IrExpression =
    irBlock(startOffset, endOffset, origin, resultType, body).let {
        it.statements.singleOrNull() as? IrExpression ?: it
    }

inline fun IrBuilderWithScope.irComposite(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    origin: IrStatementOrigin? = null,
    resultType: IrType? = null,
    body: IrBlockBuilder.() -> Unit
): IrExpression =
    IrBlockBuilder(
        context, scope,
        startOffset,
        endOffset,
        origin, resultType, true
    ).block(body)

inline fun IrBuilderWithScope.irBlockBody(
    startOffset: Int = this.startOffset,
    endOffset: Int = this.endOffset,
    body: IrBlockBodyBuilder.() -> Unit
): IrBlockBody =
    IrBlockBodyBuilder(
        context, scope,
        startOffset,
        endOffset
    ).blockBody(body)

fun IrBuilder.irConstantPrimitive(value: IrConst) =
    IrConstantPrimitiveImpl(startOffset, endOffset, value)

fun IrBuilder.irConstantArray(type: IrType, elements: List<IrConstantValue>) =
    IrConstantArrayImpl(
        startOffset, endOffset,
        type,
        elements
    )

fun IrBuilder.irConstantObject(
    constructor: IrConstructorSymbol,
    arguments: List<IrConstantValue>,
    typeArguments: List<IrType> = emptyList()
): IrConstantValue {
    return IrConstantObjectImpl(
        startOffset, endOffset,
        constructor,
        arguments,
        typeArguments,
    )
}

fun IrBuilder.irConstantObject(
    clazz: IrClass,
    arguments: List<IrConstantValue>,
    typeArguments: List<IrType> = emptyList()
): IrConstantValue {
    return irConstantObject(clazz.primaryConstructor?.symbol!!, arguments, typeArguments)
}

fun IrBuilder.irConstantObject(
    clazz: IrClass,
    elements: Map<String, IrConstantValue>,
    typeArguments: List<IrType> = emptyList()
): IrConstantValue {
    return irConstantObject(
        clazz,
        clazz.primaryConstructor!!.symbol.owner.parameters.also {
            require(it.size == elements.size) {
                "Wrong number of values provided for ${clazz.name} construction: ${elements.size} instead of ${it.size}"
            }
        }.map {
            elements[it.name.asString()] ?: error("No value for field named ${it.name} provided")
        },
        typeArguments
    )
}

fun IrBuilder.irRichFunctionReference(
    invokeFunction: IrSimpleFunction,
    superType: IrType,
    reflectionTargetSymbol: IrFunctionSymbol?,
    overriddenFunctionSymbol: IrSimpleFunctionSymbol,
    captures: List<IrExpression>,
    origin: IrStatementOrigin?,
): IrRichFunctionReferenceImpl = IrRichFunctionReferenceImpl(
    startOffset = startOffset,
    endOffset = endOffset,
    type = superType,
    reflectionTargetSymbol = reflectionTargetSymbol,
    overriddenFunctionSymbol = overriddenFunctionSymbol,
    invokeFunction = invokeFunction,
    origin = origin
).apply {
    boundValues += captures
}