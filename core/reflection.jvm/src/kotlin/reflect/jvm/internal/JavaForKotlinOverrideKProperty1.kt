/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

internal open class JavaForKotlinOverrideKProperty1<T, out V>(
    container: KDeclarationContainerImpl,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
    getterMethod: ReflectKFunction,
    setterMethod: ReflectKFunction?,
    overriddenProperty: ReflectKProperty<*>,
) : JavaForKotlinOverrideKProperty<V>(container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty),
    KProperty1<T, V> {
    override val getter: Getter<T, V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(receiver: T): V = getter.call(receiver)

    override fun getDelegate(receiver: T): Any? = null

    override fun invoke(receiver: T): V = get(receiver)

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaForKotlinOverrideKProperty1<T, V>(
            container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty,
        )

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaForKotlinOverrideKProperty1<T, V>(container, boundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty)

    override fun unbindToHigherArity(): ReflectKProperty<V> =
        throw KotlinReflectionInternalError("Cannot unbind KProperty1: $this")

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaForKotlinOverrideKProperty0(container, boundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty)

    class Getter<T, out V>(override val property: JavaForKotlinOverrideKProperty1<T, V>) :
        JavaForKotlinOverrideKProperty.Getter<V>(), KProperty1.Getter<T, V> {
        override fun invoke(receiver: T): V = property.get(receiver)
    }
}

internal open class JavaForKotlinOverrideKMutableProperty1<T, V>(
    container: KDeclarationContainerImpl,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
    getterMethod: ReflectKFunction,
    setterMethod: ReflectKFunction,
    overriddenProperty: ReflectKProperty<*>,
) : JavaForKotlinOverrideKProperty1<T, V>(container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty),
    KMutableProperty1<T, V> {
    override val setter: Setter<T, V> by lazy(PUBLICATION) { Setter(this) }

    override fun set(receiver: T, value: V): Unit = setter.call(receiver, value)

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaForKotlinOverrideKMutableProperty1<T, V>(
            container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod!!, overriddenProperty,
        )

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaForKotlinOverrideKMutableProperty0(
            container, boundReceiver, overriddenStorage, getterMethod, setterMethod!!, overriddenProperty,
        )

    class Setter<T, V>(override val property: JavaForKotlinOverrideKMutableProperty1<T, V>) :
        JavaForKotlinOverrideKProperty.Setter<V>(), KMutableProperty1.Setter<T, V> {
        override fun invoke(receiver: T, value: V): Unit = property.set(receiver, value)
    }
}
