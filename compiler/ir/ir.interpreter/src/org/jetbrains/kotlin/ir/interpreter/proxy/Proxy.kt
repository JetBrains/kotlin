/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.CommonProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.LambdaProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.reflection.ReflectionProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.state.*

internal interface Proxy {
    val state: State
    val interpreter: IrInterpreter

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
    override fun toString(): String
}

/**
 * Prepare state object to be passed in outer world
 */
internal fun State.wrap(interpreter: IrInterpreter, extendFrom: Class<*>? = null, calledFromBuiltIns: Boolean = false): Any? {
    return when (this) {
        is ExceptionState -> this
        is Wrapper -> this.value
        is Primitive<*> -> this.value
        is Common -> this.asProxy(interpreter, extendFrom, calledFromBuiltIns)
        is Lambda -> this.asProxy(interpreter)
        is ReflectionState -> this.asProxy(interpreter)
        else -> throw AssertionError("${this::class} is unsupported as argument for wrap function")
    }
}

internal fun Class<*>.isObject(): Boolean {
    return this.name == "java.lang.Object"
}