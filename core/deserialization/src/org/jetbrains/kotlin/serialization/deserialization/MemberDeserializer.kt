/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.AnnotationWithTarget
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.annotations.AnnotationsImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyGetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertySetterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.serialization.deserialization.descriptors.*
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedMemberDescriptor.CoroutinesCompatibilityMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.contains

class MemberDeserializer(private val c: DeserializationContext) {
    private val annotationDeserializer = AnnotationDeserializer(c.components.moduleDescriptor, c.components.notFoundClasses)

    fun loadProperty(proto: ProtoBuf.Property): PropertyDescriptor {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)

        val property = DeserializedPropertyDescriptor(
            c.containingDeclaration, null,
            getAnnotations(proto, flags, AnnotatedCallableKind.PROPERTY),
            ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
            ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
            Flags.IS_VAR.get(flags),
            c.nameResolver.getName(proto.name),
            ProtoEnumFlags.memberKind(Flags.MEMBER_KIND.get(flags)),
            Flags.IS_LATEINIT.get(flags),
            Flags.IS_CONST.get(flags),
            Flags.IS_EXTERNAL_PROPERTY.get(flags),
            Flags.IS_DELEGATED.get(flags),
            Flags.IS_EXPECT_PROPERTY.get(flags),
            proto,
            c.nameResolver,
            c.typeTable,
            c.versionRequirementTable,
            c.containerSource
        )

        val local = c.childContext(property, proto.typeParameterList)

        val hasGetter = Flags.HAS_GETTER.get(flags)
        val receiverAnnotations = if (hasGetter && proto.hasReceiver())
            getReceiverParameterAnnotations(proto, AnnotatedCallableKind.PROPERTY_GETTER)
        else
            Annotations.EMPTY

        property.setType(
            local.typeDeserializer.type(proto.returnType(c.typeTable)),
            local.typeDeserializer.ownTypeParameters,
            getDispatchReceiverParameter(),
            proto.receiverType(c.typeTable)?.let { local.typeDeserializer.type(it, receiverAnnotations) }
        )

        val getter = if (hasGetter) {
            val getterFlags = proto.getterFlags
            val isNotDefault = proto.hasGetterFlags() && Flags.IS_NOT_DEFAULT.get(getterFlags)
            val isExternal = proto.hasGetterFlags() && Flags.IS_EXTERNAL_ACCESSOR.get(getterFlags)
            val isInline = proto.hasGetterFlags() && Flags.IS_INLINE_ACCESSOR.get(getterFlags)
            val annotations = getAnnotations(proto, getterFlags, AnnotatedCallableKind.PROPERTY_GETTER)
            val getter = if (isNotDefault) {
                PropertyGetterDescriptorImpl(
                    property,
                    annotations,
                    ProtoEnumFlags.modality(Flags.MODALITY.get(getterFlags)),
                    ProtoEnumFlags.visibility(Flags.VISIBILITY.get(getterFlags)),
                    /* isDefault = */ !isNotDefault,
                    /* isExternal = */ isExternal,
                    isInline,
                    property.kind, null, SourceElement.NO_SOURCE
                )
            } else {
                DescriptorFactory.createDefaultGetter(property, annotations)
            }
            getter.initialize(property.returnType)
            getter
        } else {
            null
        }

        val setter = if (Flags.HAS_SETTER.get(flags)) {
            val setterFlags = proto.setterFlags
            val isNotDefault = proto.hasSetterFlags() && Flags.IS_NOT_DEFAULT.get(setterFlags)
            val isExternal = proto.hasSetterFlags() && Flags.IS_EXTERNAL_ACCESSOR.get(setterFlags)
            val isInline = proto.hasGetterFlags() && Flags.IS_INLINE_ACCESSOR.get(setterFlags)
            val annotations = getAnnotations(proto, setterFlags, AnnotatedCallableKind.PROPERTY_SETTER)
            if (isNotDefault) {
                val setter = PropertySetterDescriptorImpl(
                    property,
                    annotations,
                    ProtoEnumFlags.modality(Flags.MODALITY.get(setterFlags)),
                    ProtoEnumFlags.visibility(Flags.VISIBILITY.get(setterFlags)),
                    /* isDefault = */ !isNotDefault,
                    /* isExternal = */ isExternal,
                    isInline,
                    property.kind, null, SourceElement.NO_SOURCE
                )
                val setterLocal = local.childContext(setter, listOf())
                val valueParameters = setterLocal.memberDeserializer.valueParameters(
                    listOf(proto.setterValueParameter), proto, AnnotatedCallableKind.PROPERTY_SETTER
                )
                setter.initialize(valueParameters.single())
                setter
            } else {
                DescriptorFactory.createDefaultSetter(property, annotations)
            }
        } else {
            null
        }

