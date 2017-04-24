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

package org.jetbrains.kotlin.serialization.deserialization.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.protobuf.MessageLite
import org.jetbrains.kotlin.serialization.Flags
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.*

interface DeserializedMemberDescriptor : MemberDescriptor {
    val proto: MessageLite

    val nameResolver: NameResolver

    val typeTable: TypeTable

    val sinceKotlinInfoTable: SinceKotlinInfoTable

    val sinceKotlinInfo: SinceKotlinInfo?
        get() = SinceKotlinInfo.create(proto, nameResolver, sinceKotlinInfoTable)

    // Information about the origin of this callable's container (class or package part on JVM) or null if there's no such information.
    // TODO: merge with sourceElement of containingDeclaration
    val containerSource: DeserializedContainerSource?
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
        override val sinceKotlinInfoTable: SinceKotlinInfoTable,
        override val containerSource: DeserializedContainerSource?,
        source: SourceElement? = null
) : DeserializedCallableMemberDescriptor,
    SimpleFunctionDescriptorImpl(
                containingDeclaration, original, annotations, name, kind,
                source ?: SourceElement.NO_SOURCE) {

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
                proto, nameResolver, typeTable, sinceKotlinInfoTable, containerSource, source
        )
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
        isHeader: Boolean,
        override val proto: ProtoBuf.Property,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val sinceKotlinInfoTable: SinceKotlinInfoTable,
        override val containerSource: DeserializedContainerSource?
) : DeserializedCallableMemberDescriptor, PropertyDescriptorImpl(
        containingDeclaration, original, annotations, modality, visibility, isVar, name, kind, SourceElement.NO_SOURCE,
        isLateInit, isConst, isHeader, false, isExternal, isDelegated
) {
    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            newModality: Modality,
            newVisibility: Visibility,
            original: PropertyDescriptor?,
            kind: CallableMemberDescriptor.Kind
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
                newOwner, original, annotations, newModality, newVisibility, isVar, name, kind, isLateInit, isConst, isExternal,
                @Suppress("DEPRECATION") isDelegated, isHeader, proto, nameResolver, typeTable, sinceKotlinInfoTable, containerSource
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
        override val sinceKotlinInfoTable: SinceKotlinInfoTable,
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
                proto, nameResolver, typeTable, sinceKotlinInfoTable, containerSource, source
        )
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false

    override fun isSuspend(): Boolean  = false
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
        override val sinceKotlinInfoTable: SinceKotlinInfoTable,
        override val containerSource: DeserializedContainerSource?
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, SourceElement.NO_SOURCE, visibility),
        DeserializedMemberDescriptor {
    override lateinit var constructors: Collection<TypeAliasConstructorDescriptor> private set

    override lateinit var underlyingType: SimpleType private set
    override lateinit var expandedType: SimpleType private set
    private lateinit var typeConstructorParameters: List<TypeParameterDescriptor>
    private lateinit var defaultTypeImpl: SimpleType private set

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
        constructors = getTypeAliasConstructors()
    }

    override val classDescriptor: ClassDescriptor?
        get() = if (expandedType.isError) null else expandedType.constructor.declarationDescriptor as? ClassDescriptor

    override fun getDefaultType(): SimpleType =
            defaultTypeImpl

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasDescriptor {
        if (substitutor.isEmpty) return this
        val substituted = DeserializedTypeAliasDescriptor(
                storageManager,
                containingDeclaration,
                annotations,
                name,
                visibility,
                proto,
                nameResolver,
                typeTable,
                sinceKotlinInfoTable,
                containerSource
        )
        substituted.initialize(declaredTypeParameters,
                               substitutor.safeSubstitute(underlyingType, Variance.INVARIANT).asSimpleType(),
                               substitutor.safeSubstitute(expandedType, Variance.INVARIANT).asSimpleType())

        return substituted
    }

    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> =
            typeConstructorParameters

}
