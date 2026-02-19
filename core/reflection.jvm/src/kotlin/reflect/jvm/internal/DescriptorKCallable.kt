/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.load.java.JavaDescriptorVisibilities
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.jvm.internal.types.DescriptorKType
import kotlin.reflect.jvm.internal.types.KTypeSubstitutor

internal abstract class DescriptorKCallable<out R>(
    internal val overriddenStorage: KCallableOverriddenStorage,
) : ReflectKCallableImpl<R>() {
    abstract val descriptor: CallableMemberDescriptor

    protected abstract fun computeReturnType(): DescriptorKType

    internal abstract fun shallowCopy(
        container: KDeclarationContainerImpl,
        overriddenStorage: KCallableOverriddenStorage,
    ): DescriptorKCallable<R>

    private val _annotations = ReflectProperties.lazySoft { descriptor.computeAnnotations() }

    override val annotations: List<Annotation> get() = _annotations()

    private val _allParameters = ReflectProperties.lazySoft { computeParameters(includeReceivers = true) }

    override val allParameters: List<KParameter> get() = _allParameters()

    private val _parameters = ReflectProperties.lazySoft {
        if (isBound) computeParameters(includeReceivers = false) else allParameters
    }

    final override val parameters: List<KParameter> get() = _parameters()

    private fun computeParameters(includeReceivers: Boolean): List<KParameter> {
        val descriptor = descriptor
        val result = ArrayList<KParameter>()
        if (includeReceivers) {
            val instanceReceiver = instanceReceiverParameter
            if (instanceReceiver != null) {
                result.add(DescriptorKParameter(this, result.size, KParameter.Kind.INSTANCE) { instanceReceiver })
            }

            val contextParameters = descriptor.computeContextParameters()
            for (i in contextParameters.indices) {
                result.add(DescriptorKParameter(this, result.size, KParameter.Kind.CONTEXT) { contextParameters[i] })
            }

            val extensionReceiver = descriptor.extensionReceiverParameter
            if (extensionReceiver != null) {
                result.add(DescriptorKParameter(this, result.size, KParameter.Kind.EXTENSION_RECEIVER) { extensionReceiver })
            }
        }

        for (i in descriptor.valueParameters.indices) {
            result.add(DescriptorKParameter(this, result.size, KParameter.Kind.VALUE) { descriptor.valueParameters[i] })
        }

        // Constructor parameters of Java annotations are not ordered in any way, we order them by name here to be more stable.
        // Note that positional call (via "call") is not allowed unless there's a single non-"value" parameter,
        // so the order of parameters of Java annotation constructors here can be arbitrary
        if (isAnnotationConstructor && descriptor is JavaCallableMemberDescriptor) {
            result.sortBy { it.name }
        }

        result.trimToSize()
        return result
    }

    private fun CallableMemberDescriptor.computeContextParameters(): List<ValueParameterDescriptor> {
        val (nameResolver, contextParameters) = when (this) {
            is DeserializedSimpleFunctionDescriptor -> nameResolver to proto.contextParameterList
            is DeserializedPropertyDescriptor -> nameResolver to proto.contextParameterList
            is PropertyAccessorDescriptor -> (correspondingProperty as? DeserializedPropertyDescriptor)?.let {
                it.nameResolver to it.proto.contextParameterList
            }
            else -> null
        } ?: return emptyList()
        return contextReceiverParameters.mapIndexed { index, parameter ->
            ValueParameterDescriptorImpl(
                this, null, index,
                parameter.annotations,
                Name.guessByFirstCharacter(nameResolver.getString(contextParameters[index].name)),
                parameter.type,
                declaresDefaultValue = false,
                isCrossinline = false,
                isNoinline = false,
                varargElementType = null,
                parameter.source,
            )
        }
    }

    private val _returnType = ReflectProperties.lazySoft {
        val type = computeReturnType()
        overriddenStorage.getTypeSubstitutor(typeParameters, memberNameForDebug = name).substitute(type).type
            ?: starProjectionInTopLevelTypeIsNotPossible(containerForDebug = name)
    }

    final override val returnType: KType
        get() = _returnType()

    private val _typeParameters = ReflectProperties.lazySoft {
        val typeParametersWithNotYetSubstitutedUpperBounds =
            descriptor.typeParameters.map { descriptor -> KTypeParameterImpl(this, descriptor) }
        val substitutor = overriddenStorage.getTypeSubstitutor(typeParametersWithNotYetSubstitutedUpperBounds, memberNameForDebug = name)
        for (typeParameter in typeParametersWithNotYetSubstitutedUpperBounds) {
            typeParameter.upperBounds = typeParameter.upperBounds.map { type ->
                substitutor.substitute(type).type ?: starProjectionInTopLevelTypeIsNotPossible(containerForDebug = container)
            }
        }
        typeParametersWithNotYetSubstitutedUpperBounds
    }

    override val typeParameters: List<KTypeParameter>
        get() = _typeParameters()

    override val visibility: KVisibility?
        get() = descriptor.visibility.toKVisibility()

    internal val modality: Modality
        get() = overriddenStorage.modality ?: descriptor.modality

    internal val isPackagePrivate: Boolean
        get() = descriptor.visibility == JavaDescriptorVisibilities.PACKAGE_VISIBILITY

    final override val isFinal: Boolean
        get() = modality == Modality.FINAL

    final override val isOpen: Boolean
        get() = modality == Modality.OPEN

    final override val isAbstract: Boolean
        get() = modality == Modality.ABSTRACT
}

internal data class KCallableOverriddenStorage(
    val instanceReceiverParameter: ReceiverParameterDescriptor?,
    private val classTypeParametersSubstitutor: KTypeSubstitutor,
    val modality: Modality?,
    val originalContainerIfFakeOverride: KDeclarationContainerImpl?,
    private val originalCallableTypeParameters: List<KTypeParameter>,

    val forceIsExternal: Boolean,
    val forceIsOperator: Boolean,
    val forceIsInfix: Boolean,
    val forceIsInline: Boolean,
) {
    companion object {
        val EMPTY = KCallableOverriddenStorage(
            null,
            KTypeSubstitutor.EMPTY,
            null,
            originalContainerIfFakeOverride = null,
            originalCallableTypeParameters = emptyList(),
            forceIsExternal = false,
            forceIsOperator = false,
            forceIsInfix = false,
            forceIsInline = false,
        )
    }

    val isFakeOverride: Boolean get() = originalContainerIfFakeOverride != null

    fun withChainedClassTypeParametersSubstitutor(substitutor: KTypeSubstitutor): KCallableOverriddenStorage =
        copy(classTypeParametersSubstitutor = classTypeParametersSubstitutor.chainedWith(substitutor))

    fun getTypeSubstitutor(callableTypeParameters: List<KTypeParameter>, memberNameForDebug: String): KTypeSubstitutor =
        originalCallableTypeParameters.substitutedWith(callableTypeParameters)
            ?.disjointSumWith(classTypeParametersSubstitutor, memberNameForDebug)
            ?: classTypeParametersSubstitutor
}