        if (Flags.HAS_CONSTANT.get(flags)) {
            property.setCompileTimeInitializer(
                c.storageManager.createNullableLazyValue {
                    val container = c.containingDeclaration.asProtoContainer()!!
                    c.components.annotationAndConstantLoader.loadPropertyConstant(container, proto, property.returnType)
                }
            )
        }

        property.initialize(getter, setter, property.checkExperimentalCoroutine(local.typeDeserializer))

        return property
    }

    private fun DeserializedMemberDescriptor.checkExperimentalCoroutine(
        typeDeserializer: TypeDeserializer
    ): DeserializedMemberDescriptor.CoroutinesCompatibilityMode {
        if (!versionAndReleaseCoroutinesMismatch()) return CoroutinesCompatibilityMode.COMPATIBLE

        forceUpperBoundsComputation(typeDeserializer)

        return if (typeDeserializer.experimentalSuspendFunctionTypeEncountered)
            CoroutinesCompatibilityMode.INCOMPATIBLE
        else
            CoroutinesCompatibilityMode.COMPATIBLE
    }

    private fun forceUpperBoundsComputation(typeDeserializer: TypeDeserializer) {
        typeDeserializer.ownTypeParameters.forEach { it.upperBounds }
    }

    private fun DeserializedSimpleFunctionDescriptor.initializeWithCoroutinesExperimentalityStatus(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: KotlinType?,
        modality: Modality?,
        visibility: Visibility,
        userDataMap: Map<out FunctionDescriptor.UserDataKey<*>, *>,
        isSuspend: Boolean
    ) {
        initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility,
            userDataMap,
            computeExperimentalityModeForFunctions(
                extensionReceiverParameter,
                unsubstitutedValueParameters,
                typeParameters,
                unsubstitutedReturnType,
                isSuspend
            )
        )
    }

    private fun DeserializedCallableMemberDescriptor.computeExperimentalityModeForFunctions(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        parameters: Collection<ValueParameterDescriptor>,
        typeParameters: Collection<TypeParameterDescriptor>,
        returnType: KotlinType?,
        isSuspend: Boolean
    ): DeserializedMemberDescriptor.CoroutinesCompatibilityMode {
        if (!versionAndReleaseCoroutinesMismatch()) return CoroutinesCompatibilityMode.COMPATIBLE
        if (fqNameOrNull() == KOTLIN_SUSPEND_BUILT_IN_FUNCTION_FQ_NAME) return CoroutinesCompatibilityMode.COMPATIBLE

        val types = parameters.map { it.type } + listOfNotNull(extensionReceiverParameter?.type)

        if (returnType?.containsSuspendFunctionType() == true) return CoroutinesCompatibilityMode.INCOMPATIBLE
        if (typeParameters.any { typeParameter -> typeParameter.upperBounds.any { it.containsSuspendFunctionType() } }) {
            return CoroutinesCompatibilityMode.INCOMPATIBLE
        }

        val maxFromParameters = types.map { type ->
            when {
                type.isSuspendFunctionType && type.arguments.size <= 3 ->
                    if (type.arguments.any { it.type.containsSuspendFunctionType() })
                        CoroutinesCompatibilityMode.INCOMPATIBLE
                    else
                        CoroutinesCompatibilityMode.NEEDS_WRAPPER

                type.containsSuspendFunctionType() -> CoroutinesCompatibilityMode.INCOMPATIBLE

                else -> CoroutinesCompatibilityMode.COMPATIBLE
            }
        }.max() ?: CoroutinesCompatibilityMode.COMPATIBLE

        return maxOf(
            if (isSuspend)
                CoroutinesCompatibilityMode.NEEDS_WRAPPER
            else
                CoroutinesCompatibilityMode.COMPATIBLE,
            maxFromParameters
        )
    }


    private fun KotlinType.containsSuspendFunctionType() = contains(UnwrappedType::isSuspendFunctionType)


    private fun DeserializedMemberDescriptor.versionAndReleaseCoroutinesMismatch(): Boolean =
        c.components.configuration.releaseCoroutines && versionRequirements.none {
            it.version == VersionRequirement.Version(1, 3) && it.kind == ProtoBuf.VersionRequirement.VersionKind.LANGUAGE_VERSION
        }

    private fun loadOldFlags(oldFlags: Int): Int {
        val lowSixBits = oldFlags and 0x3f
        val rest = (oldFlags shr 8) shl 6
        return lowSixBits + rest
    }

    fun loadFunction(proto: ProtoBuf.Function): SimpleFunctionDescriptor {
        val flags = if (proto.hasFlags()) proto.flags else loadOldFlags(proto.oldFlags)
        val annotations = getAnnotations(proto, flags, AnnotatedCallableKind.FUNCTION)
        val receiverAnnotations = if (proto.hasReceiver())
            getReceiverParameterAnnotations(proto, AnnotatedCallableKind.FUNCTION)
        else Annotations.EMPTY
        val function = DeserializedSimpleFunctionDescriptor(
            c.containingDeclaration, /* original = */ null, annotations, c.nameResolver.getName(proto.name),
            ProtoEnumFlags.memberKind(Flags.MEMBER_KIND.get(flags)), proto, c.nameResolver, c.typeTable, c.versionRequirementTable,
            c.containerSource
        )
        val local = c.childContext(function, proto.typeParameterList)

        function.initializeWithCoroutinesExperimentalityStatus(
            proto.receiverType(c.typeTable)?.let { local.typeDeserializer.type(it, receiverAnnotations) }?.let { receiverType ->
                DescriptorFactory.createExtensionReceiverParameterForCallable(function, receiverType, Annotations.EMPTY)
            },
            getDispatchReceiverParameter(),
            local.typeDeserializer.ownTypeParameters,
            local.memberDeserializer.valueParameters(proto.valueParameterList, proto, AnnotatedCallableKind.FUNCTION),
            local.typeDeserializer.type(proto.returnType(c.typeTable)),
            ProtoEnumFlags.modality(Flags.MODALITY.get(flags)),
            ProtoEnumFlags.visibility(Flags.VISIBILITY.get(flags)),
            emptyMap<FunctionDescriptor.UserDataKey<*>, Any?>(),
            Flags.IS_SUSPEND.get(flags)
        )
        function.isOperator = Flags.IS_OPERATOR.get(flags)
        function.isInfix = Flags.IS_INFIX.get(flags)
        function.isExternal = Flags.IS_EXTERNAL_FUNCTION.get(flags)
        function.isInline = Flags.IS_INLINE.get(flags)
        function.isTailrec = Flags.IS_TAILREC.get(flags)
        function.isSuspend = Flags.IS_SUSPEND.get(flags)
        function.isExpect = Flags.IS_EXPECT_FUNCTION.get(flags)

        val mapValueForContract =
            c.components.contractDeserializer.deserializeContractFromFunction(proto, function, c.typeTable, c.typeDeserializer)
        if (mapValueForContract != null) {
            function.putInUserDataMap(mapValueForContract.first, mapValueForContract.second)
        }

        return function
    }

    fun loadTypeAlias(proto: ProtoBuf.TypeAlias): TypeAliasDescriptor {
        val annotations = AnnotationsImpl(proto.annotationList.map { annotationDeserializer.deserializeAnnotation(it, c.nameResolver) })

        val visibility = ProtoEnumFlags.visibility(Flags.VISIBILITY.get(proto.flags))
        val typeAlias = DeserializedTypeAliasDescriptor(
            c.storageManager, c.containingDeclaration, annotations, c.nameResolver.getName(proto.name),
            visibility, proto, c.nameResolver, c.typeTable, c.versionRequirementTable, c.containerSource
        )

        val local = c.childContext(typeAlias, proto.typeParameterList)
        typeAlias.initialize(
            local.typeDeserializer.ownTypeParameters,
            local.typeDeserializer.simpleType(proto.underlyingType(c.typeTable)),
            local.typeDeserializer.simpleType(proto.expandedType(c.typeTable)),
            typeAlias.checkExperimentalCoroutine(local.typeDeserializer)
        )

        return typeAlias
    }

    private fun getDispatchReceiverParameter(): ReceiverParameterDescriptor? {
        return (c.containingDeclaration as? ClassDescriptor)?.thisAsReceiverParameter
    }

    fun loadConstructor(proto: ProtoBuf.Constructor, isPrimary: Boolean): ClassConstructorDescriptor {
        val classDescriptor = c.containingDeclaration as ClassDescriptor
        val descriptor = DeserializedClassConstructorDescriptor(
            classDescriptor, null, getAnnotations(proto, proto.flags, AnnotatedCallableKind.FUNCTION),
            isPrimary, CallableMemberDescriptor.Kind.DECLARATION, proto, c.nameResolver, c.typeTable, c.versionRequirementTable,
            c.containerSource
        )
        val local = c.childContext(descriptor, listOf())
        descriptor.initialize(
            local.memberDeserializer.valueParameters(proto.valueParameterList, proto, AnnotatedCallableKind.FUNCTION),
            ProtoEnumFlags.visibility(Flags.VISIBILITY.get(proto.flags))
        )
        descriptor.returnType = classDescriptor.defaultType

        val doesClassContainIncompatibility =
            (c.containingDeclaration as? DeserializedClassDescriptor)
                ?.c?.typeDeserializer?.experimentalSuspendFunctionTypeEncountered == true
                    && descriptor.versionAndReleaseCoroutinesMismatch()

        descriptor.coroutinesExperimentalCompatibilityMode =
                if (doesClassContainIncompatibility)
                    CoroutinesCompatibilityMode.INCOMPATIBLE
                else descriptor.computeExperimentalityModeForFunctions(
                    null, descriptor.valueParameters, descriptor.typeParameters,
                    descriptor.returnType, isSuspend = false
                )

        return descriptor
    }

    private fun getAnnotations(proto: MessageLite, flags: Int, kind: AnnotatedCallableKind): Annotations {
        if (!Flags.HAS_ANNOTATIONS.get(flags)) {
            return Annotations.EMPTY
        }
        return NonEmptyDeserializedAnnotationsWithPossibleTargets(c.storageManager) {
            c.containingDeclaration.asProtoContainer()?.let {
                c.components.annotationAndConstantLoader.loadCallableAnnotations(it, proto, kind).toList()
            }.orEmpty()
        }
    }

    private fun getReceiverParameterAnnotations(
        proto: MessageLite,
        kind: AnnotatedCallableKind,
        receiverTargetedKind: AnnotatedCallableKind = kind
    ): Annotations {
        return DeserializedAnnotationsWithPossibleTargets(c.storageManager) {
            c.containingDeclaration.asProtoContainer()?.let {
                c.components.annotationAndConstantLoader
                    .loadExtensionReceiverParameterAnnotations(it, proto, receiverTargetedKind)
                    .map { annotation -> AnnotationWithTarget(annotation, AnnotationUseSiteTarget.RECEIVER) }
                    .toList()
            }.orEmpty()
        }
    }

    private fun valueParameters(
        valueParameters: List<ProtoBuf.ValueParameter>,
        callable: MessageLite,
        kind: AnnotatedCallableKind
    ): List<ValueParameterDescriptor> {
        val callableDescriptor = c.containingDeclaration as CallableDescriptor
        val containerOfCallable = callableDescriptor.containingDeclaration.asProtoContainer()

        return valueParameters.mapIndexed { i, proto ->
            val flags = if (proto.hasFlags()) proto.flags else 0
            val annotations = if (containerOfCallable != null && Flags.HAS_ANNOTATIONS.get(flags)) {
                NonEmptyDeserializedAnnotations(c.storageManager) {
                    c.components.annotationAndConstantLoader
                        .loadValueParameterAnnotations(containerOfCallable, callable, kind, i, proto)
                        .toList()
                }
            } else Annotations.EMPTY
            ValueParameterDescriptorImpl(
                callableDescriptor, null, i,
                annotations,
                c.nameResolver.getName(proto.name),
                c.typeDeserializer.type(proto.type(c.typeTable)),
                Flags.DECLARES_DEFAULT_VALUE.get(flags),
                Flags.IS_CROSSINLINE.get(flags),
                Flags.IS_NOINLINE.get(flags),
                proto.varargElementType(c.typeTable)?.let { c.typeDeserializer.type(it) },
                SourceElement.NO_SOURCE
            )
        }.toList()
    }

    private fun DeclarationDescriptor.asProtoContainer(): ProtoContainer? = when (this) {
        is PackageFragmentDescriptor -> ProtoContainer.Package(fqName, c.nameResolver, c.typeTable, c.containerSource)
        is DeserializedClassDescriptor -> thisAsProtoContainer
        else -> null // TODO: support annotations on lambdas and their parameters
    }
}
