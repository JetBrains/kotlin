/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

internal open class KProperty1Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : AbstractKPropertyProxy(state, callInterceptor), KProperty1<Any?, Any?> {
    override val getter: KProperty1.Getter<Any?, Any?>
        get() = object : Getter(this@KProperty1Proxy.state.getterState!!), KProperty1.Getter<Any?, Any?> {
            override fun invoke(p1: Any?): Any? = call(p1)
        }

    override fun get(receiver: Any?): Any? = getter.invoke(receiver)

    override fun invoke(p1: Any?): Any? = getter.invoke(p1)

    override fun getDelegate(receiver: Any?): Any? {
        TODO("Not yet implemented")
    }
}

internal class KMutableProperty1Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : KProperty1Proxy(state, callInterceptor), KMutableProperty1<Any?, Any?> {
    override val setter: KMutableProperty1.Setter<Any?, Any?>
        get() = object : Setter(this@KMutableProperty1Proxy.state.setterState!!), KMutableProperty1.Setter<Any?, Any?> {
            override fun invoke(p1: Any?, p2: Any?) = call(p1, p2)
        }

    override fun set(receiver: Any?, value: Any?) = setter.call(receiver, value)
}