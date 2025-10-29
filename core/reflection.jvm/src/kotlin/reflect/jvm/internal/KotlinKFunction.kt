/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.FunctionBase
import kotlin.metadata.KmType
import kotlin.metadata.KmValueParameter
import kotlin.metadata.jvm.JvmMethodSignature
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeParameter
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl
import kotlin.reflect.jvm.internal.calls.arity
import kotlin.reflect.jvm.internal.calls.createValueClassAwareCallerIfNeeded

internal abstract class KotlinKFunction(
    override val container: KDeclarationContainerImpl,
    override val signature: String,
    override val rawBoundReceiver: Any?,
) : KotlinKCallable<Any?>(), ReflectKFunction, FunctionBase<Any?>, FunctionWithAllInvokes {
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
        require(container is KPackageImpl) { "Only top-level functions are supported for now: $this" }
        val signature = jvmSignature
        val member = container.findMethodBySignature(signature.name, signature.descriptor) as Method
        createStaticMethodCaller(member, isCallByToValueClassMangledMethod = false)
            .createValueClassAwareCallerIfNeeded(this, isDefault = false, forbidUnboxingForIndices = emptyList())
    }

    override val defaultCaller: Caller<*>? by lazy(PUBLICATION) {
        require(container is KPackageImpl) { "Only top-level functions are supported for now: $this" }
        val signature = jvmSignature
        val patchingResult = patchJvmDescriptorByExtraBoxing(this, signature.descriptor)
        val member = container.findDefaultMethod(
            signature.name, patchingResult.newDescriptor, !Modifier.isStatic(caller.member!!.modifiers),
            allParameters.any { it.kind == KParameter.Kind.EXTENSION_RECEIVER },
        )

        member?.let {
            createStaticMethodCaller(it, isCallByToValueClassMangledMethod = caller.isBoundInstanceCallWithValueClasses)
        }?.createValueClassAwareCallerIfNeeded(this, isDefault = true, forbidUnboxingForIndices = patchingResult.boxedIndices.toList())
    }

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

    override fun equals(other: Any?): Boolean {
        val that = other.asReflectFunction() ?: return false
        return container == that.container && name == that.name && signature == that.signature && rawBoundReceiver == that.rawBoundReceiver
    }

    override fun hashCode(): Int =
        (container.hashCode() * 31 + name.hashCode()) * 31 + signature.hashCode()

    override fun toString(): String =
        ReflectionObjectRenderer.renderFunction(this)
}
