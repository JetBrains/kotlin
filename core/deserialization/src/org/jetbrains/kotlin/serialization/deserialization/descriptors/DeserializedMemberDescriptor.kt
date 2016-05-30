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

import com.google.protobuf.MessageLite
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.TypeTable
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.Variance

interface DeserializedMemberDescriptor : MemberDescriptor {
    val proto: MessageLite

    val nameResolver: NameResolver

    val typeTable: TypeTable

    // Information about the origin of this callable's container (class or package part on JVM) or null if there's no such information.
    val containerSource: SourceElement?
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
        override val containerSource: SourceElement?,
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
                proto, nameResolver, typeTable, containerSource, source
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
        override val proto: ProtoBuf.Property,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val containerSource: SourceElement?
) : DeserializedCallableMemberDescriptor,
        PropertyDescriptorImpl(containingDeclaration, original, annotations,
                               modality, visibility, isVar, name, kind, SourceElement.NO_SOURCE, isLateInit, isConst) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            newModality: Modality,
            newVisibility: Visibility,
            original: PropertyDescriptor?,
            kind: CallableMemberDescriptor.Kind
    ): PropertyDescriptorImpl {
        return DeserializedPropertyDescriptor(
                newOwner, original, annotations, newModality, newVisibility, isVar, name, kind, isLateInit, isConst,
                proto, nameResolver, typeTable, containerSource
        )
    }
}

class DeserializedConstructorDescriptor(
        containingDeclaration: ClassDescriptor,
        original: ConstructorDescriptor?,
        annotations: Annotations,
        isPrimary: Boolean,
        kind: CallableMemberDescriptor.Kind,
        override val proto: ProtoBuf.Constructor,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val containerSource: SourceElement?,
        source: SourceElement? = null
) : DeserializedCallableMemberDescriptor,
        ConstructorDescriptorImpl(containingDeclaration, original, annotations, isPrimary, kind, source ?: SourceElement.NO_SOURCE) {

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: CallableMemberDescriptor.Kind,
            newName: Name?,
            annotations: Annotations,
            source: SourceElement
    ): DeserializedConstructorDescriptor {
        return DeserializedConstructorDescriptor(
                newOwner as ClassDescriptor, original as ConstructorDescriptor?, annotations, isPrimary, kind,
                proto, nameResolver, typeTable, containerSource, source
        )
    }

    override fun isExternal(): Boolean = false

    override fun isInline(): Boolean = false

    override fun isTailrec(): Boolean = false

    override fun isSuspend(): Boolean  = false
}

class DeserializedTypeAliasDescriptor(
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,
        name: Name,
        visibility: Visibility,
        override val proto: ProtoBuf.TypeAlias,
        override val nameResolver: NameResolver,
        override val typeTable: TypeTable,
        override val containerSource: SourceElement?
) : AbstractTypeAliasDescriptor(containingDeclaration, annotations, name, SourceElement.NO_SOURCE, visibility),
        DeserializedMemberDescriptor {

    override lateinit var underlyingType: KotlinType private set
    override lateinit var expandedType: KotlinType private set
    private lateinit var typeConstructorParameters: List<TypeParameterDescriptor>

    fun initialize(
            declaredTypeParameters: List<TypeParameterDescriptor>,
            underlyingType: KotlinType,
            expandedType: KotlinType
    ) {
        initialize(declaredTypeParameters)
        this.underlyingType = underlyingType
        this.expandedType = expandedType
        typeConstructorParameters = computeConstructorTypeParameters()
    }

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasDescriptor {
        if (substitutor.isEmpty) return this
        val substituted = DeserializedTypeAliasDescriptor(containingDeclaration, annotations, name, visibility, proto, nameResolver, typeTable, containerSource)
        substituted.initialize(declaredTypeParameters,
                               substitutor.safeSubstitute(underlyingType, Variance.INVARIANT),
                               substitutor.safeSubstitute(expandedType, Variance.INVARIANT))

        return substituted
    }

    override fun getTypeConstructorTypeParameters(): List<TypeParameterDescriptor> =
            typeConstructorParameters

}
