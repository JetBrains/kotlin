/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.LambdaProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.state.*

internal interface Proxy {
    val state: State
    val interpreter: IrInterpreter

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

internal fun State.unwrap(interpreter: IrInterpreter): Any? {
    return when (this) {
        is Proxy -> this.state
        is Primitive<*> -> this.value
        is Common -> CommonProxy(this, interpreter)
        is Lambda -> this.asProxy(interpreter)
        is Wrapper -> this.value
        else -> throw AssertionError("Unsupported state for proxy unwrap: ${this::class.java}")
    }
}

internal fun Class<*>.isObject(): Boolean {
    return this.name == "java.lang.Object"
}