/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.types.*

interface TypeAliasConstructorDescriptor : ConstructorDescriptor, DescriptorDerivedFromTypeAlias {
    val underlyingConstructorDescriptor: ClassConstructorDescriptor

    override fun getContainingDeclaration(): TypeAliasDescriptor

    override fun getReturnType(): KotlinType

    override fun getOriginal(): TypeAliasConstructorDescriptor

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasConstructorDescriptor?

    val withDispatchReceiver: TypeAliasConstructorDescriptor?

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: Visibility,
        kind: Kind,
        copyOverrides: Boolean
    ): TypeAliasConstructorDescriptor
}

class TypeAliasConstructorDescriptorImpl private constructor(
    val storageManager: StorageManager,
    override val typeAliasDescriptor: TypeAliasDescriptor,
    underlyingConstructorDescriptor: ClassConstructorDescriptor,
    original: TypeAliasConstructorDescriptor?,
    annotations: Annotations,
    kind: Kind,
    source: SourceElement
) : TypeAliasConstructorDescriptor,
    FunctionDescriptorImpl(typeAliasDescriptor, original, annotations, Name.special("<init>"), kind, source) {

    init {
        isActual = typeAliasDescriptor.isActual
    }

    // When resolution is ran for common calls, type aliases constructors are resolved as extensions
    // (i.e. after members, and with extension receiver)
    // But when resolving super-calls (with known set of candidates) constructors of inner classes are expected to have
    // a dispatch receiver
    override val withDispatchReceiver: TypeAliasConstructorDescriptor? by storageManager.createNullableLazyValue {
        TypeAliasConstructorDescriptorImpl(
            storageManager,
            typeAliasDescriptor,
            underlyingConstructorDescriptor,
            this,
            underlyingConstructorDescriptor.annotations,
            underlyingConstructorDescriptor.kind,
            typeAliasDescriptor.source
        ).also { typeAliasConstructor ->
            val substitutorForUnderlyingClass =
                typeAliasDescriptor.getTypeSubstitutorForUnderlyingClass() ?: return@createNullableLazyValue null

            typeAliasConstructor.initialize(
                null,
                underlyingConstructorDescriptor.dispatchReceiverParameter?.substitute(substitutorForUnderlyingClass),
                typeAliasDescriptor.declaredTypeParameters,
                valueParameters,
                returnType,
                Modality.FINAL,
                typeAliasDescriptor.visibility
            )
        }
    }

    override var underlyingConstructorDescriptor: ClassConstructorDescriptor = underlyingConstructorDescriptor
        private set

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

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasConstructorDescriptor? {
        //    class C<T>(val x: T)
        //    typealias A<Q> = C<List<Q>>
        //
        //    val test = A(listOf(42))
        //
        // Here return type for substituted type alias constructor is 'C<List<Int>>',
        // which yields us a substitution [T -> List<Int>] that should be applied to
        // the unsubstituted underlying constructor with signature '<T> (T) -> C<T>'
        // producing substituted underlying constructor with signature '(List<Int>) -> C<List<Int>>'.
        val substitutedTypeAliasConstructor = super.substitute(substitutor) as TypeAliasConstructorDescriptorImpl
        val underlyingConstructorSubstitutor = TypeSubstitutor.create(substitutedTypeAliasConstructor.returnType)
        val substitutedUnderlyingConstructor = underlyingConstructorDescriptor.original.substitute(underlyingConstructorSubstitutor)
                ?: return null
        substitutedTypeAliasConstructor.underlyingConstructorDescriptor = substitutedUnderlyingConstructor
        return substitutedTypeAliasConstructor
    }

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
            storageManager,
            typeAliasDescriptor,
            underlyingConstructorDescriptor,
            this,
            annotations,
            Kind.DECLARATION, source
        )
    }

    companion object {
        private fun TypeAliasDescriptor.getTypeSubstitutorForUnderlyingClass(): TypeSubstitutor? {
            if (classDescriptor == null) return null
            return TypeSubstitutor.create(expandedType)
        }

        fun createIfAvailable(
            storageManager: StorageManager,
            typeAliasDescriptor: TypeAliasDescriptor,
            constructor: ClassConstructorDescriptor
        ): TypeAliasConstructorDescriptor? {
            val substitutorForUnderlyingClass = typeAliasDescriptor.getTypeSubstitutorForUnderlyingClass() ?: return null
            val substitutedConstructor = constructor.substitute(substitutorForUnderlyingClass) ?: return null

            val typeAliasConstructor =
                TypeAliasConstructorDescriptorImpl(
                    storageManager, typeAliasDescriptor,
                    substitutedConstructor,
                    null, constructor.annotations,
                    constructor.kind, typeAliasDescriptor.source
                )

            val valueParameters =
                FunctionDescriptorImpl.getSubstitutedValueParameters(
                    typeAliasConstructor, constructor.valueParameters, substitutorForUnderlyingClass
                ) ?: return null

            val returnType = substitutedConstructor.returnType.unwrap().lowerIfFlexible().withAbbreviation(typeAliasDescriptor.defaultType)

            val receiverParameter = constructor.dispatchReceiverParameter?.let {
                DescriptorFactory.createExtensionReceiverParameterForCallable(
                    typeAliasConstructor,
                    substitutorForUnderlyingClass.safeSubstitute(it.type, Variance.INVARIANT),
                    Annotations.EMPTY
                )
            }

            typeAliasConstructor.initialize(
                receiverParameter,
                null,
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



