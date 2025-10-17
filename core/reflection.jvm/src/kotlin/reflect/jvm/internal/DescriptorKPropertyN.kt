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
import kotlin.reflect.KMutableProperty

internal open class DescriptorKPropertyN<out V> : DescriptorKProperty<V> {
    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    private val _getter = lazy(PUBLICATION) { Getter(this) }

    override val getter: Getter<V> get() = _getter.value

    class Getter<out V>(override val property: DescriptorKPropertyN<V>) : DescriptorKProperty.Getter<V>()
}

internal class DescriptorKMutablePropertyN<V> : DescriptorKPropertyN<V>, KMutableProperty<V> {
    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : super(container, descriptor)

    private val _setter = lazy(PUBLICATION) { Setter(this) }

    override val setter: Setter<V> get() = _setter.value

    class Setter<V>(override val property: DescriptorKMutablePropertyN<V>) : DescriptorKProperty.Setter<V>()
}
