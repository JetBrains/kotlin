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
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import java.lang.reflect.Field
import java.lang.reflect.Method

abstract class DescriptorBasedProperty(computeDescriptor: () -> PropertyDescriptor) {
    protected abstract val container: KCallableContainerImpl

    protected abstract val name: String

    private data class PropertyProtoData(
            val proto: ProtoBuf.Callable,
            val nameResolver: NameResolver,
            val signature: JvmProtoBuf.JvmPropertySignature
    )

    protected val descriptor: PropertyDescriptor by ReflectProperties.lazySoft(computeDescriptor)

    // null if this is a property declared in a foreign (Java) class
    private val protoData: PropertyProtoData? by ReflectProperties.lazyWeak @p {(): PropertyProtoData? ->
        val property = DescriptorUtils.unwrapFakeOverride(descriptor) as? DeserializedPropertyDescriptor
        if (property != null) {
            val proto = property.proto
            if (proto.hasExtension(JvmProtoBuf.propertySignature)) {
                return@p PropertyProtoData(proto, property.nameResolver, proto.getExtension(JvmProtoBuf.propertySignature))
            }
        }
        null
    }

    open val field: Field? by ReflectProperties.lazySoft {
        val proto = protoData
        if (proto == null) container.jClass.getField(name)
        else if (!proto.signature.hasField()) null
        else container.findFieldBySignature(proto.proto, proto.signature.getField(), proto.nameResolver)
    }

    open val getter: Method? by ReflectProperties.lazySoft {
        val proto = protoData
        if (proto == null || !proto.signature.hasGetter()) null
        else container.findMethodBySignature(proto.signature.getGetter(), proto.nameResolver)
    }

    open val setter: Method? by ReflectProperties.lazySoft {
        val proto = protoData
        if (proto == null || !proto.signature.hasSetter()) null
        else container.findMethodBySignature(proto.signature.getSetter(), proto.nameResolver)
    }
}
