/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.proxy.reflection

import org.jetbrains.kotlin.ir.interpreter.IrInterpreter
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty2
import kotlin.reflect.KTypeParameter

internal open class KProperty2Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : AbstractKPropertyProxy(state, interpreter), KProperty2<Proxy, Proxy, Any?> {

    override val getter: KProperty2.Getter<Proxy, Proxy, Any?>
        get() = TODO("Not yet implemented")
    override val parameters: List<KParameter>
        get() = TODO("Not yet implemented")
    override val typeParameters: List<KTypeParameter>
        get() = TODO("Not yet implemented")

    override fun call(vararg args: Any?): Any? {
        TODO("Not yet implemented")
    }

    override fun callBy(args: Map<KParameter, Any?>): Any? {
        TODO("Not yet implemented")
    }

    override fun get(receiver1: Proxy, receiver2: Proxy): Any? {
        TODO("Not yet implemented")
    }

    override fun getDelegate(receiver1: Proxy, receiver2: Proxy): Any? {
        TODO("Not yet implemented")
    }

    override fun invoke(p1: Proxy, p2: Proxy): Any? = get(p1, p2)
}

internal class KMutableProperty2Proxy(
    override val state: KPropertyState, override val interpreter: IrInterpreter
) : KProperty2Proxy(state, interpreter), KMutableProperty2<Proxy, Proxy, Any?> {

    override val setter: KMutableProperty2.Setter<Proxy, Proxy, Any?>
        get() = TODO("Not yet implemented")

    override fun set(receiver1: Proxy, receiver2: Proxy, value: Any?) {
        TODO("Not yet implemented")
    }
}