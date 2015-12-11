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
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty0

internal open class KProperty0Impl<out R> : DescriptorBasedProperty<R>, KProperty0<R>, KPropertyImpl<R> {
    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : super(container, name, signature)

    private val getter_ = ReflectProperties.lazy { Getter(this) }

    override val getter: Getter<R> get() = getter_()

    override fun get(): R = getter.call()

    override fun invoke(): R = get()

    class Getter<out R>(override val property: KProperty0Impl<R>) : KPropertyImpl.Getter<R>(), KProperty0.Getter<R> {
        override fun invoke(): R = property.get()
    }
}

internal open class KMutableProperty0Impl<R> : KProperty0Impl<R>, KMutableProperty0<R>, KMutablePropertyImpl<R> {
    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : super(container, name, signature)

    private val setter_ = ReflectProperties.lazy { Setter(this) }

    override val setter: Setter<R> get() = setter_()

    override fun set(value: R) = setter.call(value)

    class Setter<R>(override val property: KMutableProperty0Impl<R>) : KMutablePropertyImpl.Setter<R>(), KMutableProperty0.Setter<R> {
        override fun invoke(value: R): Unit = property.set(value)
    }
}
