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
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.resolve.DescriptorFactory
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

interface KPropertyImpl<out R> : KProperty<R>, KCallableImpl<R> {
    val javaField: Field?

    val javaGetter: Method?

    override val getter: Getter<R>

    override val descriptor: PropertyDescriptor

    interface Accessor<out R> : KProperty.Accessor<R> {
        override val property: KPropertyImpl<R>
    }

    abstract class Getter<out R> : KProperty.Getter<R>, Accessor<R>, KCallableImpl<R> {
        override val name: String get() = "<get-${property.name}>"

        override val descriptor: PropertyGetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default getter created this way won't have any source information
            property.descriptor.getGetter() ?: DescriptorFactory.createDefaultGetter(property.descriptor)
        }

        override fun call(vararg args: Any?): R = property.call(*args)
    }
}


interface KMutablePropertyImpl<R> : KMutableProperty<R>, KPropertyImpl<R> {
    val javaSetter: Method?

    override val setter: Setter<R>

    abstract class Setter<R> : KMutableProperty.Setter<R>, KPropertyImpl.Accessor<R>, KCallableImpl<Unit> {
        abstract override val property: KMutablePropertyImpl<R>

        override val name: String get() = "<set-${property.name}>"

        override val descriptor: PropertySetterDescriptor by ReflectProperties.lazySoft {
            // TODO: default setter created this way won't have any source information
            property.descriptor.getSetter() ?: DescriptorFactory.createDefaultSetter(property.descriptor)
        }
    }
}
