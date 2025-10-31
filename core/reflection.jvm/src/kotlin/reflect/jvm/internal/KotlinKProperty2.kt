/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmProperty
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.KProperty2

internal open class KotlinKProperty2<D, E, out V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKProperty<V>(container, signature, rawBoundReceiver, kmProperty), KProperty2<D, E, V> {
    private val _getter: Lazy<Getter<D, E, V>> = lazy(PUBLICATION) { Getter(this) }

    override val getter: Getter<D, E, V> get() = _getter.value

    override fun get(receiver1: D, receiver2: E): V = getter.call(receiver1, receiver2)

    private val delegateSource = lazy(PUBLICATION) { computeDelegateSource() }

    override fun getDelegate(receiver1: D, receiver2: E): Any? = getDelegateImpl(delegateSource.value, receiver1, receiver2)

    override fun invoke(receiver1: D, receiver2: E): V = get(receiver1, receiver2)

    class Getter<D, E, out V>(override val property: KotlinKProperty2<D, E, V>) : KotlinKProperty.Getter<V>(), KProperty2.Getter<D, E, V> {
        override fun invoke(receiver1: D, receiver2: E): V = property.get(receiver1, receiver2)
    }
}

internal class KotlinKMutableProperty2<D, E, V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKProperty2<D, E, V>(container, signature, rawBoundReceiver, kmProperty), KMutableProperty2<D, E, V> {
    private val _setter: Lazy<Setter<D, E, V>> = lazy(PUBLICATION) { Setter(this) }

    override val setter: Setter<D, E, V> get() = _setter.value

    override fun set(receiver1: D, receiver2: E, value: V): Unit = setter.call(receiver1, receiver2, value)

    class Setter<D, E, V>(override val property: KotlinKMutableProperty2<D, E, V>) :
        KotlinKProperty.Setter<V>(), KMutableProperty2.Setter<D, E, V> {
        override fun invoke(receiver1: D, receiver2: E, value: V): Unit = property.set(receiver1, receiver2, value)
    }
}
