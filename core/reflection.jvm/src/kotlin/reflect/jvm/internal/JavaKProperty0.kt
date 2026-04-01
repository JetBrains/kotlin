/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

internal open class JavaKProperty0<out V>(
    container: KDeclarationContainerImpl, field: Field, rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKProperty<V>(container, field, rawBoundReceiver, overriddenStorage), KProperty0<V> {
    override val getter: Getter<V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(): V = getter.call()

    override fun getDelegate(): Any? = null

    override fun invoke(): V = get()

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaKProperty0(container, jField, rawBoundReceiver, overriddenStorage)

    class Getter<out R>(override val property: JavaKProperty0<R>) : JavaKProperty.Getter<R>(), KProperty0.Getter<R> {
        override fun invoke(): R = property.get()
    }
}

internal open class JavaKMutableProperty0<V>(
    container: KDeclarationContainerImpl, field: Field, rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKProperty0<V>(container, field, rawBoundReceiver, overriddenStorage), KMutableProperty0<V> {
    override val setter: Setter<V> by lazy(PUBLICATION) { Setter(this) }

    override fun set(value: V): Unit = setter.call(value)

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaKMutableProperty0(container, jField, rawBoundReceiver, overriddenStorage)

    class Setter<R>(override val property: JavaKMutableProperty0<R>) : JavaKProperty.Setter<R>(), KMutableProperty0.Setter<R> {
        override fun invoke(value: R): Unit = property.set(value)
    }
}
