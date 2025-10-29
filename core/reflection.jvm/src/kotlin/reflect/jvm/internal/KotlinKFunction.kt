/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.jvm.internal.FunctionBase
import kotlin.metadata.*
import kotlin.metadata.jvm.signature
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl
import kotlin.reflect.jvm.internal.calls.arity
import kotlin.reflect.jvm.internal.calls.createValueClassAwareCallerIfNeeded

@OptIn(ExperimentalContextParameters::class)
internal class KotlinKFunction(
    override val container: KDeclarationContainerImpl,
    override val signature: String,
    override val rawBoundReceiver: Any?,
    private val kmFunction: KmFunction,
) : KotlinKCallable<Any?>(), ReflectKFunction, FunctionBase<Any?>, FunctionWithAllInvokes {
    override val name: String
        get() = kmFunction.name

    override val allParameters: List<KParameter> by lazy(PUBLICATION) {
        computeParameters(
            kmFunction.contextParameters, kmFunction.receiverParameterType, kmFunction.valueParameters, typeParameterTable.value,
            includeReceivers = true,
        )
    }

    override val parameters: List<KParameter> by lazy(PUBLICATION) {
        if (isBound) computeParameters(
            kmFunction.contextParameters, kmFunction.receiverParameterType, kmFunction.valueParameters, typeParameterTable.value,
            includeReceivers = false,
        )
        else allParameters
    }

    override val returnType: KType by lazy(PUBLICATION) {
        kmFunction.returnType.toKType(container.jClass.classLoader, typeParameterTable.value) {
            extractContinuationArgument() ?: caller.returnType
        }
    }

    val typeParameterTable: Lazy<TypeParameterTable> = lazy(PUBLICATION) {
        val parent = (container as? KClassImpl<*>)?.typeParameterTable
        TypeParameterTable.create(kmFunction.typeParameters, parent, this, container.jClass.classLoader)
    }

    override val typeParameters: List<KTypeParameter> get() = typeParameterTable.value.ownTypeParameters

    override val visibility: KVisibility? get() = kmFunction.visibility.toKVisibility()
    override val modality: Modality get() = kmFunction.modality
    override val isSuspend: Boolean get() = kmFunction.isSuspend
    override val isInline: Boolean get() = kmFunction.isInline
    override val isExternal: Boolean get() = kmFunction.isExternal
    override val isOperator: Boolean get() = kmFunction.isOperator
    override val isInfix: Boolean get() = kmFunction.isInfix

    override val annotations: List<Annotation>
        get() {
            val signature = kmFunction.signature
                ?: throw KotlinReflectionInternalError("No signature for function: $this")
            val member = container.findMethodBySignature(signature.name, signature.descriptor)
                ?: return emptyList()
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
        // TODO: will fail for builtins
        val signature = kmFunction.signature
            ?: throw KotlinReflectionInternalError("No signature for function: $this")
        val member = container.findMethodBySignature(signature.name, signature.descriptor) as Method
        createStaticMethodCaller(member, isCallByToValueClassMangledMethod = false)
            .createValueClassAwareCallerIfNeeded(this, isDefault = false, forbidUnboxingForIndices = emptyList() /* TODO */)
    }

    override val defaultCaller: Caller<*>? by lazy(PUBLICATION) {
        require(container is KPackageImpl) { "Only top-level functions are supported for now: $this" }
        val signature = kmFunction.signature
            ?: throw KotlinReflectionInternalError("No signature for function: $this")
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
