/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

internal open class JavaFieldKProperty1<T, out V>(
    container: KDeclarationContainerImpl, field: Field, rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaFieldKProperty<V>(container, field, rawBoundReceiver, overriddenStorage), KProperty1<T, V> {
    override val getter: Getter<T, V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(receiver: T): V = getter.call(receiver)

    override fun getDelegate(receiver: T): Any? = null

    override fun invoke(receiver: T): V = get(receiver)

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaFieldKProperty1<T, V>(container, jField, rawBoundReceiver, overriddenStorage)

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaFieldKProperty1<T, V>(container, jField, boundReceiver, overriddenStorage)

    override fun unbindToHigherArity(): ReflectKProperty<V> =
        throw KotlinReflectionInternalError("Cannot unbind KProperty1: $this")

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaFieldKProperty0(container, jField, boundReceiver, overriddenStorage)

    class Getter<T, out V>(override val property: JavaFieldKProperty1<T, V>) : JavaFieldKProperty.Getter<V>(), KProperty1.Getter<T, V> {
        override fun invoke(receiver: T): V = property.get(receiver)
    }
}

internal open class JavaFieldKMutableProperty1<T, V>(
    container: KDeclarationContainerImpl, field: Field, rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaFieldKProperty1<T, V>(container, field, rawBoundReceiver, overriddenStorage), KMutableProperty1<T, V> {
    override val setter: Setter<T, V> by lazy(PUBLICATION) { Setter(this) }

    override fun set(receiver: T, value: V): Unit = setter.call(receiver, value)

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaFieldKMutableProperty1<T, V>(container, jField, rawBoundReceiver, overriddenStorage)

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaFieldKMutableProperty1<T, V>(container, jField, boundReceiver, overriddenStorage)

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaFieldKMutableProperty0(container, jField, boundReceiver, overriddenStorage)

    class Setter<T, V>(override val property: JavaFieldKMutableProperty1<T, V>) : JavaFieldKProperty.Setter<V>(), KMutableProperty1.Setter<T, V> {
        override fun invoke(receiver: T, value: V): Unit = property.set(receiver, value)
    }
}
