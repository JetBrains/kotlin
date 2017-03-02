/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.descriptors.impl

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.*

interface TypeAliasConstructorDescriptor : ConstructorDescriptor {
    val underlyingConstructorDescriptor: ClassConstructorDescriptor
    val typeAliasDescriptor: TypeAliasDescriptor

    override fun getContainingDeclaration(): TypeAliasDescriptor

    override fun getReturnType(): KotlinType

    override fun getOriginal(): TypeAliasConstructorDescriptor

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasConstructorDescriptor

    override fun copy(
            newOwner: DeclarationDescriptor,
            modality: Modality,
            visibility: Visibility,
            kind: Kind,
            copyOverrides: Boolean
    ): TypeAliasConstructorDescriptor
}

class TypeAliasConstructorDescriptorImpl private constructor(
        override val typeAliasDescriptor: TypeAliasDescriptor,
        override val underlyingConstructorDescriptor: ClassConstructorDescriptor,
        original: TypeAliasConstructorDescriptor?,
        annotations: Annotations,
        kind: Kind,
        source: SourceElement
) : TypeAliasConstructorDescriptor,
        FunctionDescriptorImpl(typeAliasDescriptor, original, annotations, Name.special("<init>"), kind, source)
{
    override fun isPrimary(): Boolean =
            underlyingConstructorDescriptor.isPrimary

    override fun getContainingDeclaration(): TypeAliasDescriptor =
            typeAliasDescriptor

    override fun getConstructedClass(): ClassDescriptor =
            underlyingConstructorDescriptor.constructedClass

    override fun getReturnType(): KotlinType =
            super.getReturnType()!!

    override fun getOriginal(): TypeAliasConstructorDescriptor =
            super.getOriginal() as TypeAliasConstructorDescriptor

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasConstructorDescriptor =
            super.substitute(substitutor) as TypeAliasConstructorDescriptor

    override fun copy(
            newOwner: DeclarationDescriptor,
            modality: Modality,
            visibility: Visibility,
            kind: Kind,
            copyOverrides: Boolean
    ): TypeAliasConstructorDescriptor =
            newCopyBuilder()
                    .setOwner(newOwner)
                    .setModality(modality)
                    .setVisibility(visibility)
                    .setKind(kind)
                    .setCopyOverrides(copyOverrides)
                    .build() as TypeAliasConstructorDescriptor

    override fun createSubstitutedCopy(
            newOwner: DeclarationDescriptor,
            original: FunctionDescriptor?,
            kind: Kind,
            newName: Name?,
            annotations: Annotations,
            source: SourceElement
    ): TypeAliasConstructorDescriptorImpl {
        assert(kind == Kind.DECLARATION || kind == Kind.SYNTHESIZED) {
            "Creating a type alias constructor that is not a declaration: \ncopy from: ${this}\nnewOwner: $newOwner\nkind: $kind"
        }
        assert(newName == null) { "Renaming type alias constructor: $this" }
        return TypeAliasConstructorDescriptorImpl(
                typeAliasDescriptor, underlyingConstructorDescriptor,
                this,
                annotations,
                Kind.DECLARATION, source)
    }

    companion object {
        private fun TypeAliasDescriptor.getTypeSubstitutorForUnderlyingClass(): TypeSubstitutor? {
            if (classDescriptor == null) return null
            return TypeSubstitutor.create(expandedType)
        }

        fun createIfAvailable(
                typeAliasDescriptor: TypeAliasDescriptor,
                constructor: ClassConstructorDescriptor,
                // When resolution is ran for common calls, type aliases constructors are resolved as extensions
                // (i.e. after members, and with extension receiver)
                // But when resolving super-calls (with known set of candidates) constructors of inner classes are expected to have
                // an dispatch receiver
                withDispatchReceiver: Boolean
        ): TypeAliasConstructorDescriptor? {
            val substitutorForUnderlyingClass = typeAliasDescriptor.getTypeSubstitutorForUnderlyingClass() ?: return null

            val typeAliasConstructor =
                    TypeAliasConstructorDescriptorImpl(typeAliasDescriptor, constructor, null, constructor.annotations,
                                                       constructor.kind, typeAliasDescriptor.source)

            val valueParameters =
                    FunctionDescriptorImpl.getSubstitutedValueParameters(
                            typeAliasConstructor, constructor.valueParameters, substitutorForUnderlyingClass, false, false
                    )
                    ?: return null

            val returnType = run {
                val returnTypeNoAbbreviation = substitutorForUnderlyingClass.substitute(constructor.returnType, Variance.INVARIANT)
                                               ?: return null
                val abbreviation = typeAliasDescriptor.defaultType
                if (returnTypeNoAbbreviation is SimpleType && abbreviation is SimpleType)
                    returnTypeNoAbbreviation.withAbbreviation(abbreviation)
                else
                    returnTypeNoAbbreviation
            }

            val receiverParameterType =
                    if (withDispatchReceiver) null
                    else constructor.dispatchReceiverParameter?.let { substitutorForUnderlyingClass.safeSubstitute(it.type, Variance.INVARIANT) }

            val dispatchReceiver =
                    if (withDispatchReceiver) constructor.dispatchReceiverParameter?.substitute(substitutorForUnderlyingClass)
                    else null

            typeAliasConstructor.initialize(
                    receiverParameterType,
                    dispatchReceiver,
                    typeAliasDescriptor.declaredTypeParameters,
                    valueParameters,
                    returnType,
                    Modality.FINAL,
                    typeAliasDescriptor.visibility
            )

            return typeAliasConstructor
        }
    }
}



