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
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.FunctionImpl
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.JvmFunctionSignature.BuiltInFunction
import kotlin.reflect.jvm.internal.JvmFunctionSignature.JavaConstructor
import kotlin.reflect.jvm.internal.JvmFunctionSignature.JavaMethod
import kotlin.reflect.jvm.internal.JvmFunctionSignature.KotlinFunction

open class KFunctionImpl protected constructor(
        private val container: KCallableContainerImpl,
        name: String,
        signature: String,
        descriptorInitialValue: FunctionDescriptor?
) : KFunction<Any?>, KCallableImpl<Any?>, FunctionImpl(),
        KLocalFunction<Any?>, KMemberFunction<Any, Any?>, KTopLevelExtensionFunction<Any?, Any?>, KTopLevelFunction<Any?> {
    constructor(container: KCallableContainerImpl, name: String, signature: String) : this(container, name, signature, null)

    constructor(container: KCallableContainerImpl, descriptor: FunctionDescriptor) : this(
            container, descriptor.name.asString(), RuntimeTypeMapper.mapSignature(descriptor).asString(), descriptor
    )

    override val descriptor: FunctionDescriptor by ReflectProperties.lazySoft<FunctionDescriptor>(descriptorInitialValue) {
        container.findFunctionDescriptor(name, signature)
    }

    override val name: String get() = descriptor.name.asString()

    override val caller: FunctionCaller by ReflectProperties.lazySoft {
        val jvmSignature = RuntimeTypeMapper.mapSignature(descriptor)
        val member: Member? = when (jvmSignature) {
            is KotlinFunction ->
                if (name == "<init>") container.findConstructorBySignature(jvmSignature.signature, jvmSignature.nameResolver,
                                                                           Visibilities.isPrivate(descriptor.visibility))
                else container.findMethodBySignature(jvmSignature.proto, jvmSignature.signature, jvmSignature.nameResolver,
                                                     Visibilities.isPrivate(descriptor.visibility))
            is JavaMethod -> jvmSignature.method
            is JavaConstructor -> jvmSignature.constructor
            is BuiltInFunction -> jvmSignature.getMember(container)
        }

        when (member) {
            is Constructor<*> -> FunctionCaller.Constructor(member)
            is Method -> when {
                !Modifier.isStatic(member.modifiers) -> FunctionCaller.InstanceMethod(member)
                descriptor.annotations.findAnnotation(PLATFORM_STATIC) != null -> FunctionCaller.PlatformStaticInObject(member)
                else -> FunctionCaller.StaticMethod(member)
            }
            else -> throw KotlinReflectionInternalError("Call is not yet supported for this function: $descriptor")
        }
    }

    override fun getArity(): Int {
        return descriptor.valueParameters.size() +
               (if (descriptor.dispatchReceiverParameter != null) 1 else 0) +
               (if (descriptor.extensionReceiverParameter != null) 1 else 0)
    }

    override fun equals(other: Any?): Boolean =
            other is KFunctionImpl && descriptor == other.descriptor

    override fun hashCode(): Int =
            descriptor.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderFunction(descriptor)
}
