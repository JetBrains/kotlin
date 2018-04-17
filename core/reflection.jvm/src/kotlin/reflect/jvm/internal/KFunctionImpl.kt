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
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.jvm.internal.CallableReference
import kotlin.jvm.internal.FunctionBase
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.internal.AnnotationConstructorCaller.CallMode.CALL_BY_NAME
import kotlin.reflect.jvm.internal.AnnotationConstructorCaller.CallMode.POSITIONAL_CALL
import kotlin.reflect.jvm.internal.AnnotationConstructorCaller.Origin.JAVA
import kotlin.reflect.jvm.internal.AnnotationConstructorCaller.Origin.KOTLIN
import kotlin.reflect.jvm.internal.JvmFunctionSignature.*

internal class KFunctionImpl private constructor(
    override val container: KDeclarationContainerImpl,
    name: String,
    private val signature: String,
    descriptorInitialValue: FunctionDescriptor?,
    private val boundReceiver: Any? = CallableReference.NO_RECEIVER
) : KCallableImpl<Any?>(), KFunction<Any?>, FunctionBase<Any?>, FunctionWithAllInvokes {
    constructor(container: KDeclarationContainerImpl, name: String, signature: String, boundReceiver: Any?)
            : this(container, name, signature, null, boundReceiver)

    constructor(container: KDeclarationContainerImpl, descriptor: FunctionDescriptor) : this(
        container,
        descriptor.name.asString(),
        RuntimeTypeMapper.mapSignature(descriptor).asString(),
        descriptor
    )

    override val isBound: Boolean get() = boundReceiver != CallableReference.NO_RECEIVER

    override val descriptor: FunctionDescriptor by ReflectProperties.lazySoft(descriptorInitialValue) {
        container.findFunctionDescriptor(name, signature)
    }

    override val name: String get() = descriptor.name.asString()

    override val caller: FunctionCaller<*> by ReflectProperties.lazySoft caller@{
        val jvmSignature = RuntimeTypeMapper.mapSignature(descriptor)
        val member: Member? = when (jvmSignature) {
            is KotlinConstructor -> {
                if (isAnnotationConstructor)
                    return@caller AnnotationConstructorCaller(container.jClass, parameters.map { it.name!! }, POSITIONAL_CALL, KOTLIN)
                container.findConstructorBySignature(jvmSignature.constructorDesc, descriptor.isPublicInBytecode)
            }
            is KotlinFunction ->
                container.findMethodBySignature(jvmSignature.methodName, jvmSignature.methodDesc, descriptor.isPublicInBytecode)
            is JavaMethod -> jvmSignature.method
            is JavaConstructor -> jvmSignature.constructor
            is FakeJavaAnnotationConstructor -> {
                val methods = jvmSignature.methods
                return@caller AnnotationConstructorCaller(container.jClass, methods.map { it.name }, POSITIONAL_CALL, JAVA, methods)
            }
        }

        when (member) {
            is Constructor<*> ->
                createConstructorCaller(member)
            is Method -> when {
                !Modifier.isStatic(member.modifiers) ->
                    createInstanceMethodCaller(member)
                descriptor.annotations.findAnnotation(JVM_STATIC) != null ->
                    createJvmStaticInObjectCaller(member)
                else ->
                    createStaticMethodCaller(member)
            }
            else -> throw KotlinReflectionInternalError("Could not compute caller for function: $descriptor (member = $member)")
        }
    }

    override val defaultCaller: FunctionCaller<*>? by ReflectProperties.lazySoft defaultCaller@{
        val jvmSignature = RuntimeTypeMapper.mapSignature(descriptor)
        val member: Member? = when (jvmSignature) {
            is KotlinFunction -> {
                container.findDefaultMethod(
                    jvmSignature.methodName, jvmSignature.methodDesc,
                    !Modifier.isStatic(caller.member!!.modifiers), descriptor.isPublicInBytecode
                )
            }
            is KotlinConstructor -> {
                if (isAnnotationConstructor)
                    return@defaultCaller AnnotationConstructorCaller(container.jClass, parameters.map { it.name!! }, CALL_BY_NAME, KOTLIN)
                container.findDefaultConstructor(jvmSignature.constructorDesc, descriptor.isPublicInBytecode)
            }
            is FakeJavaAnnotationConstructor -> {
                val methods = jvmSignature.methods
                return@defaultCaller AnnotationConstructorCaller(container.jClass, methods.map { it.name }, CALL_BY_NAME, JAVA, methods)
            }
            else -> {
                // Java methods, Java constructors and built-ins don't have $default methods
                null
            }
        }

        when (member) {
            is Constructor<*> ->
                createConstructorCaller(member)
            is Method -> when {
                // Note that static $default methods for @JvmStatic functions are generated differently in objects and companion objects.
                // In objects, $default's signature does _not_ contain the additional object instance parameter,
                // as opposed to companion objects where the first parameter is the companion object instance.
                descriptor.annotations.findAnnotation(JVM_STATIC) != null &&
                        !(descriptor.containingDeclaration as ClassDescriptor).isCompanionObject ->
                    createJvmStaticInObjectCaller(member)

                else ->
                    createStaticMethodCaller(member)
            }
            else -> null
        }
    }

    private fun createStaticMethodCaller(member: Method) =
        if (isBound) FunctionCaller.BoundStaticMethod(member, boundReceiver) else FunctionCaller.StaticMethod(member)

    private fun createJvmStaticInObjectCaller(member: Method) =
        if (isBound) FunctionCaller.BoundJvmStaticInObject(member) else FunctionCaller.JvmStaticInObject(member)

    private fun createInstanceMethodCaller(member: Method) =
        if (isBound) FunctionCaller.BoundInstanceMethod(member, boundReceiver) else FunctionCaller.InstanceMethod(member)

    private fun createConstructorCaller(member: Constructor<*>) =
        if (isBound) FunctionCaller.BoundConstructor(member, boundReceiver) else FunctionCaller.Constructor(member)

    override val arity: Int get() = caller.arity

    override val isInline: Boolean
        get() = descriptor.isInline

    override val isExternal: Boolean
        get() = descriptor.isExternal

    override val isOperator: Boolean
        get() = descriptor.isOperator

    override val isInfix: Boolean
        get() = descriptor.isInfix

    override val isSuspend: Boolean
        get() = descriptor.isSuspend

    override fun equals(other: Any?): Boolean {
        val that = other.asKFunctionImpl() ?: return false
        return container == that.container && name == that.name && signature == that.signature && boundReceiver == that.boundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderFunction(descriptor)
}
