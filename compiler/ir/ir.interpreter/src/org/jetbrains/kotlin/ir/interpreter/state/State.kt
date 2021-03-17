/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.handleUserException
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.defaultType

internal interface State {
    val fields: MutableList<Variable>
    val irClass: IrClass

    fun getState(symbol: IrSymbol): State? {
        return fields.firstOrNull { it.symbol == symbol }?.state
    }

    fun setField(newVar: Variable) {
        when (val oldState = fields.firstOrNull { it.symbol == newVar.symbol }) {
            null -> fields.add(newVar)                                      // newVar isn't present in value list
            else -> fields[fields.indexOf(oldState)].state = newVar.state   // newVar already present
        }
    }

    fun getIrFunctionByIrCall(expression: IrCall): IrFunction?
}

internal fun State.isNull() = this is Primitive<*> && this.value == null

internal fun State.asInt() = (this as Primitive<*>).value as Int
internal fun State.asBoolean() = (this as Primitive<*>).value as Boolean
internal fun State.asString() = (this as Primitive<*>).value.toString()

internal fun State.asBooleanOrNull() = (this as? Primitive<*>)?.value as? Boolean
internal fun State.asStringOrNull() = (this as Primitive<*>).value as? String

internal fun State.isSubtypeOf(other: IrType): Boolean {
    if (this is Primitive<*> && this.value == null) return other.isNullable()
    if (this is ExceptionState) return this.isSubtypeOf(other.classOrNull!!.owner)

    if (this is Primitive<*> && this.type.isArray() && other.isArray()) {
        fun IrType.arraySubtypeCheck(other: IrType): Boolean {
            if (other !is IrSimpleType || this !is IrSimpleType) return false
            val thisArgument = this.arguments.single().typeOrNull ?: return false
            val otherArgument = other.arguments.single().typeOrNull ?: return other.arguments.single() is IrStarProjection
            if (thisArgument.isArray() && otherArgument.isArray()) return thisArgument.arraySubtypeCheck(otherArgument)
            if (otherArgument.classOrNull == null) return false
            return thisArgument.classOrNull?.isSubtypeOfClass(otherArgument.classOrNull!!) ?: false
        }
        return this.type.arraySubtypeCheck(other)
    }

    return this.irClass.defaultType.isSubtypeOfClass(other.classOrNull!!)
}

/**
 * This method used to check if for not null parameter there was passed null argument.
 */
internal fun State.checkNullability(
    irType: IrType?, environment: IrInterpreterEnvironment, exceptionToThrow: () -> Throwable = { NullPointerException() }
): State? {
    if (irType !is IrSimpleType) return this
    if (this.isNull() && !irType.isNullable()) {
        exceptionToThrow().handleUserException(environment)
        return null
    }
    return this
}