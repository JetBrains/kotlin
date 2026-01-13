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
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.shouldHideConstructorDueToValueClassTypeValueParameters
import java.lang.reflect.Constructor
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.CallableReference
import kotlin.jvm.internal.FunctionBase
import kotlin.reflect.KClass
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.internal.JvmFunctionSignature.*
import kotlin.reflect.jvm.internal.calls.*
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.CallMode.CALL_BY_NAME
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.CallMode.POSITIONAL_CALL
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.Origin.JAVA
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.Origin.KOTLIN
import kotlin.reflect.jvm.internal.types.DescriptorKType

internal class DescriptorKFunction private constructor(
    override val container: KDeclarationContainerImpl,
    name: String,
    override val signature: String,
    descriptorInitialValue: FunctionDescriptor?,
    override val rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : DescriptorKCallable<Any?>(overriddenStorage), ReflectKFunction,
    FunctionBase<Any?>, FunctionWithAllInvokes {

    constructor(container: KDeclarationContainerImpl, name: String, signature: String, boundReceiver: Any?)
            : this(container, name, signature, null, boundReceiver, KCallableOverriddenStorage.EMPTY)

    constructor(
        container: KDeclarationContainerImpl,
        descriptor: FunctionDescriptor,
        overriddenStorage: KCallableOverriddenStorage = KCallableOverriddenStorage.EMPTY,
    ) : this(
        container,
        descriptor.name.asString(),
        RuntimeTypeMapper.mapSignature(descriptor).asString(),
        descriptor,
        CallableReference.NO_RECEIVER,
        overriddenStorage
    )

    override val descriptor: FunctionDescriptor by ReflectProperties.lazySoft(descriptorInitialValue) {
        container.findFunctionDescriptor(name, signature)
    }

    override val name: String get() = descriptor.name.asString()

    override val caller: Caller<*> by lazy(PUBLICATION) caller@{
        @Suppress("USELESS_CAST")
        val member: Member? = when (val jvmSignature = RuntimeTypeMapper.mapSignature(descriptor)) {
            is KotlinConstructor -> {
                if (isAnnotationConstructor)
                    return@caller AnnotationConstructorCaller(container.jClass, parameters.map { it.name!! }, POSITIONAL_CALL, KOTLIN)
                container.findConstructorBySignature(jvmSignature.constructorDesc) as Member?
            }
            is KotlinFunction -> container.findMethodBySignature(jvmSignature.methodName, jvmSignature.methodDesc) as Member?
            is JavaMethod -> jvmSignature.method as Member
            is JavaConstructor -> jvmSignature.constructor as Member
            is FakeJavaAnnotationConstructor -> {
                val methods = jvmSignature.methods
                return@caller AnnotationConstructorCaller(container.jClass, methods.map { it.name }, POSITIONAL_CALL, JAVA, methods)
            }
        }

        when (member) {
            is Constructor<*> ->
                createConstructorCaller(member, descriptor, false)
            is Method -> when {
                !Modifier.isStatic(member.modifiers) ->
                    createInstanceMethodCaller(member)
                descriptor.annotations.findAnnotation(JVM_STATIC) != null ->
                    createJvmStaticInObjectCaller(member)
                else ->
                    createStaticMethodCaller(member, isCallByToValueClassMangledMethod = false)
            }
            else -> throw KotlinReflectionInternalError("Could not compute caller for function: $descriptor (member = $member)")
        }.createValueClassAwareCallerIfNeeded(this, isDefault = false, forbidUnboxingForIndices = emptyList())
    }

    override val callerWithDefaults: Caller<*>? by lazy(PUBLICATION) defaultCaller@{
        @Suppress("USELESS_CAST")
        val preventUnboxingForIndices = mutableListOf<Int>()
        val member: Member? = when (val jvmSignature = RuntimeTypeMapper.mapSignature(descriptor)) {
            is KotlinFunction -> run {
                getFunctionWithDefaultParametersForValueClassOverride(this)?.let { defaultImplsFunction ->
                    val defaultImplsFunctionName = defaultImplsFunction.signature.substringBefore('(')
                    val defaultImplsFunctionDesc = defaultImplsFunction.signature.substring(defaultImplsFunctionName.length)
                    val patchingResult =
                        patchJvmDescriptorByExtraBoxing(defaultImplsFunction, defaultImplsFunctionDesc)
                    preventUnboxingForIndices.addAll(patchingResult.boxedIndices)
                    return@run container.findDefaultMethod(
                        defaultImplsFunctionName,
                        patchingResult.newDescriptor,
                        true,
                        descriptor.extensionReceiverParameter != null
                    )
                }

                val patchingResult = patchJvmDescriptorByExtraBoxing(this, jvmSignature.methodDesc)
                preventUnboxingForIndices.addAll(patchingResult.boxedIndices)
                container.findDefaultMethod(
                    jvmSignature.methodName,
                    patchingResult.newDescriptor,
                    !Modifier.isStatic(caller.member!!.modifiers),
                    descriptor.extensionReceiverParameter != null
                ) as Member?
            }
            is KotlinConstructor -> {
                if (isAnnotationConstructor)
                    return@defaultCaller AnnotationConstructorCaller(container.jClass, parameters.map { it.name!! }, CALL_BY_NAME, KOTLIN)
                val patchingResult = patchJvmDescriptorByExtraBoxing(this, jvmSignature.constructorDesc)
                preventUnboxingForIndices.addAll(patchingResult.boxedIndices)
                container.findDefaultConstructor(patchingResult.newDescriptor) as Member?
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
                createConstructorCaller(member, descriptor, true)
            is Method -> when {
                // Note that static $default methods for @JvmStatic functions are generated differently in objects and companion objects.
                // In objects, $default's signature does _not_ contain the additional object instance parameter,
                // as opposed to companion objects where the first parameter is the companion object instance.
                descriptor.annotations.findAnnotation(JVM_STATIC) != null &&
                        !(descriptor.containingDeclaration as ClassDescriptor).isCompanionObject ->
                    createJvmStaticInObjectCaller(member)

                else -> {
                    createStaticMethodCaller(member, isCallByToValueClassMangledMethod = caller.isBoundInstanceCallWithValueClasses)
                }
            }
            else -> null
        }?.createValueClassAwareCallerIfNeeded(this, isDefault = true, preventUnboxingForIndices)
    }

    override val overridden: Collection<ReflectKFunction>
        get() = descriptor.overriddenDescriptors.map {
            val containerClass = (it.containingDeclaration as ClassDescriptor).toJavaClass()
                ?: throw KotlinReflectionInternalError("Unknown container class for overridden function: $this")
            DescriptorKFunction(containerClass.kotlin as KClassImpl<*>, it)
        }

    override fun shallowCopy(container: KDeclarationContainerImpl, overriddenStorage: KCallableOverriddenStorage): DescriptorKFunction =
        DescriptorKFunction(container, descriptor, overriddenStorage)

    private fun getFunctionWithDefaultParametersForValueClassOverride(function: ReflectKFunction): ReflectKFunction? {
        if (
            function.valueParameters.none { (it as? ReflectKParameter)?.declaresDefaultValue == true } &&
            (function.container as? KClass<*>)?.isValue == true &&
            Modifier.isStatic(caller.member!!.modifiers)
        ) {
            // firstOrNull is used to mimic the wrong behaviour of regular class reflection as KT-40327 is not fixed.
            // The behaviours equality is currently backed by codegen/box/reflection/callBy/brokenDefaultParametersFromDifferentFunctions.kt. 
            return function.overridden
                .firstOrNull { function -> function.valueParameters.any { (it as? ReflectKParameter)?.declaresDefaultValue == true } }
        }
        return null
    }

    // boundReceiver is unboxed receiver when the receiver is inline class.
    // However, when the expected dispatch receiver type is an interface,
    // the member belongs to the interface/DefaultImpls, so the receiver should not be unboxed.
    private fun useBoxedBoundReceiver(member: Method) =
        descriptor.dispatchReceiverParameter?.type?.isInlineClassType() == true && member.parameterTypes.firstOrNull()?.isInterface == true

    private fun createStaticMethodCaller(member: Method, isCallByToValueClassMangledMethod: Boolean): Caller<*> =
        if (isBound)
            CallerImpl.Method.BoundStatic(
                member, isCallByToValueClassMangledMethod, if (useBoxedBoundReceiver(member)) rawBoundReceiver else boundReceiver
            )
        else CallerImpl.Method.Static(member)

    private fun createJvmStaticInObjectCaller(member: Method) =
        if (isBound) CallerImpl.Method.BoundJvmStaticInObject(member) else CallerImpl.Method.JvmStaticInObject(member)

    private fun createInstanceMethodCaller(member: Method) =
        if (isBound) CallerImpl.Method.BoundInstance(member, boundReceiver) else CallerImpl.Method.Instance(member)

    private fun createConstructorCaller(
        member: Constructor<*>, descriptor: FunctionDescriptor, isDefault: Boolean
    ): CallerImpl<Constructor<*>> {
        return if (!isDefault && shouldHideConstructorDueToValueClassTypeValueParameters(descriptor)) {
            if (isBound)
                CallerImpl.AccessorForHiddenBoundConstructor(member, boundReceiver)
            else
                CallerImpl.AccessorForHiddenConstructor(member)
        } else {
            if (isBound)
                CallerImpl.BoundConstructor(member, boundReceiver)
            else
                CallerImpl.Constructor(member)
        }
    }

    override fun computeReturnType(): DescriptorKType =
        DescriptorKType(descriptor.returnType!!) {
            extractContinuationArgument() ?: caller.returnType
        }

    override val arity: Int get() = caller.arity

    override val isInline: Boolean
        get() = overriddenStorage.forceIsInline || descriptor.isInline

    override val isExternal: Boolean
        get() = overriddenStorage.forceIsExternal || descriptor.isExternal

    override val isOperator: Boolean
        get() = overriddenStorage.forceIsOperator || descriptor.isOperator

    override val isInfix: Boolean
        get() = overriddenStorage.forceIsInfix || descriptor.isInfix

    override val isSuspend: Boolean
        get() = descriptor.isSuspend

    override val isPrimaryConstructor: Boolean
        get() = (descriptor as? ConstructorDescriptor)?.isPrimary == true

    override fun equals(other: Any?): Boolean {
        val that = other.asReflectFunction() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderFunction(this)
}
