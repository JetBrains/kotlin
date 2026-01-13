/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.*
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.FunctionBase
import kotlin.metadata.KmType
import kotlin.metadata.KmValueParameter
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.internal.calls.*
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.CallMode.CALL_BY_NAME
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.CallMode.POSITIONAL_CALL
import kotlin.reflect.jvm.internal.calls.AnnotationConstructorCaller.Origin.KOTLIN
import kotlin.reflect.jvm.jvmErasure

internal abstract class KotlinKFunction(
    override val container: KDeclarationContainerImpl,
    override val signature: String,
    override val rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : KotlinKCallable<Any?>(overriddenStorage), ReflectKFunction, FunctionBase<Any?>, FunctionWithAllInvokes {
    protected abstract val contextParameters: List<KmValueParameter>
    protected abstract val extensionReceiverType: KmType?
    protected abstract val valueParameters: List<KmValueParameter>
    protected abstract val typeParameterTable: TypeParameterTable
    protected abstract val jvmSignature: JvmMethodSignature

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(contextParameters, extensionReceiverType, valueParameters, typeParameterTable, includeReceivers = true)
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound)
            computeParameters(contextParameters, extensionReceiverType, valueParameters, typeParameterTable, includeReceivers = false)
        else allParameters
    }

    override val typeParameters: List<KTypeParameter> get() = typeParameterTable.ownTypeParameters

    override val annotations: List<Annotation>
        get() {
            val member = caller.member as? AnnotatedElement ?: return emptyList()
            return member.annotations.toList().unwrapKotlinRepeatableAnnotations()
        }

    override val arity: Int get() = caller.arity

    override val overridden: Collection<ReflectKFunction>
        get() {
            require(container is KPackageImpl) {
                "Only top-level functions are supported for now: $this"
            }
            return emptyList()
        }

    override val caller: Caller<*> by lazy(PUBLICATION) {
        require(isConstructor || container is KPackageImpl) { "Only constructors and top-level functions are supported for now: $this" }
        val signature = jvmSignature
        val member: Member? =
            if (isConstructor && !container.isInlineClass()) {
                if (isAnnotationConstructor)
                    return@lazy AnnotationConstructorCaller(container.jClass, parameters.map { it.name!! }, POSITIONAL_CALL, KOTLIN)
                container.findConstructorBySignature(signature.descriptor)
            } else container.findMethodBySignature(signature.name, signature.descriptor)

        when (member) {
            is Constructor<*> -> createConstructorCaller(member, isDefault = false)
            is Method -> createStaticMethodCaller(member, isCallByToValueClassMangledMethod = false)
            else -> throw KotlinReflectionInternalError("Could not compute caller for function: $this")
        }.createValueClassAwareCallerIfNeeded(this, isDefault = false, forbidUnboxingForIndices = emptyList())
    }

    override val callerWithDefaults: Caller<*>? by lazy(PUBLICATION) {
        require(isConstructor || container is KPackageImpl) { "Only constructors and top-level functions are supported for now: $this" }
        val signature = jvmSignature
        val preventUnboxingForIndices = mutableListOf<Int>()
        val member: Member? =
            if (isConstructor && !container.isInlineClass()) {
                if (isAnnotationConstructor)
                    return@lazy AnnotationConstructorCaller(container.jClass, parameters.map { it.name!! }, CALL_BY_NAME, KOTLIN)
                val patchingResult = patchJvmDescriptorByExtraBoxing(this, jvmSignature.descriptor)
                preventUnboxingForIndices.addAll(patchingResult.boxedIndices)
                container.findDefaultConstructor(patchingResult.newDescriptor) as Member?
            } else {
                val patchingResult = patchJvmDescriptorByExtraBoxing(this, signature.descriptor)
                preventUnboxingForIndices.addAll(patchingResult.boxedIndices)
                container.findDefaultMethod(
                    signature.name, patchingResult.newDescriptor, !Modifier.isStatic(caller.member!!.modifiers),
                    allParameters.any { it.kind == KParameter.Kind.EXTENSION_RECEIVER },
                )
            }

        when (member) {
            is Constructor<*> -> createConstructorCaller(member, isDefault = true)
            is Method -> createStaticMethodCaller(member, isCallByToValueClassMangledMethod = caller.isBoundInstanceCallWithValueClasses)
            else -> null
        }?.createValueClassAwareCallerIfNeeded(this, isDefault = true, forbidUnboxingForIndices = preventUnboxingForIndices)
    }

    private fun KDeclarationContainerImpl.isInlineClass(): Boolean =
        this is KClassImpl<*> && isValue

    // boundReceiver is unboxed receiver when the receiver is inline class.
    // However, when the expected dispatch receiver type is an interface,
    // the member belongs to the interface/DefaultImpls, so the receiver should not be unboxed.
    private fun useBoxedBoundReceiver(member: Method): Boolean {
        require(container is KPackageImpl) { "Only top-level functions are supported for now: $this" }
        return false
    }

    private fun createStaticMethodCaller(member: Method, isCallByToValueClassMangledMethod: Boolean): Caller<*> =
        if (isBound)
            CallerImpl.Method.BoundStatic(
                member, isCallByToValueClassMangledMethod, if (useBoxedBoundReceiver(member)) rawBoundReceiver else boundReceiver
            )
        else CallerImpl.Method.Static(member)

    private fun createConstructorCaller(member: Constructor<*>, isDefault: Boolean): CallerImpl<Constructor<*>> {
        return if (!isDefault && this is KotlinKConstructor && shouldHideConstructorDueToValueClassTypeValueParameters(this)) {
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

    private fun shouldHideConstructorDueToValueClassTypeValueParameters(constructor: KotlinKConstructor): Boolean =
        constructor.visibility != KVisibility.PRIVATE &&
                constructor.parameters.any { it.type.jvmErasure.isValueClassThatRequiresMangling() }

    private fun KClass<*>.isValueClassThatRequiresMangling(): Boolean =
        isValue && this != Result::class

    override fun equals(other: Any?): Boolean {
        val that = other.asReflectFunction() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderFunction(this)
}
