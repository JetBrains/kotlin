/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.getDispatchReceiver
import org.jetbrains.kotlin.ir.interpreter.internalName
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.Common
import org.jetbrains.kotlin.ir.interpreter.toState
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny

/**
 * calledFromBuiltIns - used to avoid cyclic calls. For example:
 *     override fun toString(): String {
 *         return super.toString()
 *     }
 */
internal class CommonProxy private constructor(
    override val state: Common, override val interpreter: IrInterpreter, private val calledFromBuiltIns: Boolean = false
) : Proxy {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Proxy) return false

        val valueArguments = mutableListOf<Variable>()
        val equalsFun = state.getEqualsFunction()
        if (equalsFun.isFakeOverriddenFromAny() || calledFromBuiltIns) return this.state === other.state

        equalsFun.getDispatchReceiver()!!.let { valueArguments.add(Variable(it, state)) }
        valueArguments.add(Variable(equalsFun.valueParameters.single().symbol, other.state))

        return with(interpreter) { equalsFun.proxyInterpret(valueArguments) } as Boolean
    }

    override fun hashCode(): Int {
        val valueArguments = mutableListOf<Variable>()
        val hashCodeFun = state.getHashCodeFunction()
        if (hashCodeFun.isFakeOverriddenFromAny() || calledFromBuiltIns) return System.identityHashCode(state)

        hashCodeFun.getDispatchReceiver()!!.let { valueArguments.add(Variable(it, state)) }
        return with(interpreter) { hashCodeFun.proxyInterpret(valueArguments) } as Int
    }

    override fun toString(): String {
        val valueArguments = mutableListOf<Variable>()
        val toStringFun = state.getToStringFunction()
        if (toStringFun.isFakeOverriddenFromAny() || calledFromBuiltIns) {
            return "${state.irClass.internalName()}@" + hashCode().toString(16).padStart(8, '0')
        }

        toStringFun.getDispatchReceiver()!!.let { valueArguments.add(Variable(it, state)) }
        return with(interpreter) { toStringFun.proxyInterpret(valueArguments) } as String
    }

    companion object {
        internal fun Common.asProxy(interpreter: IrInterpreter, extendFrom: Class<*>? = null, calledFromBuiltIns: Boolean = false): Any {
            val commonProxy = CommonProxy(this, interpreter, calledFromBuiltIns)

            val interfaces = when (extendFrom) {
                null, Object::class.java -> arrayOf(Proxy::class.java)
                else -> arrayOf(extendFrom, Proxy::class.java)
            }
            return java.lang.reflect.Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), interfaces)
            { /*proxy*/_, method, args ->
                when {
                    method.declaringClass == Proxy::class.java && method.name == "getState" -> commonProxy.state
                    method.declaringClass == Proxy::class.java && method.name == "getInterpreter" -> commonProxy.interpreter
                    method.name == "equals" && method.parameterTypes.single().isObject() -> commonProxy.equals(args.single())
                    method.name == "hashCode" && method.parameterTypes.isEmpty() -> commonProxy.hashCode()
                    method.name == "toString" && method.parameterTypes.isEmpty() -> commonProxy.toString()
                    else -> {
                        val irFunction = commonProxy.state.getIrFunction(method)
                            ?: throw AssertionError("Cannot find method $method in ${commonProxy.state}")
                        val valueArguments = mutableListOf<Variable>()
                        valueArguments += Variable(irFunction.getDispatchReceiver()!!, commonProxy.state)
                        valueArguments += irFunction.valueParameters
                            .mapIndexed { index, parameter -> Variable(parameter.symbol, args[index].toState(parameter.type)) }
                        with(interpreter) { irFunction.proxyInterpret(valueArguments, method.returnType) }
                    }
                }
            }
        }
    }
}