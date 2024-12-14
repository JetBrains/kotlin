/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.lower.loops

import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrReturnTarget
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.isSubtypeOfClass
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

/** Return the negated value if the expression is const, otherwise call unaryMinus(). */
internal fun IrExpression.negate(): IrExpression {
    return when (val value = (this as? IrConst)?.value as? Number) {
        is Int -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, -value)
        is Long -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, -value)
        else -> {
            // This expression's type could be Nothing from an exception throw, in which case the unary minus function will not exist.
            if (type.isNothing()) return this

            val unaryMinusFun = type.getClass()!!.functions.single {
                it.name == OperatorNameConventions.UNARY_MINUS &&
                        it.hasShape(dispatchReceiver = true)
            }
            IrCallImpl(
                startOffset, endOffset, unaryMinusFun.returnType,
                unaryMinusFun.symbol,
                typeArgumentsCount = 0
            ).apply {
                dispatchReceiver = this@negate
            }.implicitCastIfNeededTo(type)
        }
    }
}

/** Return `this - 1` if the expression is const, otherwise call dec(). */
internal fun IrExpression.decrement(): IrExpression {
    return when (val thisValue = (this as? IrConst)?.value) {
        is Int -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Int, thisValue - 1)
        is Long -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Long, thisValue - 1)
        is Char -> IrConstImpl(startOffset, endOffset, type, IrConstKind.Char, thisValue - 1)
        else -> {
            val decFun = type.getClass()!!.functions.single {
                it.name == OperatorNameConventions.DEC &&
                        it.hasShape(dispatchReceiver = true)
            }
            IrCallImpl(
                startOffset, endOffset, type,
                decFun.symbol,
                typeArgumentsCount = 0
            ).apply {
                dispatchReceiver = this@decrement
            }
        }
    }
}

internal val IrExpression.canChangeValueDuringExecution: Boolean
    get() = when (this) {
        is IrGetValue ->
            !this.symbol.owner.isImmutable
        is IrConst,
        is IrGetObjectValue,
        is IrGetEnumValue ->
            false
        else ->
            true
    }

internal val IrExpression.canHaveSideEffects: Boolean
    get() = !isTrivial()

internal val IrExpression.constLongValue: Long?
    get() = when {
        this !is IrConst -> null
        type.isUByte() -> (value as? Number)?.toLong()?.toUByte()?.toLong()
        type.isUShort() -> (value as? Number)?.toLong()?.toUShort()?.toLong()
        type.isUInt() -> (value as? Number)?.toLong()?.toUInt()?.toLong()
        type.isChar() -> (value as? Char)?.code?.toLong()
        else -> (value as? Number)?.toLong()
    }

/**
 * If [expression] can have side effects ([IrExpression.canHaveSideEffects]), this function creates a temporary local variable for that
 * expression and returns that variable and an [IrGetValue] for it. Otherwise, it returns no variable and [expression].
 *
 * This helps reduce local variable usage.
 */
internal fun DeclarationIrBuilder.createTemporaryVariableIfNecessary(
    expression: IrExpression,
    nameHint: String? = null,
    irType: IrType? = null,
    isMutable: Boolean = false
): Pair<IrVariable?, IrExpression> =
    if (expression.canHaveSideEffects) {
        scope.createTmpVariable(expression, nameHint = nameHint, irType = irType, isMutable = isMutable).let { Pair(it, irGet(it)) }
    } else {
        Pair(null, expression)
    }

/**
 * If [expression] can change value during execution ([IrExpression.canChangeValueDuringExecution]),
 * this function creates a temporary local variable for that expression and returns that variable and an [IrGetValue] for it.
 * Otherwise, it returns no variable and [expression].
 * Note that a variable expression doesn't have side effects per se, but can change value during execution,
 * so if it's denotes a value that would be used in a loop (say, a loop bound), it should be cached in a temporary at the loop header.
 *
 * This helps reduce local variable usage.
 */
internal fun DeclarationIrBuilder.createLoopTemporaryVariableIfNecessary(
    expression: IrExpression,
    nameHint: String? = null,
    irType: IrType? = null,
    isMutable: Boolean = false
): Pair<IrVariable?, IrExpression> =
    if (expression.canChangeValueDuringExecution) {
        scope.createTmpVariable(expression, nameHint = nameHint, irType = irType, isMutable = isMutable).let { Pair(it, irGet(it)) }
    } else {
        Pair(null, expression)
    }

