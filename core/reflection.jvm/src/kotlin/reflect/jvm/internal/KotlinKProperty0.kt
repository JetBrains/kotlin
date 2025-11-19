/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

internal open class KotlinKProperty0<out V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKProperty<V>(container, signature, rawBoundReceiver, kmProperty), KProperty0<V> {
    override val getter: Getter<V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(): V = getter.call()

    override fun getDelegate(): Any? {
        checkLocalDelegatedPropertyOrAccessor()
        return null
    }

    override fun invoke(): V = get()

    class Getter<out R>(override val property: KotlinKProperty0<R>) : KotlinKProperty.Getter<R>(), KProperty0.Getter<R> {
        override fun invoke(): R = property.get()
    }
}

internal class KotlinKMutableProperty0<V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKProperty0<V>(container, signature, rawBoundReceiver, kmProperty), KMutableProperty0<V> {
    override val setter: Setter<V> by lazy(PUBLICATION) { Setter(this) }

    override fun set(value: V): Unit = setter.call(value)

    class Setter<R>(override val property: KotlinKMutableProperty0<R>) : KotlinKProperty.Setter<R>(), KMutableProperty0.Setter<R> {
        override fun invoke(value: R): Unit = property.set(value)
    }
}
