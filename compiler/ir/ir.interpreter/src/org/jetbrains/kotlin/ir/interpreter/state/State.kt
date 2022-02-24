/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.state

import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.IrInterpreterEnvironment
import org.jetbrains.kotlin.ir.interpreter.exceptions.handleUserException
import org.jetbrains.kotlin.ir.interpreter.stack.Fields
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KFunctionState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.ReflectionState
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.util.OperatorNameConventions

internal interface State {
    val fields: Fields
    val irClass: IrClass

    fun getField(symbol: IrSymbol): State? {
        return fields[symbol]
    }

    fun setField(symbol: IrSymbol, state: State) {
        fields[symbol] = state
    }

    fun getIrFunctionByIrCall(expression: IrCall): IrFunction?
}

internal fun State.isNull() = this is Primitive<*> && this.value == null
internal fun State?.isUnit() = this is Common && this.irClassFqName() == "kotlin.Unit"

internal fun State.asInt() = (this as Primitive<*>).value as Int
internal fun State.asBoolean() = (this as Primitive<*>).value as Boolean
internal fun State.asString() = (this as Primitive<*>).value.toString()

internal fun State.asBooleanOrNull() = (this as? Primitive<*>)?.value as? Boolean
internal fun State.asStringOrNull() = (this as Primitive<*>).value as? String

internal fun State.isSubtypeOf(other: IrType): Boolean {
    if (this.isNull() && other.isNullable()) return true
    if (this is Primitive<*> && this.value == null) return other.isNullable()
    if (this is ExceptionState) return this.isSubtypeOf(other.classOrNull!!.owner)

    if (this is Primitive<*> && (this.type.isArray() || this.type.isNullableArray()) && (other.isArray() || other.isNullableArray())) {
        fun IrType.arraySubtypeCheck(other: IrType): Boolean {
            if (other !is IrSimpleType || this !is IrSimpleType) return false
            val thisArgument = this.arguments.single().typeOrNull ?: return false
            val otherArgument = other.arguments.single().typeOrNull ?: return other.arguments.single() is IrStarProjection
            if (thisArgument.isArray() && otherArgument.isArray()) return thisArgument.arraySubtypeCheck(otherArgument)
            if (otherArgument.classOrNull == null) return true
            return thisArgument.classOrNull?.isSubtypeOfClass(otherArgument.classOrNull!!) ?: false
        }
        return this.type.arraySubtypeCheck(other)
    }

    if (other.classOrNull?.owner?.isFun == true) {
        return this is KFunctionState && this.funInterface?.isSubtypeOfClass(other.classOrNull!!) == true
    }

    val thisType = this.irClass.defaultType
    if (other.isFunction() && thisType.isKFunction()/* TODO || (other.isSuspendFunction && thisType.isKSuspendFunction())*/) {
        // KFunction{n} has no super type of Function{n},
        // but the single overridden function of KFunction{n}.invoke is Function{n}.invoke.
        val invokeFun = this.irClass.declarations.filterIsInstance<IrSimpleFunction>().single { it.name == OperatorNameConventions.INVOKE }
        return invokeFun.overriddenSymbols.single().owner.parentAsClass.isSubclassOf(other.classOrNull!!.owner)
    }

    return thisType.isSubtypeOfClass(other.classOrNull!!)
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

internal fun State?.mustBeHandledAsReflection(call: IrCall): Boolean {
    val owner = call.symbol.owner
    if (owner.body != null || owner.origin == IrDeclarationOrigin.FUNCTION_FOR_DEFAULT_PARAMETER) return false
    return this is ReflectionState && !(this is KFunctionState && KFunctionState.isCallToInvokeOrMethodFromFunInterface(call))
}

internal fun State.hasTheSameFieldsWith(other: State): Boolean {
    if (this.fields.size != other.fields.size) return false
    // TODO prove that this will always work or find better solution
    this.fields.values.zip(other.fields.values).forEach { (firstState, secondState) ->
        when {
            firstState is Primitive<*> && secondState is Primitive<*> -> if (firstState.value != secondState.value) return false
            else -> if (firstState !== secondState) return false
        }
    }
    return true
}
