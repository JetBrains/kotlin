/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.interpreter.*
import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.getDispatchReceiver
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.Common
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.toState
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny

internal class CommonProxy private constructor(override val state: Common, override val callInterceptor: CallInterceptor) : Proxy {
    private fun defaultEquals(other: Any?): Boolean = if (other is Proxy) this.state === other.state else false
    private fun defaultHashCode(): Int = System.identityHashCode(state)
    private fun defaultToString(): String = "${state.irClass.internalName()}@" + hashCode().toString(16).padStart(8, '0')

    /**
     *  This check used to avoid cyclic calls. For example:
     *     override fun toString(): String = super.toString()
     */
    private fun IrFunction.wasAlreadyCalled(): Boolean {
        val anyParameter = this.getLastOverridden().dispatchReceiverParameter!!.symbol
        val callStack = callInterceptor.environment.callStack
        if (callStack.containsVariable(anyParameter) && callStack.getState(anyParameter) === state) return true
        return this == callInterceptor.environment.callStack.currentFrameOwner
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false

        val valueArguments = mutableListOf<Variable>()
        val equalsFun = state.getEqualsFunction()
        if (equalsFun.isFakeOverriddenFromAny() || equalsFun.wasAlreadyCalled()) return defaultEquals(other)

        equalsFun.getDispatchReceiver()!!.let { valueArguments.add(Variable(it, state)) }
        valueArguments.add(Variable(equalsFun.valueParameters.single().symbol, if (other is Proxy) other.state else other as State))

        return callInterceptor.interceptProxy(equalsFun, valueArguments) as Boolean
    }

    override fun hashCode(): Int {
        val valueArguments = mutableListOf<Variable>()
        val hashCodeFun = state.getHashCodeFunction()
        if (hashCodeFun.isFakeOverriddenFromAny() || hashCodeFun.wasAlreadyCalled()) return defaultHashCode()

        hashCodeFun.getDispatchReceiver()!!.let { valueArguments.add(Variable(it, state)) }
        return callInterceptor.interceptProxy(hashCodeFun, valueArguments) as Int
    }

    override fun toString(): String {
        val valueArguments = mutableListOf<Variable>()
        val toStringFun = state.getToStringFunction()
        if (toStringFun.isFakeOverriddenFromAny() || toStringFun.wasAlreadyCalled()) return defaultToString()

        toStringFun.getDispatchReceiver()!!.let { valueArguments.add(Variable(it, state)) }
        return callInterceptor.interceptProxy(toStringFun, valueArguments) as String
    }

    companion object {
        internal fun Common.asProxy(callInterceptor: CallInterceptor, extendFrom: Class<*>? = null): Any {
            val commonProxy = CommonProxy(this, callInterceptor)
            val interfaces = when (extendFrom) {
                null, Object::class.java -> arrayOf(Proxy::class.java)
                else -> arrayOf(extendFrom, Proxy::class.java)
            }

            return java.lang.reflect.Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), interfaces)
            { /*proxy*/_, method, args ->
                when {
                    method.declaringClass == Proxy::class.java && method.name == "getState" -> commonProxy.state
                    method.declaringClass == Proxy::class.java && method.name == "getCallInterceptor" -> commonProxy.callInterceptor
                    method.name == "equals" && method.parameterTypes.single().isObject() -> commonProxy.equals(args.single())
                    method.name == "hashCode" && method.parameterTypes.isEmpty() -> commonProxy.hashCode()
                    method.name == "toString" && method.parameterTypes.isEmpty() -> commonProxy.toString()
                    else -> {
                        val irFunction = commonProxy.state.getIrFunction(method)
                            ?: return@newProxyInstance commonProxy.fallbackIfMethodNotFound(method)
                        val valueArguments = mutableListOf<Variable>()
                        valueArguments += Variable(irFunction.getDispatchReceiver()!!, commonProxy.state)
                        valueArguments += irFunction.valueParameters
                            .mapIndexed { index, parameter -> Variable(parameter.symbol, args[index].toState(parameter.type)) }
                        callInterceptor.interceptProxy(irFunction, valueArguments, method.returnType)
                    }
                }
            }
        }

        private fun CommonProxy.fallbackIfMethodNotFound(method: java.lang.reflect.Method): Any {
            return when {
                method.name == "toArray" && method.parameterTypes.isEmpty() -> {
                    val wrapper = this.state.superWrapperClass
                    if (wrapper == null) arrayOf() else (wrapper as Collection<*>).toTypedArray()
                }
                else -> throw AssertionError("Cannot find method $method in ${this.state}")
            }
        }
    }
}