/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.builtins.functions.BuiltInFunctionArity
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KFunctionState
import org.jetbrains.kotlin.ir.interpreter.toState
import org.jetbrains.kotlin.ir.util.isSuspend
import kotlin.reflect.*

internal class KFunctionProxy(
    override val state: KFunctionState, override val interpreter: IrInterpreter
) : ReflectionProxy, KFunction<Any?>, FunctionWithAllInvokes {
    override val arity: Int = state.getArity() ?: BuiltInFunctionArity.BIG_ARITY

    override val isInline: Boolean
        get() = state.irFunction.isInline
    override val isExternal: Boolean
        get() = state.irFunction.isExternal
    override val isOperator: Boolean
        get() = state.irFunction is IrSimpleFunction && state.irFunction.isOperator
    override val isInfix: Boolean
        get() = state.irFunction is IrSimpleFunction && state.irFunction.isInfix
    override val name: String
        get() = state.irFunction.name.asString()


    override val annotations: List<Annotation>
        get() = TODO("Not yet implemented")
    override val parameters: List<KParameter>
        get() = state.getParameters(interpreter)
    override val returnType: KType
        get() = state.getReturnType(interpreter)
    override val typeParameters: List<KTypeParameter>
        get() = state.getTypeParameters(interpreter)

    override fun call(vararg args: Any?): Any? {
        // TODO check arity
        var index = 0
        val dispatchReceiver = state.irFunction.dispatchReceiverParameter?.let { Variable(it.symbol, args[index++].toState(it.type)) }
        val extensionReceiver = state.irFunction.extensionReceiverParameter?.let { Variable(it.symbol, args[index++].toState(it.type)) }
        val valueArguments = listOfNotNull(dispatchReceiver, extensionReceiver) +
                state.irFunction.valueParameters.map { parameter -> Variable(parameter.symbol, args[index++].toState(parameter.type)) }
        return with(interpreter) { state.irFunction.interpret(valueArguments) }
    }

    override fun callBy(args: Map<KParameter, Any?>): Any? {
        TODO("Not yet implemented")
    }

    override val visibility: KVisibility?
        get() = state.irFunction.visibility.toKVisibility()
    override val isFinal: Boolean
        get() = state.irFunction is IrSimpleFunction && state.irFunction.modality == Modality.FINAL
    override val isOpen: Boolean
        get() = state.irFunction is IrSimpleFunction && state.irFunction.modality == Modality.OPEN
    override val isAbstract: Boolean
        get() = state.irFunction is IrSimpleFunction && state.irFunction.modality == Modality.ABSTRACT
    override val isSuspend: Boolean
        get() = state.irFunction.isSuspend

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReflectionProxy) return false

        return state == other.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun toString(): String {
        return state.toString()
    }
}

