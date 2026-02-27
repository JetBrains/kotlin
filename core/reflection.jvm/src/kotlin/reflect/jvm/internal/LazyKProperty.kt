/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.reflect.*

internal abstract class LazyKProperty<out V, out D : KProperty<V>>(computeProperty: () -> D) : KProperty<V> {
    val delegate: D by lazy(LazyThreadSafetyMode.PUBLICATION, computeProperty)

    override val name: String get() = delegate.name
    override val parameters: List<KParameter> get() = delegate.parameters
    override val returnType: KType get() = delegate.returnType
    override val typeParameters: List<KTypeParameter> get() = delegate.typeParameters
    override fun call(vararg args: Any?): V = delegate.call(*args)
    override fun callBy(args: Map<KParameter, Any?>): V = delegate.callBy(args)
    override val visibility: KVisibility? get() = delegate.visibility
    override val isFinal: Boolean get() = delegate.isFinal
    override val isOpen: Boolean get() = delegate.isOpen
    override val isAbstract: Boolean get() = delegate.isAbstract
    override val isSuspend: Boolean get() = delegate.isSuspend
    override val isLateinit: Boolean get() = delegate.isLateinit
    override val isConst: Boolean get() = delegate.isConst
    override val annotations: List<Annotation> get() = delegate.annotations

    override fun equals(other: Any?): Boolean = delegate == other
    override fun hashCode(): Int = delegate.hashCode()
    override fun toString(): String = delegate.toString()
}

internal open class LazyKProperty0<out V, out D : KProperty0<V>>(computeProperty: () -> D) :
    LazyKProperty<V, D>(computeProperty), KProperty0<V> {
    override val getter: KProperty0.Getter<V> get() = delegate.getter
    override fun get(): V = delegate.get()
    override fun getDelegate(): Any? = delegate.getDelegate()
    override fun invoke(): V = delegate.invoke()
}

internal class LazyKMutableProperty0<V, D : KMutableProperty0<V>>(computeProperty: () -> D) :
    LazyKProperty0<V, D>(computeProperty), KMutableProperty0<V> {
    override val setter: KMutableProperty0.Setter<V> get() = delegate.setter
    override fun set(value: V): Unit = delegate.set(value)
}

internal open class LazyKProperty1<T, out V, out D : KProperty1<T, V>>(computeProperty: () -> D) :
    LazyKProperty<V, D>(computeProperty), KProperty1<T, V> {
    override val getter: KProperty1.Getter<T, V> get() = delegate.getter
    override fun get(receiver: T): V = delegate.get(receiver)
    override fun getDelegate(receiver: T): Any? = delegate.getDelegate(receiver)
    override fun invoke(receiver: T): V = delegate.invoke(receiver)
}

internal class LazyKMutableProperty1<T, V, D : KMutableProperty1<T, V>>(computeProperty: () -> D) :
    LazyKProperty1<T, V, D>(computeProperty), KMutableProperty1<T, V> {
    override val setter: KMutableProperty1.Setter<T, V> get() = delegate.setter
    override fun set(receiver: T, value: V): Unit = delegate.set(receiver, value)
}
