/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmProperty
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

internal open class KotlinKProperty1<T, out V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKProperty<V>(container, signature, rawBoundReceiver, kmProperty), KProperty1<T, V> {
    private val _getter: Lazy<Getter<T, V>> = lazy(PUBLICATION) { Getter(this) }

    override val getter: Getter<T, V> get() = _getter.value

    override fun get(receiver: T): V = getter.call(receiver)

    private val delegateSource = lazy(PUBLICATION) { computeDelegateSource() }

    override fun getDelegate(receiver: T): Any? = getDelegateImpl(delegateSource.value, receiver, null)

    override fun invoke(receiver: T): V = get(receiver)

    class Getter<T, out V>(override val property: KotlinKProperty1<T, V>) : KotlinKProperty.Getter<V>(), KProperty1.Getter<T, V> {
        override fun invoke(receiver: T): V = property.get(receiver)
    }
}

internal class KotlinKMutableProperty1<T, V>(
    container: KDeclarationContainerImpl, signature: String, rawBoundReceiver: Any?, kmProperty: KmProperty,
) : KotlinKProperty1<T, V>(container, signature, rawBoundReceiver, kmProperty), KMutableProperty1<T, V> {
    private val _setter: Lazy<Setter<T, V>> = lazy(PUBLICATION) { Setter(this) }

    override val setter: Setter<T, V> get() = _setter.value

    override fun set(receiver: T, value: V): Unit = setter.call(receiver, value)

    class Setter<T, V>(override val property: KotlinKMutableProperty1<T, V>) : KotlinKProperty.Setter<V>(), KMutableProperty1.Setter<T, V> {
        override fun invoke(receiver: T, value: V): Unit = property.set(receiver, value)
    }
}