// `this` may be of nullable type, while targetClass is assumed to be non-nullable
internal fun IrExpression.castIfNecessary(targetClass: IrClass) =
    when {
        // This expression's type could be Nothing from an exception throw.
        type == targetClass.defaultType || type.isNothing() -> this
        this is IrConst && targetClass.defaultType.isPrimitiveType() -> {
            val targetType = targetClass.defaultType
            val longOrSmallerValue = constLongValue
            val uLongValue = if (targetType.isULong()) longOrSmallerValue!!.toULong() else null
            when (targetType.getPrimitiveType()) {
                PrimitiveType.BYTE -> IrConstImpl.byte(startOffset, endOffset, targetType, longOrSmallerValue!!.toByte())
                PrimitiveType.SHORT -> IrConstImpl.short(startOffset, endOffset, targetType, longOrSmallerValue!!.toShort())
                PrimitiveType.INT -> IrConstImpl.int(startOffset, endOffset, targetType, longOrSmallerValue!!.toInt())
                PrimitiveType.LONG -> IrConstImpl.long(startOffset, endOffset, targetType, longOrSmallerValue!!.toLong())
                PrimitiveType.FLOAT -> {
                    val floatValue = when (val value = value) {
                        is Float -> value
                        is Double -> value.toFloat()
                        else -> uLongValue?.toFloat() ?: longOrSmallerValue?.toFloat()
                    }
                    IrConstImpl.float(startOffset, endOffset, targetType, floatValue!!)
                }
                PrimitiveType.DOUBLE -> {
                    val doubleValue = when (val value = value) {
                        is Float -> value.toDouble()
                        is Double -> value
                        else -> uLongValue?.toDouble() ?: longOrSmallerValue?.toDouble()
                    }
                    IrConstImpl.double(startOffset, endOffset, targetType, doubleValue!!)
                }
                else -> error("Cannot cast expression of type ${type.render()} to ${targetType.render()}")
            }
        }
        else -> {
            val classifier = (type as IrSimpleType).classifier
            // Nullable type is not a subtype of assumed non-nullable type of targetClass
            // KT-68888: TODO `!type.isNullable()` can(?) be removed to get rid of conversion from nullable to non-nullable
            if (!type.isNullable() && classifier.isSubtypeOfClass(targetClass.symbol)) this
            else {
                makeIrCallConversionToTargetClass(classifier.closestSuperClass()!!.owner, targetClass)
            }
        }
    }

// For a class, returns `this`,
// For an interface, returns null,
// For a type parameter, finds nearest class ancestor.
internal fun IrClassifierSymbol.closestSuperClass(): IrClassSymbol? =
    if (this is IrClassSymbol)
        if (owner.isInterface) null
        else this
    else
        superTypes().mapNotNull { it.classifierOrFail.closestSuperClass() }.singleOrNull()

private fun IrExpression.makeIrCallConversionToTargetClass(
    sourceClass: IrClass,
    targetClass: IrClass,
): IrExpression {
    val numberCastFunctionName = Name.identifier("to${targetClass.name}")
    val castFun = sourceClass.functions.singleOrNull {
        it.name == numberCastFunctionName &&
                it.hasShape(dispatchReceiver = true)
    } ?: error("Internal error: cannot convert ${sourceClass.name} to ${targetClass.name}: ${render()}")

    return IrCallImpl(
        startOffset, endOffset,
        castFun.returnType, castFun.symbol,
        typeArgumentsCount = 0
    ).also { it.dispatchReceiver = this }
}

// Gets type of the deepest EXPRESSION from possible recursive snippet `IrGetValue(IrVariable(initializer=EXPRESSION))`.
// In case it cannot be unwrapped, just returns back type of `expression` parameter
internal fun IrExpression.getMostPreciseTypeFromValInitializer(): IrType {
    fun unwrapValInitializer(expression: IrExpression): IrExpression? {
        val irVariable = (expression as? IrGetValue)?.symbol?.owner as? IrVariable ?: return null
        if (irVariable.isVar)
            return null // The actual type of the variable's value may change after reassignment, so its declared type is what matters
        return irVariable.initializer
    }

    fun unwrapStatementContainer(expression: IrExpression): IrExpression? =
        (expression as? IrStatementContainer)?.let {
            when (it) {
                is IrReturnTarget -> // KT-67695: TODO: Perform full traverse to calculate the common type for all IrReturn statements.
                    null // Meanwhile, conservatively returning `null`, which sadly prevents iterator removal in some tests in `nested.kt`.
                else -> it.statements.lastOrNull() as? IrExpression
            }
        }

    fun unwrapImplicitCast(expression: IrExpression): IrExpression? =
        (expression as? IrTypeOperatorCall)?.let {
            expression.argument.takeIf {
                expression.operator.let { it == IrTypeOperator.IMPLICIT_CAST || it == IrTypeOperator.CAST }
            }
        }

    var temp = this
    do {
        unwrapValInitializer(temp)?.let { temp = it }
            ?: unwrapStatementContainer(temp)?.let { temp = it }
            ?: unwrapImplicitCast(temp)?.let { temp = it }
            ?: return temp.type
    } while (true)
}
