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
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.IllegalPropertyAccessException
import kotlin.reflect.KMutableProperty2
import kotlin.reflect.KProperty2

open class KProperty2Impl<D, E, out R> : DescriptorBasedProperty<R>, KProperty2<D, E, R>, KPropertyImpl<R> {
    constructor(container: KClassImpl<D>, name: String, signature: String) : super(container, name, signature)

    constructor(container: KClassImpl<D>, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val name: String get() = descriptor.getName().asString()

    override val getter by ReflectProperties.lazy { Getter(this) }

    override val javaGetter: Method get() = super.javaGetter!!

    override val javaField: Field? get() = null

    override fun get(receiver1: D, receiver2: E): R {
        try {
            @suppress("UNCHECKED_CAST")
            return javaGetter.invoke(receiver1, receiver2) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    class Getter<D, E, out R>(override val property: KProperty2Impl<D, E, R>) : KPropertyImpl.Getter<R>(), KProperty2.Getter<D, E, R> {
        override fun invoke(receiver1: D, receiver2: E): R = property.get(receiver1, receiver2)
    }
}


class KMutableProperty2Impl<D, E, R> : KProperty2Impl<D, E, R>, KMutableProperty2<D, E, R>, KMutablePropertyImpl<R> {
    constructor(container: KClassImpl<D>, name: String, signature: String) : super(container, name, signature)

    constructor(container: KClassImpl<D>, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val setter by ReflectProperties.lazy { Setter(this) }

    override val javaSetter: Method get() = super.javaSetter!!

    override fun set(receiver1: D, receiver2: E, value: R) {
        try {
            javaSetter.invoke(receiver1, receiver2, value)
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    class Setter<D, E, R>(override val property: KMutableProperty2Impl<D, E, R>) : KMutablePropertyImpl.Setter<R>(), KMutableProperty2.Setter<D, E, R> {
        override fun invoke(receiver1: D, receiver2: E, value: R): Unit = property.set(receiver1, receiver2, value)
    }
}
