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
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

open class KProperty1Impl<T, out R> : DescriptorBasedProperty<R>, KProperty1<T, R>, KPropertyImpl<R> {
    constructor(container: KCallableContainerImpl, name: String, signature: String) : super(container, name, signature)

    constructor(container: KCallableContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val getter by ReflectProperties.lazy { Getter(this) }

    // TODO: consider optimizing this, not to do complex checks on every access
    @suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_UNIT_OR_ANY" /* KT-8619 */)
    override fun get(receiver: T): R = reflectionCall {
        val getter = javaGetter
        return when {
            getter == null -> javaField!!.get(receiver)
            Modifier.isStatic(getter.modifiers) -> {
                // Workaround the case of platformStatic property in object, getter of which doesn't take a receiver
                if (getter.parameterTypes.isEmpty()) getter.invoke(null)
                else getter.invoke(null, receiver)
            }
            else -> getter.invoke(receiver)
        } as R
    }

    @suppress("UNCHECKED_CAST")
    override fun call(vararg args: Any?): R {
        require(args.size() == 1) { "Property $name expects one argument, but ${args.size()} were provided." }
        return get(args.single() as T)
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
        reflectionCall {
            val setter = javaSetter
            when {
                setter == null -> javaField!!.set(receiver, value)
                Modifier.isStatic(setter.modifiers) -> {
                    // Workaround the case of platformStatic property in object, setter of which doesn't take a receiver
                    if (setter.parameterTypes.size() == 1) setter.invoke(null, value)
                    else setter.invoke(null, receiver, value)
                }
                else -> setter.invoke(receiver, value)
            }
        }
    }

    class Setter<T, R>(override val property: KMutableProperty1Impl<T, R>) : KMutablePropertyImpl.Setter<R>(), KMutableProperty1.Setter<T, R> {
        override fun invoke(receiver: T, value: R): Unit = property.set(receiver, value)

        @suppress("UNCHECKED_CAST")
        override fun call(vararg args: Any?) {
            require(args.size() == 2) { "Property setter for ${property.name} expects two arguments, but ${args.size()} were provided." }
            property.set(args[0] as T, args[1] as R)
        }
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
