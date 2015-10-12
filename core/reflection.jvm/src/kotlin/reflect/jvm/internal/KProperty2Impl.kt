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
import kotlin.jvm.internal.MutablePropertyReference2
import kotlin.jvm.internal.PropertyReference2
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.KProperty2

internal open class KProperty2Impl<D, E, out R> : DescriptorBasedProperty<R>, KProperty2<D, E, R>, KPropertyImpl<R> {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : super(container, name, signature)

    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val getter by ReflectProperties.lazy { Getter(this) }

    override fun get(receiver1: D, receiver2: E): R = getter.call(receiver1, receiver2)

    class Getter<D, E, out R>(override val property: KProperty2Impl<D, E, R>) : KPropertyImpl.Getter<R>(), KProperty2.Getter<D, E, R> {
        override fun invoke(receiver1: D, receiver2: E): R = property.get(receiver1, receiver2)
    }
}

internal open class KMutableProperty2Impl<D, E, R> : KProperty2Impl<D, E, R>, KMutableProperty2<D, E, R>, KMutablePropertyImpl<R> {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : super(container, name, signature)

    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val setter by ReflectProperties.lazy { Setter(this) }

    override fun set(receiver1: D, receiver2: E, value: R) = setter.call(receiver1, receiver2, value)

    class Setter<D, E, R>(override val property: KMutableProperty2Impl<D, E, R>) : KMutablePropertyImpl.Setter<R>(), KMutableProperty2.Setter<D, E, R> {
        override fun invoke(receiver1: D, receiver2: E, value: R): Unit = property.set(receiver1, receiver2, value)
    }
}


internal class KProperty2FromReferenceImpl(
        val reference: PropertyReference2
) : KProperty2Impl<Any?, Any?, Any?>(
        reference.owner as KDeclarationContainerImpl,
        reference.name,
        reference.signature
) {
    override val name: String get() = reference.name

    override fun get(receiver1: Any?, receiver2: Any?): Any? = reference.get(receiver1, receiver2)
}

internal class KMutableProperty2FromReferenceImpl(
        val reference: MutablePropertyReference2
) : KMutableProperty2Impl<Any?, Any?, Any?>(
        reference.owner as KDeclarationContainerImpl,
        reference.name,
        reference.signature
) {
    override val name: String get() = reference.name

    override fun get(receiver1: Any?, receiver2: Any?): Any? = reference.get(receiver1, receiver2)

    override fun set(receiver1: Any?, receiver2: Any?, value: Any?) {
        reference.set(receiver1, receiver2, value)
    }
}
