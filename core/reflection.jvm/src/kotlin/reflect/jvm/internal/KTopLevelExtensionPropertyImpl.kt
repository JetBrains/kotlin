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
import java.lang.reflect.Method
import kotlin.reflect.IllegalPropertyAccessException
import kotlin.reflect.KMutableTopLevelExtensionProperty
import kotlin.reflect.KTopLevelExtensionProperty

open class KTopLevelExtensionPropertyImpl<T, out R>(
        override val container: KPackageImpl,
        computeDescriptor: () -> PropertyDescriptor
) : DescriptorBasedProperty(computeDescriptor), KTopLevelExtensionProperty<T, R>, KPropertyImpl<R> {
    override val name: String get() = descriptor.getName().asString()

    override val getter: Method get() = super<DescriptorBasedProperty>.getter!!

    override fun get(receiver: T): R {
        try {
            [suppress("UNCHECKED_CAST")]
            return getter.invoke(null, receiver) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun equals(other: Any?): Boolean =
            other is KTopLevelExtensionPropertyImpl<*, *> && descriptor == other.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderProperty(descriptor)
}

class KMutableTopLevelExtensionPropertyImpl<T, R>(
        container: KPackageImpl,
        computeDescriptor: () -> PropertyDescriptor
) : KTopLevelExtensionPropertyImpl<T, R>(container, computeDescriptor), KMutableTopLevelExtensionProperty<T, R>, KMutablePropertyImpl<R> {
    override val setter: Method get() = super<KTopLevelExtensionPropertyImpl>.setter!!

    override fun set(receiver: T, value: R) {
        try {
            setter.invoke(null, receiver, value)
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }
}
