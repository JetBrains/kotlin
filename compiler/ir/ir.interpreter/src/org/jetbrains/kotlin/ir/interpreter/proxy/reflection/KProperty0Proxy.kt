/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

internal open class KProperty0Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : AbstractKPropertyProxy(state, callInterceptor), KProperty0<Any?> {
    override val getter: KProperty0.Getter<Any?>
        get() = object : Getter(this@KProperty0Proxy.state.getterState!!), KProperty0.Getter<Any?> { // TODO avoid !!; getter will be null in case of java property
            override fun invoke(): Any? = call()
        }

    override fun get(): Any? = getter.invoke()

    override fun invoke(): Any? = getter.invoke()

    override fun getDelegate(): Any? {
        TODO("Not yet implemented")
    }
}

internal class KMutableProperty0Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : KProperty0Proxy(state, callInterceptor), KMutableProperty0<Any?> {
    override val setter: KMutableProperty0.Setter<Any?>
        get() = object : Setter(this@KMutableProperty0Proxy.state.setterState!!), KMutableProperty0.Setter<Any?> {
            override fun invoke(p1: Any?) = call(p1)
        }

    override fun set(value: Any?) = setter.call(value)
}
