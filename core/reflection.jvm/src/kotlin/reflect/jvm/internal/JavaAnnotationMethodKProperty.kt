/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl

internal abstract class JavaAnnotationMethodKProperty<out V>(
    container: KDeclarationContainerImpl, method: Method, rawBoundReceiver: Any?, overriddenStorage: KCallableOverriddenStorage,
) : JavaKProperty<V>(container, method, rawBoundReceiver, overriddenStorage) {
    val jMethod: Method get() = member as Method

    override val signature: String
        get() = jMethod.jvmSignature

    override val returnType: KType by lazy(PUBLICATION) {
        substituteType(jMethod.genericReturnType.toKType(emptyMap(), TypeNullability.NOT_NULL, isForAnnotationParameter = true))
    }

    override val isConst: Boolean get() = false
    override val javaField: Field? get() = null

    abstract class Getter<out V> : JavaKProperty.Getter<V>() {
        abstract override val property: JavaAnnotationMethodKProperty<V>

        override val caller: Caller<*> by lazy(PUBLICATION) {
            val method = property.jMethod
            if (isBound) CallerImpl.Method.BoundInstance(method, boundReceiver) else CallerImpl.Method.Instance(method)
        }
    }
}

internal class JavaAnnotationMethodKProperty0<out V>(
    container: KDeclarationContainerImpl, method: Method, rawBoundReceiver: Any?, overriddenStorage: KCallableOverriddenStorage,
) : JavaAnnotationMethodKProperty<V>(container, method, rawBoundReceiver, overriddenStorage), KProperty0<V> {
    override val getter: Getter<V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(): V = getter.call()

    override fun getDelegate(): Any? = null

    override fun invoke(): V = get()

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaAnnotationMethodKProperty0(container, jMethod, rawBoundReceiver, overriddenStorage)

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaAnnotationMethodKProperty0(container, jMethod, boundReceiver, overriddenStorage)

    override fun unbindToHigherArity(): ReflectKProperty<V> =
        JavaAnnotationMethodKProperty1<Any?, V>(container, jMethod, CallableReference.NO_RECEIVER, overriddenStorage)

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        throw KotlinReflectionInternalError("Cannot bind KProperty0: $this")

    class Getter<out R>(
        override val property: JavaAnnotationMethodKProperty0<R>,
    ) : JavaAnnotationMethodKProperty.Getter<R>(), KProperty0.Getter<R> {
        override fun invoke(): R = property.get()
    }
}

internal class JavaAnnotationMethodKProperty1<T, out V>(
    container: KDeclarationContainerImpl, method: Method, rawBoundReceiver: Any?, overriddenStorage: KCallableOverriddenStorage,
) : JavaAnnotationMethodKProperty<V>(container, method, rawBoundReceiver, overriddenStorage), KProperty1<T, V> {
    override val getter: Getter<T, V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(receiver: T): V = getter.call(receiver)

    override fun getDelegate(receiver: T): Any? = null

    override fun invoke(receiver: T): V = get(receiver)

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): ReflectKCallable<V> =
        JavaAnnotationMethodKProperty1<T, V>(container, jMethod, rawBoundReceiver, overriddenStorage)

    override fun rebindSameArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaAnnotationMethodKProperty1<T, V>(container, jMethod, boundReceiver, overriddenStorage)

    override fun unbindToHigherArity(): ReflectKProperty<V> =
        throw KotlinReflectionInternalError("Cannot unbind KProperty1: $this")

    override fun bindToLowerArity(boundReceiver: Any?): ReflectKProperty<V> =
        JavaAnnotationMethodKProperty0(container, jMethod, boundReceiver, overriddenStorage)

    class Getter<T, out V>(
        override val property: JavaAnnotationMethodKProperty1<T, V>,
    ) : JavaAnnotationMethodKProperty.Getter<V>(), KProperty1.Getter<T, V> {
        override fun invoke(receiver: T): V = property.get(receiver)
    }
}
