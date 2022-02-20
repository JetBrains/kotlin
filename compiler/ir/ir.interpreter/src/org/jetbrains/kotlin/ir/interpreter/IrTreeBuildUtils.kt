/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.interpreter.state.Complex
import org.jetbrains.kotlin.ir.interpreter.state.ExceptionState
import org.jetbrains.kotlin.ir.interpreter.state.Primitive
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.name.Name

internal val TEMP_CLASS_FOR_INTERPRETER = object : IrDeclarationOriginImpl("TEMP_CLASS_FOR_INTERPRETER") {}
internal val TEMP_FUNCTION_FOR_INTERPRETER = object : IrDeclarationOriginImpl("TEMP_FUNCTION_FOR_INTERPRETER") {}

fun Any?.toIrConstOrNull(irType: IrType, startOffset: Int = SYNTHETIC_OFFSET, endOffset: Int = SYNTHETIC_OFFSET): IrConst<*>? {
    if (this == null) return IrConstImpl.constNull(startOffset, endOffset, irType)

    val constType = irType.makeNotNull()
    return when (irType.getPrimitiveType()) {
        PrimitiveType.BOOLEAN -> IrConstImpl.boolean(startOffset, endOffset, constType, this as Boolean)
        PrimitiveType.CHAR -> IrConstImpl.char(startOffset, endOffset, constType, this as Char)
        PrimitiveType.BYTE -> IrConstImpl.byte(startOffset, endOffset, constType, (this as Number).toByte())
        PrimitiveType.SHORT -> IrConstImpl.short(startOffset, endOffset, constType, (this as Number).toShort())
        PrimitiveType.INT -> IrConstImpl.int(startOffset, endOffset, constType, (this as Number).toInt())
        PrimitiveType.FLOAT -> IrConstImpl.float(startOffset, endOffset, constType, (this as Number).toFloat())
        PrimitiveType.LONG -> IrConstImpl.long(startOffset, endOffset, constType, (this as Number).toLong())
        PrimitiveType.DOUBLE -> IrConstImpl.double(startOffset, endOffset, constType, (this as Number).toDouble())
        null -> when (constType.getUnsignedType()) {
            UnsignedType.UBYTE -> IrConstImpl.byte(startOffset, endOffset, constType, (this as Number).toByte())
            UnsignedType.USHORT -> IrConstImpl.short(startOffset, endOffset, constType, (this as Number).toShort())
            UnsignedType.UINT -> IrConstImpl.int(startOffset, endOffset, constType, (this as Number).toInt())
            UnsignedType.ULONG -> IrConstImpl.long(startOffset, endOffset, constType, (this as Number).toLong())
            null -> when {
                constType.isString() -> IrConstImpl.string(startOffset, endOffset, constType, this as String)
                else -> null
            }
        }
    }
}

fun Any?.toIrConst(irType: IrType, startOffset: Int = SYNTHETIC_OFFSET, endOffset: Int = SYNTHETIC_OFFSET): IrConst<*> =
    toIrConstOrNull(irType, startOffset, endOffset)
        ?: throw UnsupportedOperationException("Unsupported const element type ${irType.makeNotNull().render()}")

internal fun State.toIrExpression(expression: IrExpression): IrExpression {
    val start = expression.startOffset
    val end = expression.endOffset
    val type = expression.type.makeNotNull()
    return when (this) {
        is Primitive<*> ->
            when {
                this.value == null -> this.value.toIrConst(type, start, end)
                type.isPrimitiveType() || type.isString() -> this.value.toIrConst(type, start, end)
                else -> expression // TODO support for arrays
            }
        is ExceptionState -> {
            IrErrorExpressionImpl(expression.startOffset, expression.endOffset, expression.type, "\n" + this.getFullDescription())
        }
        is Complex -> {
            val stateType = this.irClass.defaultType
            when {
                stateType.isUnsignedType() -> (this.fields.values.single() as Primitive<*>).value.toIrConst(type, start, end)
                else -> expression
            }
        }
        else -> expression // TODO support
    }
}

internal fun IrFunction.createCall(origin: IrStatementOrigin? = null): IrCall {
    this as IrSimpleFunction
    return IrCallImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, returnType, symbol, typeParameters.size, valueParameters.size, origin)
}

internal fun IrConstructor.createConstructorCall(irType: IrType = returnType): IrConstructorCall {
    return IrConstructorCallImpl.fromSymbolOwner(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, irType, symbol)
}

internal fun IrValueDeclaration.createGetValue(): IrGetValue {
    return IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, this.type, this.symbol)
}

internal fun IrValueDeclaration.createTempVariable(): IrVariable {
    return IrVariableImpl(
        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, IrDeclarationOrigin.IR_TEMPORARY_VARIABLE, IrVariableSymbolImpl(),
        this.name, this.type, isVar = false, isConst = false, isLateinit = false
    )
}

