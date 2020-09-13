/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.state.ReflectionState
import org.jetbrains.kotlin.ir.interpreter.state.State

internal interface ReflectionProxy : Proxy {
    fun evaluate(expression: IrCall, args: List<Variable>): State

    companion object {
        internal fun ReflectionState.asProxy(interpreter: IrInterpreter): ReflectionProxy {
            return when {
                this is KPropertyState -> when {
                    this.isKProperty0() -> KProperty0Proxy(this, interpreter)
                    this.isKMutableProperty0() -> KMutableProperty0Proxy(this, interpreter)
                    this.isKProperty1() -> KProperty1Proxy(this, interpreter)
                    this.isKMutableProperty1() -> KMutableProperty1Proxy(this, interpreter)
                    this.isKProperty2() -> KProperty2Proxy(this, interpreter)
                    this.isKMutableProperty2() -> KMutableProperty2Proxy(this, interpreter)
                    else -> TODO()
                }
                else -> TODO("not supported reference state")
            }
        }
    }
}
