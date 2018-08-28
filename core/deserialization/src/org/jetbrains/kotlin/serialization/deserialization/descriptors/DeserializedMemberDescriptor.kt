/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*

interface DeserializedMemberDescriptor : MemberDescriptor {
    val proto: MessageLite

    val nameResolver: NameResolver

    val typeTable: TypeTable

    val versionRequirementTable: VersionRequirementTable

    val versionRequirements: List<VersionRequirement>
        get() = VersionRequirement.create(proto, nameResolver, versionRequirementTable)

    // Information about the origin of this callable's container (class or package part on JVM) or null if there's no such information.
    // TODO: merge with sourceElement of containingDeclaration
    val containerSource: DeserializedContainerSource?

    val coroutinesExperimentalCompatibilityMode: CoroutinesCompatibilityMode

    enum class CoroutinesCompatibilityMode {
        COMPATIBLE,
        NEEDS_WRAPPER,
        INCOMPATIBLE
    }
}

interface DeserializedContainerSource : SourceElement {
    // Non-null if this container is loaded from a class with an incompatible binary version
    val incompatibility: IncompatibleVersionErrorData<*>?

    // True iff this is container is "invisible" because it's loaded from a pre-release class and this compiler is a release
    val isPreReleaseInvisible: Boolean

    // This string should only be used in error messages
    val presentableString: String
}

interface DeserializedCallableMemberDescriptor : DeserializedMemberDescriptor, CallableMemberDescriptor

class DeserializedSimpleFunctionDescriptor(
    containingDeclaration: DeclarationDescriptor,
    original: SimpleFunctionDescriptor?,
    annotations: Annotations,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    override val proto: ProtoBuf.Function,
    override val nameResolver: NameResolver,
    override val typeTable: TypeTable,
    override val versionRequirementTable: VersionRequirementTable,
    override val containerSource: DeserializedContainerSource?,
    source: SourceElement? = null
) : DeserializedCallableMemberDescriptor,
    SimpleFunctionDescriptorImpl(
        containingDeclaration, original, annotations, name, kind,
        source ?: SourceElement.NO_SOURCE
    ) {

    override var coroutinesExperimentalCompatibilityMode = DeserializedMemberDescriptor.CoroutinesCompatibilityMode.COMPATIBLE
        private set

    fun initialize(
        extensionReceiverParameter: ReceiverParameterDescriptor?,
        dispatchReceiverParameter: ReceiverParameterDescriptor?,
        typeParameters: List<TypeParameterDescriptor>,
        unsubstitutedValueParameters: List<ValueParameterDescriptor>,
        unsubstitutedReturnType: KotlinType?,
        modality: Modality?,
        visibility: Visibility,
        userDataMap: Map<out FunctionDescriptor.UserDataKey<*>, *>,
        isExperimentalCoroutineInReleaseEnvironment: DeserializedMemberDescriptor.CoroutinesCompatibilityMode
    ): SimpleFunctionDescriptorImpl {
        return super.initialize(
            extensionReceiverParameter,
            dispatchReceiverParameter,
            typeParameters,
            unsubstitutedValueParameters,
            unsubstitutedReturnType,
            modality,
            visibility,
            userDataMap
        ).also {
            this.coroutinesExperimentalCompatibilityMode = isExperimentalCoroutineInReleaseEnvironment
        }
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): FunctionDescriptorImpl {
        return DeserializedSimpleFunctionDescriptor(
            newOwner, original as SimpleFunctionDescriptor?, annotations, newName ?: name, kind,
            proto, nameResolver, typeTable, versionRequirementTable, containerSource, source
        ).also {
            it.coroutinesExperimentalCompatibilityMode = coroutinesExperimentalCompatibilityMode
        }
    }
}

class DeserializedPropertyDescriptor(
    containingDeclaration: DeclarationDescriptor,
    original: PropertyDescriptor?,
    annotations: Annotations,
    modality: Modality,
    visibility: Visibility,
    isVar: Boolean,
    name: Name,
    kind: CallableMemberDescriptor.Kind,
    isLateInit: Boolean,
    isConst: Boolean,
    isExternal: Boolean,
    isDelegated: Boolean,
    isExpect: Boolean,
    override val proto: ProtoBuf.Property,
    override val nameResolver: NameResolver,
    override val typeTable: TypeTable,
    override val versionRequirementTable: VersionRequirementTable,
    override val containerSource: DeserializedContainerSource?
) : DeserializedCallableMemberDescriptor, PropertyDescriptorImpl(
    containingDeclaration, original, annotations, modality, visibility, isVar, name, kind, SourceElement.NO_SOURCE,
    isLateInit, isConst, isExpect, false, isExternal, isDelegated
) {
    override var coroutinesExperimentalCompatibilityMode = DeserializedMemberDescriptor.CoroutinesCompatibilityMode.COMPATIBLE
        private set

    fun initialize(
        getter: PropertyGetterDescriptorImpl?,
        setter: PropertySetterDescriptor?,
        isExperimentalCoroutineInReleaseEnvironment: DeserializedMemberDescriptor.CoroutinesCompatibilityMode
    ) {
        super.initialize(getter, setter)
            .also { this.coroutinesExperimentalCompatibilityMode = isExperimentalCoroutineInReleaseEnvironment }
    }

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        newModality: Modality,
        newVisibility: Visibility,
        original: PropertyDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
            newOwner, original, annotations, newModality, newVisibility, isVar, newName, kind, isLateInit, isConst, isExternal,
            @Suppress("DEPRECATION") isDelegated, isExpect, proto, nameResolver, typeTable, versionRequirementTable, containerSource
        )
    }

    override fun isExternal() = Flags.IS_EXTERNAL_PROPERTY.get(proto.flags)
}

