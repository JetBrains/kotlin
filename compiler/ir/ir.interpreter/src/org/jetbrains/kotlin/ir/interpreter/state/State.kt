/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.exceptions.throwAsUserException
import org.jetbrains.kotlin.ir.interpreter.isFunction
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType

internal interface State {
    val fields: MutableList<Variable>
    val irClass: IrClass
    val typeArguments: MutableList<Variable>

    fun getState(symbol: IrSymbol): State? {
        return fields.firstOrNull { it.symbol == symbol }?.state
    }

    fun setField(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.symbol == newVar.symbol }) {
            null -> fields.add(newVar)                                      // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)].state = newVar.state   // newVar already present
        }
    }

    fun addTypeArguments(typeArguments: List<Variable>) {
        this.typeArguments.addAll(typeArguments)
    }

    fun getIrFunctionByIrCall(expression: IrCall): IrFunction?
}

internal fun State.isNull() = this is Primitive<*> && this.value == null

internal fun State.asInt() = (this as Primitive<*>).value as Int
internal fun State.asBoolean() = (this as Primitive<*>).value as Boolean
internal fun State.asString() = (this as Primitive<*>).value.toString()

internal fun State.asBooleanOrNull() = (this as? Primitive<*>)?.value as? Boolean

internal fun State.isSubtypeOf(other: IrType): Boolean {
    if (this is Primitive<*> && this.value == null) return other.isNullable()

    if (this is Primitive<*> && this.type.isArray() && other.isArray()) {
        val thisClass = this.typeArguments.single().state.irClass.symbol
        val otherArgument = (other as IrSimpleType).arguments.single()
        if (otherArgument is IrStarProjection) return true
        return thisClass.isSubtypeOfClass(otherArgument.typeOrNull!!.classOrNull!!)
    }

    if (this is Lambda) {
        if (!other.isFunction()) return false
        val typeArgumentsCount = (other as? IrSimpleType)?.arguments?.size ?: return false
        val lambdaArgumentCount = this.irFunction.valueParameters.size + 1 // +1 for return type
        return typeArgumentsCount == lambdaArgumentCount
    }

    return this.irClass.defaultType.isSubtypeOfClass(other.classOrNull!!)
}

/**
 * This method used to check if for not null parameter there was passed null argument.
 */
internal fun State.checkNullability(
    irType: IrType?, throwException: () -> Nothing = { NullPointerException().throwAsUserException() }
): State {
    if (irType !is IrSimpleType) return this
    if (this.isNull() && !irType.isNullable()) {
        throwException()
    }
    return this
}