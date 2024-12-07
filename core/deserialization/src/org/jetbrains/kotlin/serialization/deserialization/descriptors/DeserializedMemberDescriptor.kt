/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*

interface DescriptorWithContainerSource : MemberDescriptor {
    val containerSource: DeserializedContainerSource?
}

interface DeserializedMemberDescriptor : DeserializedDescriptor, MemberDescriptor, DescriptorWithContainerSource {
    val proto: MessageLite

    val nameResolver: NameResolver

    val typeTable: TypeTable

    val versionRequirementTable: VersionRequirementTable

    val versionRequirements: List<VersionRequirement>
        get() = VersionRequirement.create(proto, nameResolver, versionRequirementTable)

    // Information about the origin of this callable's container (class or package part on JVM) or null if there's no such information.
    // TODO: merge with sourceElement of containingDeclaration
    override val containerSource: DeserializedContainerSource?
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
            it.setHasStableParameterNames(hasStableParameterNames())
        }
    }
}

class DeserializedPropertyDescriptor(
        containingDeclaration: DeclarationDescriptor,
        original: PropertyDescriptor?,
        annotations: Annotations,
        modality: Modality,
        visibility: DescriptorVisibility,
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
    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            newModality: Modality,
            newVisibility: DescriptorVisibility,
            original: PropertyDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name,
            source: SourceElement
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
            newOwner, original, annotations, newModality, newVisibility, isVar, newName, kind, isLateInit, isConst, isExternal,
            isDelegated, isExpect, proto, nameResolver, typeTable, versionRequirementTable, containerSource
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
        ).also {
            it.setHasStableParameterNames(hasStableParameterNames())
        }
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false

    override fun isSuspend(): Boolean = false
}

class DeserializedTypeAliasDescriptor(
    storageManager: StorageManager,
    containingDeclaration: DeclarationDescriptor,
    annotations: Annotations,
    name: Name,
    visibility: DescriptorVisibility,
    override val proto: ProtoBuf.TypeAlias,
    override val nameResolver: NameResolver,
    override val typeTable: TypeTable,
    override val versionRequirementTable: VersionRequirementTable,
    override val containerSource: DeserializedContainerSource?
) : AbstractTypeAliasDescriptor(storageManager, containingDeclaration, annotations, name, SourceElement.NO_SOURCE, visibility),
    DeserializedMemberDescriptor {

    override lateinit var underlyingType: SimpleType private set
    override lateinit var expandedType: SimpleType private set
    private lateinit var typeConstructorParameters: List<TypeParameterDescriptor>
    private lateinit var defaultTypeImpl: SimpleType

    fun initialize(
        declaredTypeParameters: List<TypeParameterDescriptor>,
        underlyingType: SimpleType,
        expandedType: SimpleType
    ) {
        initialize(declaredTypeParameters)
        this.underlyingType = underlyingType
        this.expandedType = expandedType
        typeConstructorParameters = computeConstructorTypeParameters()
        defaultTypeImpl = computeDefaultType()
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
            substitutor.safeSubstitute(expandedType, Variance.INVARIANT).asSimpleType()
        )

        return substituted
    }

    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> = typeConstructorParameters
}