class DeserializedClassConstructorDescriptor(
    containingDeclaration: ClassDescriptor,
    original: ConstructorDescriptor?,
    annotations: Annotations,
    isPrimary: Boolean,
    kind: CallableMemberDescriptor.Kind,
    override val proto: ProtoBuf.Constructor,
    override val nameResolver: NameResolver,
    override val typeTable: TypeTable,
    override val versionRequirementTable: VersionRequirementTable,
    override val containerSource: DeserializedContainerSource?,
    source: SourceElement? = null
) : DeserializedCallableMemberDescriptor,
    ClassConstructorDescriptorImpl(containingDeclaration, original, annotations, isPrimary, kind, source ?: SourceElement.NO_SOURCE) {

    override var coroutinesExperimentalCompatibilityMode = DeserializedMemberDescriptor.CoroutinesCompatibilityMode.COMPATIBLE
        internal set

    override fun createSubstitutedCopy(
        newOwner: DeclarationDescriptor,
        original: FunctionDescriptor?,
        kind: CallableMemberDescriptor.Kind,
        newName: Name?,
        annotations: Annotations,
        source: SourceElement
    ): DeserializedClassConstructorDescriptor {
        return DeserializedClassConstructorDescriptor(
            newOwner as ClassDescriptor, original as ConstructorDescriptor?, annotations, isPrimary, kind,
            proto, nameResolver, typeTable, versionRequirementTable, containerSource, source
        ).also { it.coroutinesExperimentalCompatibilityMode = coroutinesExperimentalCompatibilityMode }
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false

    override fun isSuspend(): Boolean = false
}

class DeserializedTypeAliasDescriptor(
    override val storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    visibility: Visibility,
    override val proto: ProtoBuf.TypeAlias,
    override val nameResolver: NameResolver,
    override val typeTable: TypeTable,
    override val versionRequirementTable: VersionRequirementTable,
    override val containerSource: DeserializedContainerSource?
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, SourceElement.NO_SOURCE, visibility),
    DeserializedMemberDescriptor {
    override lateinit var constructors: Collection<TypeAliasConstructorDescriptor> private set

    override lateinit var underlyingType: SimpleType private set
    override lateinit var expandedType: SimpleType private set
    private lateinit var typeConstructorParameters: List<TypeParameterDescriptor>
    private lateinit var defaultTypeImpl: SimpleType

    override var coroutinesExperimentalCompatibilityMode = DeserializedMemberDescriptor.CoroutinesCompatibilityMode.COMPATIBLE
        private set

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        underlyingType: SimpleType,
        expandedType: SimpleType,
        isExperimentalCoroutineInReleaseEnvironment: DeserializedMemberDescriptor.CoroutinesCompatibilityMode
    ) {
        initialize(declaredTypeParameters)
        this.underlyingType = underlyingType
        this.expandedType = expandedType
        typeConstructorParameters = computeConstructorTypeParameters()
        defaultTypeImpl = computeDefaultType()
        constructors = getTypeAliasConstructors()
        this.coroutinesExperimentalCompatibilityMode = isExperimentalCoroutineInReleaseEnvironment
    }

    override val classDescriptor: ClassDescriptor?
        get() = if (expandedType.isError) null else expandedType.constructor.declarationDescriptor as? ClassDescriptor

    override fun getDefaultType(): SimpleType = defaultTypeImpl

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasDescriptor {
        if (substitutor.isEmpty) return this
        val substituted = DeserializedTypeAliasDescriptor(
            storageManager, containingDeclaration, annotations, name, visibility,
            proto, nameResolver, typeTable, versionRequirementTable, containerSource
        )
        substituted.initialize(
            declaredTypeParameters,
            substitutor.safeSubstitute(underlyingType, Variance.INVARIANT).asSimpleType(),
            substitutor.safeSubstitute(expandedType, Variance.INVARIANT).asSimpleType(),
            coroutinesExperimentalCompatibilityMode
        )

        return substituted
    }

    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> = typeConstructorParameters
}