internal fun IrClass.createGetObject(): IrGetObjectValue {
    return IrGetObjectValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, this.defaultType, this.symbol)
}

internal fun IrFunction.createReturn(value: IrExpression): IrReturn {
    return IrReturnImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, this.returnType, this.symbol, value)
}

internal fun createTempFunction(
    name: Name,
    type: IrType,
    origin: IrDeclarationOrigin = TEMP_FUNCTION_FOR_INTERPRETER,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
): IrSimpleFunction {
    return IrFactoryImpl.createFunction(
        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, origin, IrSimpleFunctionSymbolImpl(), name, visibility, Modality.FINAL, type,
        isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isOperator = true, isInfix = false, isExpect = false
    )
}

internal fun createTempClass(name: Name, origin: IrDeclarationOrigin = TEMP_CLASS_FOR_INTERPRETER): IrClass {
    return IrFactoryImpl.createClass(
        SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, origin, IrClassSymbolImpl(), name,
        ClassKind.CLASS, DescriptorVisibilities.PRIVATE, Modality.FINAL
    )
}

internal fun IrFunction.createGetField(): IrExpression {
    val backingField = (this as IrSimpleFunction).correspondingPropertySymbol?.owner?.backingField!!
    val receiver = dispatchReceiverParameter ?: extensionReceiverParameter
    return IrGetFieldImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, backingField.symbol, backingField.type, receiver?.createGetValue())
}

internal fun List<IrStatement>.wrapWithBlockBody(): IrBlockBody {
    return IrBlockBodyImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, this)
}

internal fun IrFunctionAccessExpression.shallowCopy(copyTypeArguments: Boolean = true): IrFunctionAccessExpression {
    return when (this) {
        is IrCall -> symbol.owner.createCall()
        is IrConstructorCall -> symbol.owner.createConstructorCall()
        is IrDelegatingConstructorCall -> IrDelegatingConstructorCallImpl.fromSymbolOwner(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, symbol)
        is IrEnumConstructorCall ->
            IrEnumConstructorCallImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, type, symbol, typeArgumentsCount, valueArgumentsCount)
        else -> TODO("Expression $this cannot be copied")
    }.apply {
        if (copyTypeArguments) {
            (0 until this@shallowCopy.typeArgumentsCount).forEach { this.putTypeArgument(it, this@shallowCopy.getTypeArgument(it)) }
        }
    }
}

internal fun IrBuiltIns.copyArgs(from: IrFunctionAccessExpression, into: IrFunctionAccessExpression) {
    into.dispatchReceiver = from.dispatchReceiver
    into.extensionReceiver = from.extensionReceiver
    (0 until from.valueArgumentsCount)
        .map { from.getValueArgument(it) }
        .forEachIndexed { i, arg ->
            into.putValueArgument(i, arg ?: IrConstImpl.constNull(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, this.anyNType))
        }
}

internal fun IrBuiltIns.irEquals(arg1: IrExpression, arg2: IrExpression): IrCall {
    val equalsCall = this.eqeqSymbol.owner.createCall(IrStatementOrigin.EQEQ)
    equalsCall.putValueArgument(0, arg1)
    equalsCall.putValueArgument(1, arg2)
    return equalsCall
}

internal fun IrBuiltIns.irIfNullThenElse(nullableArg: IrExpression, ifTrue: IrExpression, ifFalse: IrExpression): IrWhen {
    val nullCondition = this.irEquals(nullableArg, IrConstImpl.constNull(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, this.anyNType))
    val trueBranch = IrBranchImpl(nullCondition, ifTrue) // use default
    val elseBranch = IrElseBranchImpl(IrConstImpl.constTrue(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, this.booleanType), ifFalse)

    return IrIfThenElseImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, ifTrue.type).apply { branches += listOf(trueBranch, elseBranch) }
}

internal fun IrBuiltIns.emptyArrayConstructor(arrayType: IrType): IrConstructorCall {
    val arrayClass = arrayType.classOrNull!!.owner
    val constructor = arrayClass.constructors.firstOrNull { it.valueParameters.size == 1 } ?: arrayClass.constructors.first()
    val constructorCall = constructor.createConstructorCall(arrayType)

    constructorCall.putValueArgument(0, 0.toIrConst(this.intType))
    if (constructor.valueParameters.size == 2) {
        // TODO find a way to avoid creation of empty lambda
        val tempFunction = createTempFunction(Name.identifier("TempForVararg"), this.anyType)
        tempFunction.parent = arrayClass // can be anything, will not be used in any case
        val initLambda = IrFunctionExpressionImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, constructor.valueParameters[1].type, tempFunction, IrStatementOrigin.LAMBDA)
        constructorCall.putValueArgument(1, initLambda)
        constructorCall.putTypeArgument(0, (arrayType as IrSimpleType).arguments.singleOrNull()?.typeOrNull)
    }
    return constructorCall
}
