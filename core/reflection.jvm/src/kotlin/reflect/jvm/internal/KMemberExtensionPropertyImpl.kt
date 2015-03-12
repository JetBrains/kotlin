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
import kotlin.reflect.KMemberExtensionProperty
import kotlin.reflect.KMutableMemberExtensionProperty

open class KMemberExtensionPropertyImpl<D : Any, E, out R>(
        override val container: KClassImpl<D>,
        computeDescriptor: () -> PropertyDescriptor
) : DescriptorBasedProperty(computeDescriptor), KMemberExtensionProperty<D, E, R>, KPropertyImpl<R> {
    override val name: String get() = descriptor.getName().asString()

    override val getter: Method get() = super<DescriptorBasedProperty>.getter!!

    override val field: Field? get() = null

    override fun get(dispatchReceiver: D, extensionReceiver: E): R {
        try {
            [suppress("UNCHECKED_CAST")]
            return getter.invoke(dispatchReceiver, extensionReceiver) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun equals(other: Any?): Boolean =
            other is KMemberExtensionPropertyImpl<*, *, *> && descriptor == other.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderProperty(descriptor)
}


class KMutableMemberExtensionPropertyImpl<D : Any, E, R>(
        container: KClassImpl<D>,
        computeDescriptor: () -> PropertyDescriptor
) : KMemberExtensionPropertyImpl<D, E, R>(container, computeDescriptor), KMutableMemberExtensionProperty<D, E, R>, KMutablePropertyImpl<R> {
    override val setter: Method get() = super<KMemberExtensionPropertyImpl>.setter!!

    override fun set(dispatchReceiver: D, extensionReceiver: E, value: R) {
        try {
            setter.invoke(dispatchReceiver, extensionReceiver, value)
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }
}
