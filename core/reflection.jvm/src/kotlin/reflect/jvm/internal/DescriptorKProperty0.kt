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
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

internal open class DescriptorKProperty0<out V> : KProperty0<V>, DescriptorKProperty<V> {
    constructor(
        container: KDeclarationContainerImpl,
        descriptor: PropertyDescriptor,
        boundReceiver: Any?,
        overriddenStorage: KCallableOverriddenStorage,
        boundContextArguments: List<Any?> = emptyList(),
    ) : super(container, descriptor, boundReceiver, overriddenStorage, boundContextArguments)

    constructor(container: KDeclarationContainerImpl, name: String, signature: String, boundReceiver: Any?, boundContextArguments: List<Any?>) : super(
        container, name, signature, boundReceiver, boundContextArguments
    )

    override val getter: Getter<V> by lazy(PUBLICATION) { Getter(this) }

    override fun get(): V = getter.call()

    private val delegateValue = lazy(PUBLICATION) { getDelegateImpl(computeDelegateSource(), null, null) }

    override fun getDelegate(): Any? = delegateValue.value

    override fun invoke(): V = get()

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): DescriptorKProperty0<V> =
        DescriptorKProperty0(container, descriptor, CallableReference.NO_RECEIVER, overriddenStorage)

    override fun bindToLowerArity(boundReceiver: Any?, boundContextArguments: List<Any?>) =
        throw KotlinReflectionInternalError("Cannot bind KProperty0: $this")

    override fun unbindToHigherArity(): ReflectKCallable<V> =
        if (hasContextParameters) DescriptorKPropertyN(container, descriptor, CallableReference.NO_RECEIVER, overriddenStorage)
        else DescriptorKProperty1<Any?, V>(container, descriptor, CallableReference.NO_RECEIVER, overriddenStorage)

    class Getter<out R>(override val property: DescriptorKProperty0<R>) : DescriptorKProperty.Getter<R>(), KProperty0.Getter<R> {
        override fun invoke(): R = property.get()
    }
}

internal class DescriptorKMutableProperty0<V> : DescriptorKProperty0<V>, KMutableProperty0<V> {
    constructor(
        container: KDeclarationContainerImpl,
        descriptor: PropertyDescriptor,
        boundReceiver: Any?,
        overriddenStorage: KCallableOverriddenStorage,
        boundContextArguments: List<Any?> = emptyList(),
    ) : super(container, descriptor, boundReceiver, overriddenStorage, boundContextArguments)

    constructor(container: KDeclarationContainerImpl, name: String, signature: String, boundReceiver: Any?, boundContextArguments: List<Any?>) : super(
        container, name, signature, boundReceiver, boundContextArguments
    )

    override val setter: Setter<V> by lazy(PUBLICATION) { Setter(this) }

    override fun set(value: V) = setter.call(value)

    override fun shallowCopy(
        container: KDeclarationContainerImpl,
        overriddenStorage: KCallableOverriddenStorage,
    ): DescriptorKMutableProperty0<V> =
        DescriptorKMutableProperty0(container, descriptor, CallableReference.NO_RECEIVER, overriddenStorage)

    override fun unbindToHigherArity(): ReflectKCallable<V> =
        if (hasContextParameters) DescriptorKMutablePropertyN(container, descriptor, CallableReference.NO_RECEIVER, overriddenStorage)
        else DescriptorKMutableProperty1<Any?, V>(container, descriptor, CallableReference.NO_RECEIVER, overriddenStorage)

    class Setter<R>(override val property: DescriptorKMutableProperty0<R>) : DescriptorKProperty.Setter<R>(), KMutableProperty0.Setter<R> {
        override fun invoke(value: R): Unit = property.set(value)
    }
}
