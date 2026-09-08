/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

internal open class JavaForKotlinOverrideKProperty0<out V>(
    container: KDeclarationContainerImpl,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
    getterMethod: ReflectKFunction,
    setterMethod: ReflectKFunction?,
    overriddenProperty: ReflectKProperty<*>,
) : JavaForKotlinOverrideKProperty<V>(container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty),
    KProperty0<V> {
    override val getter: Getter<V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(): V = getter.call()

    override fun getDelegate(): Any? = null

    override fun invoke(): V = get()

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaForKotlinOverrideKProperty0(container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty)

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaForKotlinOverrideKProperty0(container, boundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty)

    override fun unbindToHigherArity(): ReflectKProperty<V> =
        JavaForKotlinOverrideKProperty1<Any?, V>(
            container, CallableReference.NO_RECEIVER, overriddenStorage, getterMethod, setterMethod, overriddenProperty,
        )

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        throw KotlinReflectionInternalError("Cannot bind KProperty0: $this")

    class Getter<out R>(override val property: JavaForKotlinOverrideKProperty0<R>) :
        JavaForKotlinOverrideKProperty.Getter<R>(), KProperty0.Getter<R> {
        override fun invoke(): R = property.get()
    }
}

internal open class JavaForKotlinOverrideKMutableProperty0<V>(
    container: KDeclarationContainerImpl,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
    getterMethod: ReflectKFunction,
    setterMethod: ReflectKFunction,
    overriddenProperty: ReflectKProperty<*>,
) : JavaForKotlinOverrideKProperty0<V>(container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod, overriddenProperty),
    KMutableProperty0<V> {
    override val setter: Setter<V> by lazy(PUBLICATION) { Setter(this) }

    override fun set(value: V): Unit = setter.call(value)

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaForKotlinOverrideKMutableProperty0(
            container, rawBoundReceiver, overriddenStorage, getterMethod, setterMethod!!, overriddenProperty,
        )

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaForKotlinOverrideKMutableProperty0(
            container, boundReceiver, overriddenStorage, getterMethod, setterMethod!!, overriddenProperty,
        )

    override fun unbindToHigherArity(): ReflectKProperty<V> =
        JavaForKotlinOverrideKMutableProperty1<Any?, V>(
            container, CallableReference.NO_RECEIVER, overriddenStorage, getterMethod, setterMethod!!, overriddenProperty,
        )

    class Setter<R>(override val property: JavaForKotlinOverrideKMutableProperty0<R>) :
        JavaForKotlinOverrideKProperty.Setter<R>(), KMutableProperty0.Setter<R> {
        override fun invoke(value: R): Unit = property.set(value)
    }
}
