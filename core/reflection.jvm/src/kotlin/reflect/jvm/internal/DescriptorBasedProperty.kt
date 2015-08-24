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
import kotlin.reflect.jvm.internal.JvmPropertySignature.JavaField
import kotlin.reflect.jvm.internal.JvmPropertySignature.KotlinProperty

abstract class DescriptorBasedProperty<out R> protected constructor(
        internal val container: KDeclarationContainerImpl,
        name: String,
        signature: String,
        descriptorInitialValue: PropertyDescriptor?
) : KCallableImpl<R> {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : this(
            container, name, signature, null
    )

    constructor(container: KDeclarationContainerImpl, descriptor: PropertyDescriptor) : this(
            container,
            descriptor.name.asString(),
            RuntimeTypeMapper.mapPropertySignature(descriptor).asString(),
            descriptor
    )

    override val descriptor: PropertyDescriptor by ReflectProperties.lazySoft<PropertyDescriptor>(descriptorInitialValue) {
        container.findPropertyDescriptor(name, signature)
    }

    internal val javaField: Field? by ReflectProperties.lazySoft {
        val jvmSignature = RuntimeTypeMapper.mapPropertySignature(descriptor)
        when (jvmSignature) {
            is KotlinProperty -> {
                if (!jvmSignature.signature.hasField()) null
                else container.findFieldBySignature(jvmSignature.proto, jvmSignature.signature.field, jvmSignature.nameResolver)
            }
            is JavaField -> jvmSignature.field
        }
    }

    override fun equals(other: Any?): Boolean =
            other is DescriptorBasedProperty<*> && descriptor == other.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderProperty(descriptor)
}
