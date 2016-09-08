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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.FunctionImpl
import kotlin.reflect.KFunction
import kotlin.reflect.KotlinReflectionInternalError
import kotlin.reflect.jvm.internal.JvmFunctionSignature.*

internal class KFunctionImpl private constructor(
        override val container: KDeclarationContainerImpl,
        name: String,
        private val signature: String,
        descriptorInitialValue: FunctionDescriptor?
) : KCallableImpl<Any?>(), KFunction<Any?>, FunctionImpl, FunctionWithAllInvokes {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String) : this(container, name, signature, null)

    constructor(container: KDeclarationContainerImpl, descriptor: FunctionDescriptor) : this(
            container, descriptor.name.asString(), RuntimeTypeMapper.mapSignature(descriptor).asString(), descriptor
    )

    override val descriptor: FunctionDescriptor by ReflectProperties.lazySoft(descriptorInitialValue) {
        container.findFunctionDescriptor(name, signature)
    }

    override val name: String get() = descriptor.name.asString()

    private fun isDeclared(): Boolean = Visibilities.isPrivate(descriptor.visibility)

    override val caller: FunctionCaller<*> by ReflectProperties.lazySoft {
        val jvmSignature = RuntimeTypeMapper.mapSignature(descriptor)
        val member: Member? = when (jvmSignature) {
            is KotlinConstructor -> container.findConstructorBySignature(jvmSignature.constructorDesc, isDeclared())
            is KotlinFunction -> container.findMethodBySignature(jvmSignature.methodName, jvmSignature.methodDesc, isDeclared())
            is JavaMethod -> jvmSignature.method
            is JavaConstructor -> jvmSignature.constructor
            is BuiltInFunction -> jvmSignature.getMember(container)
        }

        when (member) {
            is Constructor<*> -> FunctionCaller.Constructor(member)
            is Method -> when {
                !Modifier.isStatic(member.modifiers) -> FunctionCaller.InstanceMethod(member)
                descriptor.annotations.findAnnotation(JVM_STATIC) != null ->
                    FunctionCaller.JvmStaticInObject(member)

                else -> FunctionCaller.StaticMethod(member)
            }
            else -> throw KotlinReflectionInternalError("Call is not yet supported for this function: $descriptor (member = $member)")
        }
    }

    override val defaultCaller: FunctionCaller<*>? by ReflectProperties.lazySoft {
        val jvmSignature = RuntimeTypeMapper.mapSignature(descriptor)
        val member: Member? = when (jvmSignature) {
            is KotlinFunction -> {
                container.findDefaultMethod(jvmSignature.methodName, jvmSignature.methodDesc,
                                            !Modifier.isStatic(caller.member.modifiers), isDeclared())
            }
            is KotlinConstructor -> {
                container.findDefaultConstructor(jvmSignature.constructorDesc, isDeclared())
            }
            else -> {
                // Java methods, Java constructors and built-ins don't have $default methods
                null
            }
        }

        when (member) {
            is Constructor<*> -> FunctionCaller.Constructor(member)
            is Method -> when {
                // Note that static $default methods for @JvmStatic functions are generated differently in objects and companion objects.
                // In objects, $default's signature does _not_ contain the additional object instance parameter,
                // as opposed to companion objects where the first parameter is the companion object instance.
                descriptor.annotations.findAnnotation(JVM_STATIC) != null &&
                !(descriptor.containingDeclaration as ClassDescriptor).isCompanionObject ->
                    FunctionCaller.JvmStaticInObject(member)

                else -> FunctionCaller.StaticMethod(member)
            }
            else -> null
        }
    }

    override fun getArity() = caller.arity

    override val isInline: Boolean
        get() = descriptor.isInline

    override val isExternal: Boolean
        get() = descriptor.isExternal

    override val isOperator: Boolean
        get() = descriptor.isOperator

    override val isInfix: Boolean
        get() = descriptor.isInfix

    override val isTailrec: Boolean
        get() = descriptor.isTailrec

    override val isSuspend: Boolean
        get() = descriptor.isSuspend

    override fun equals(other: Any?): Boolean {
        val that = other.asKFunctionImpl() ?: return false
        return container == that.container && name == that.name && signature == that.signature
    }

    override fun hashCode(): Int =
            (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
            ReflectionObjectRenderer.renderFunction(descriptor)
}
