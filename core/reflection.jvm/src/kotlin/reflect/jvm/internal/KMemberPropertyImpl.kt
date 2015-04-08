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
import kotlin.reflect.IllegalPropertyAccessException
import kotlin.reflect.KMemberProperty
import kotlin.reflect.KMutableMemberProperty

open class KMemberPropertyImpl<T : Any, out R> : DescriptorBasedProperty, KMemberProperty<T, R>, KPropertyImpl<R> {
    constructor(container: KClassImpl<T>, name: String) : super(container, name, null)

    constructor(container: KClassImpl<T>, descriptor: PropertyDescriptor) : super(container, descriptor)

    override val name: String get() = descriptor.getName().asString()

    override fun get(instance: T): R {
        try {
            val getter = getter
            [suppress("UNCHECKED_CAST")]
            return if (getter != null) getter(instance) as R else field!!.get(instance) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }
}


class KMutableMemberPropertyImpl<T : Any, R> : KMemberPropertyImpl<T, R>, KMutableMemberProperty<T, R>, KMutablePropertyImpl<R> {
    constructor(container: KClassImpl<T>, name: String) : super(container, name)

    constructor(container: KClassImpl<T>, descriptor: PropertyDescriptor) : super(container, descriptor)

    override fun set(instance: T, value: R) {
        try {
            val setter = setter
            if (setter != null) setter(instance, value) else field!!.set(instance, value)
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }
}
