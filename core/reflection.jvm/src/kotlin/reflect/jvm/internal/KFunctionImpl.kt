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

@file:suppress("DEPRECATED_SYMBOL_WITH_MESSAGE")
package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.load.java.sources.JavaSourceElement
import org.jetbrains.kotlin.load.java.structure.reflect.ReflectJavaConstructor
import org.jetbrains.kotlin.load.java.structure.reflect.ReflectJavaMethod
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.kotlin.serialization.jvm.JvmProtoBuf
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import kotlin.jvm.internal.FunctionImpl
import kotlin.reflect.*

open class KFunctionImpl protected constructor(
        container: KCallableContainerImpl,
        name: String,
        signature: String,
        descriptorInitialValue: FunctionDescriptor?
) : KFunction<Any?>, KCallableImpl<Any?>, FunctionImpl(),
        KLocalFunction<Any?>, KMemberFunction<Any, Any?>, KTopLevelExtensionFunction<Any?, Any?>, KTopLevelFunction<Any?> {
    constructor(container: KCallableContainerImpl, name: String, signature: String) : this(container, name, signature, null)

    constructor(container: KCallableContainerImpl, descriptor: FunctionDescriptor) : this(
            container, descriptor.getName().asString(), RuntimeTypeMapper.mapSignature(descriptor), descriptor
    )

    private data class FunctionProtoData(
            val proto: ProtoBuf.Callable,
            val nameResolver: NameResolver,
            val signature: JvmProtoBuf.JvmMethodSignature
    )

    override val descriptor: FunctionDescriptor by ReflectProperties.lazySoft<FunctionDescriptor>(descriptorInitialValue) {
        container.findFunctionDescriptor(name, signature)
    }

    // null if this is a function declared in a foreign (Java) class
    private val protoData: FunctionProtoData? by ReflectProperties.lazyWeak {
        val function = DescriptorUtils.unwrapFakeOverride(descriptor) as? DeserializedCallableMemberDescriptor
        if (function != null) {
            val proto = function.proto
            if (proto.hasExtension(JvmProtoBuf.methodSignature)) {
                return@lazyWeak FunctionProtoData(proto, function.nameResolver, proto.getExtension(JvmProtoBuf.methodSignature))
            }
        }
        null
    }

    internal val javaMethod: Method? by ReflectProperties.lazySoft {
        if (name != "<init>") {
            val proto = protoData
            if (proto != null) {
                container.findMethodBySignature(proto.proto, proto.signature, proto.nameResolver,
                                                Visibilities.isPrivate(descriptor.getVisibility()))
            }
            else {
                ((descriptor.getOriginal().getSource() as? JavaSourceElement)?.javaElement as? ReflectJavaMethod)?.member
            }
        }
        else null
    }

    internal val javaConstructor: Constructor<*>? by ReflectProperties.lazySoft {
        if (name == "<init>") {
            val proto = protoData
            if (proto != null) {
                return@lazySoft container.findConstructorBySignature(
                        proto.signature, proto.nameResolver, Visibilities.isPrivate(descriptor.getVisibility())
                )
            }
            else {
                ((descriptor.getOriginal().getSource() as? JavaSourceElement)?.javaElement as? ReflectJavaConstructor)?.member
            }
        }
        else null
    }

    override val name: String get() = descriptor.getName().asString()

    override fun getArity(): Int {
        // TODO: test?
        return descriptor.getValueParameters().size() +
               (if (descriptor.getDispatchReceiverParameter() != null) 1 else 0) +
               (if (descriptor.getExtensionReceiverParameter() != null) 1 else 0)
    }

    override fun equals(other: Any?): Boolean =
            other is KFunctionImpl && descriptor == other.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderFunction(descriptor)
}
