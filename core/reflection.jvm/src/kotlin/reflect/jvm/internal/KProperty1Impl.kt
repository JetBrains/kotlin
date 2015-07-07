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
import java.lang.reflect.Modifier
import kotlin.jvm.internal.MutablePropertyReference1
import kotlin.jvm.internal.PropertyReference1
import kotlin.reflect.IllegalPropertyAccessException
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

open class KProperty1Impl<T, out R> : DescriptorBasedProperty<R>, KProperty1<T, R>, KPropertyImpl<R> {
    constructor(container: KCallableContainerImpl, name: String, signature: String) : super(container, name, signature)

    constructor(container: KCallableContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val name: String get() = descriptor.getName().asString()

    override val getter by ReflectProperties.lazy { Getter(this) }

    // TODO: consider optimizing this, not to do complex checks on every access
    @suppress("UNCHECKED_CAST")
    override fun get(receiver: T): R {
        try {
            val getter = javaGetter ?:
                         return javaField!!.get(receiver) as R

            if (Modifier.isStatic(getter.getModifiers())) {
                // Workaround the case of platformStatic property in object, getter of which doesn't take a receiver
                if (getter.getParameterTypes().isEmpty()) {
                    return getter.invoke(null) as R
                }

                return getter.invoke(null, receiver) as R
            }

            return getter.invoke(receiver) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    class Getter<T, out R>(override val property: KProperty1Impl<T, R>) : KPropertyImpl.Getter<R>(), KProperty1.Getter<T, R> {
        override fun invoke(receiver: T): R = property.get(receiver)
    }
}


open class KMutableProperty1Impl<T, R> : KProperty1Impl<T, R>, KMutableProperty1<T, R>, KMutablePropertyImpl<R> {
    constructor(container: KCallableContainerImpl, name: String, signature: String) : super(container, name, signature)

    constructor(container: KCallableContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val setter by ReflectProperties.lazy { Setter(this) }

    override fun set(receiver: T, value: R) {
        try {
            val setter = javaSetter ?:
                         return javaField!!.set(receiver, value)

            if (Modifier.isStatic(setter.getModifiers())) {
                // Workaround the case of platformStatic property in object, setter of which doesn't take a receiver
                if (setter.getParameterTypes().size() == 1) {
                    setter.invoke(null, value)
                }
                else {
                    setter.invoke(null, receiver, value)
                }
            }
            else {
                setter.invoke(receiver, value)
            }
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    class Setter<T, R>(override val property: KMutableProperty1Impl<T, R>) : KMutablePropertyImpl.Setter<R>(), KMutableProperty1.Setter<T, R> {
        override fun invoke(receiver: T, value: R): Unit = property.set(receiver, value)
    }
}


class KProperty1FromReferenceImpl(
        val reference: PropertyReference1
) : KProperty1Impl<Any?, Any?>(
        reference.getOwner() as KCallableContainerImpl,
        reference.getName(),
        reference.getSignature()
) {
    override val name: String get() = reference.getName()

    override fun get(receiver: Any?): Any? = reference.get(receiver)
}


class KMutableProperty1FromReferenceImpl(
        val reference: MutablePropertyReference1
) : KMutableProperty1Impl<Any?, Any?>(
        reference.getOwner() as KCallableContainerImpl,
        reference.getName(),
        reference.getSignature()
) {
    override val name: String get() = reference.getName()

    override fun get(receiver: Any?): Any? = reference.get(receiver)

    override fun set(receiver: Any?, value: Any?) {
        reference.set(receiver, value)
    }
}
