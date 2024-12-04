/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.CallInterceptor
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.KProperty2

internal open class KProperty2Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : AbstractKPropertyProxy(state, callInterceptor), KProperty2<Any?, Any?, Any?> {
    override val getter: KProperty2.Getter<Any?, Any?, Any?>
        get() = object : Getter(this@KProperty2Proxy.state.getterState!!), KProperty2.Getter<Any?, Any?, Any?> {
            override fun invoke(p1: Any?, p2: Any?): Any? = call(p1, p2)
        }

    override fun get(receiver1: Any?, receiver2: Any?): Any? = getter.call(receiver1, receiver2)

    override fun invoke(p1: Any?, p2: Any?): Any? = getter.call(p1, p2)

    override fun getDelegate(receiver1: Any?, receiver2: Any?): Any? {
        TODO("Not yet implemented")
    }
}

internal class KMutableProperty2Proxy(
    state: KPropertyState, callInterceptor: CallInterceptor
) : KProperty2Proxy(state, callInterceptor), KMutableProperty2<Any?, Any?, Any?> {
    override val setter: KMutableProperty2.Setter<Any?, Any?, Any?>
        get() = object : Setter(this@KMutableProperty2Proxy.state.setterState!!), KMutableProperty2.Setter<Any?, Any?, Any?> {
            override fun invoke(p1: Any?, p2: Any?, p3: Any?) = call(p1, p2, p3)
        }

    override fun set(receiver1: Any?, receiver2: Any?, value: Any?) = setter.call(receiver1, receiver2, value)
}