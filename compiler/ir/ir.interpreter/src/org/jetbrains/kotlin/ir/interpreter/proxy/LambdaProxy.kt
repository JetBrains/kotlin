/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy

import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.Lambda
import org.jetbrains.kotlin.ir.interpreter.toState

internal class LambdaProxy private constructor(
    override val state: Lambda, override val interpreter: IrInterpreter
) : Proxy {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Proxy) return false

        return state == other.state
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }

    override fun toString(): String {
        return state.toString()
    }

    companion object {
        fun Lambda.asProxy(interpreter: IrInterpreter): Any {
            val lambdaProxy = LambdaProxy(this, interpreter)
            val arity = this.getArity()
            val hasBigArity = arity == null || arity >= FunctionInvokeDescriptor.BIG_ARITY

            val functionClass = when {
                !hasBigArity && this.isFunction -> Class.forName("kotlin.jvm.functions." + this.irClass.name)
                hasBigArity && this.isFunction -> Class.forName("kotlin.jvm.functions.FunctionN")
                this.isKFunction -> Class.forName("kotlin.reflect.KFunction")
                else -> throw InternalError("Cannot handle lambda $this as proxy")
            }
            return java.lang.reflect.Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), arrayOf(functionClass, Proxy::class.java))
            { proxy, method, args ->
                when {
                    method.declaringClass == Proxy::class.java && method.name == "getState" -> lambdaProxy.state
                    method.declaringClass == Proxy::class.java && method.name == "getInterpreter" -> lambdaProxy.interpreter
                    method.name == "equals" && method.parameterTypes.single().isObject() -> lambdaProxy.equals(args.single())
                    method.name == "hashCode" && method.parameterTypes.isEmpty() -> lambdaProxy.hashCode()
                    method.name == "toString" && method.parameterTypes.isEmpty() -> lambdaProxy.toString()
                    method.name == "invoke" && method.parameterTypes.single().isObject() -> {
                        val valueArguments = this.irFunction.valueParameters
                            .mapIndexed { index, parameter -> Variable(parameter.symbol, args[index].toState(parameter.type)) }
                        with(interpreter) { lambdaProxy.state.irFunction.interpret(valueArguments, method.returnType) }
                    }
                    else -> throw InternalError("Cannot invoke method ${method.name} from lambda $this")
                }
            }
        }
    }
}