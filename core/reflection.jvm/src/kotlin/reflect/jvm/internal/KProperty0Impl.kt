/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

internal open class KProperty0Impl<out V> : KProperty0<V>, KPropertyImpl<V> {
    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    constructor(container: KDeclarationContainerImpl, name: String, signature: String, boundReceiver: Any?) : super(
        container, name, signature, boundReceiver
    )

    private val _getter = lazy(PUBLICATION) { Getter(this) }

    override val getter: Getter<V> get() = _getter.value

    override fun get(): V = getter.call()

    private val delegateValue = lazy(PUBLICATION) { getDelegateImpl(computeDelegateSource(), null, null) }

    override fun getDelegate(): Any? = delegateValue.value

    override fun invoke(): V = get()

    class Getter<out R>(override val property: KProperty0Impl<R>) : KPropertyImpl.Getter<R>(), KProperty0.Getter<R> {
        override fun invoke(): R = property.get()
    }
}

internal class KMutableProperty0Impl<V> : KProperty0Impl<V>, KMutableProperty0<V> {
    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    constructor(container: KDeclarationContainerImpl, name: String, signature: String, boundReceiver: Any?) : super(
        container, name, signature, boundReceiver
    )

    private val _setter = lazy(PUBLICATION) { Setter(this) }

    override val setter: Setter<V> get() = _setter.value

    override fun set(value: V) = setter.call(value)

    class Setter<R>(override val property: KMutableProperty0Impl<R>) : KPropertyImpl.Setter<R>(), KMutableProperty0.Setter<R> {
        override fun invoke(value: R): Unit = property.set(value)
    }
}
