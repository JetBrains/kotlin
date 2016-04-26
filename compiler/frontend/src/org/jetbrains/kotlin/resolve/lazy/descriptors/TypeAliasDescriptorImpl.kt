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

package org.jetbrains.kotlin.resolve.lazy.descriptors

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorNonRootImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.storage.NotNullLazyValue
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSubstitutor

class TypeAliasDescriptorImpl(
        containingDeclaration: DeclarationDescriptor,
        annotations: Annotations,
        name: Name,
        sourceElement: SourceElement,
        private val fVisibility: Visibility
) : DeclarationDescriptorNonRootImpl(containingDeclaration, annotations, name, sourceElement),
        TypeAliasDescriptor {

    // TODO kotlinize some interfaces
    private lateinit var fDeclaredTypeParameters: List<TypeParameterDescriptor>
    private lateinit var lazyUnderlyingType: NotNullLazyValue<KotlinType>

    override val underlyingType: KotlinType get() = lazyUnderlyingType()

    fun initialize(declaredTypeParameters: List<TypeParameterDescriptor>, lazyUnderlyingType: NotNullLazyValue<KotlinType>) {
        this.fDeclaredTypeParameters = declaredTypeParameters
        this.lazyUnderlyingType = lazyUnderlyingType
    }

    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
            visitor.visitTypeAliasDescriptor(this, data)

    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> =
            fDeclaredTypeParameters

    override val underlyingClassDescriptor: ClassDescriptor
        get() = underlyingType.constructor.declarationDescriptor as ClassDescriptor  // TODO

    override fun getModality() = Modality.FINAL

    override fun getVisibility() = fVisibility

    override fun substitute(substitutor: TypeSubstitutor): TypeAliasDescriptor =
            if (substitutor.isEmpty) this
            else TODO("typealias doSubstitute")

    override fun getDefaultType(): KotlinType =
            TODO("typealias getDefaultType")

    override fun getTypeConstructor(): TypeConstructor =
            typeConstructor

    override fun toString(): String = "typealias ${name.asString()}"

    private val typeConstructor = object : TypeConstructor {
        override fun getDeclarationDescriptor(): TypeAliasDescriptor =
                this@TypeAliasDescriptorImpl

        override fun getParameters(): List<TypeParameterDescriptor> =
                declarationDescriptor.declaredTypeParameters // TODO type parameters of outer class

        override fun getSupertypes(): Collection<KotlinType> =
                declarationDescriptor.underlyingType.constructor.supertypes

        override fun isFinal(): Boolean =
                declarationDescriptor.underlyingType.constructor.isFinal

        override fun isDenotable(): Boolean =
                true

        override fun getBuiltIns(): KotlinBuiltIns =
                declarationDescriptor.builtIns

        override fun getAnnotations(): Annotations =
                declarationDescriptor.annotations
    }

    companion object {
        @JvmStatic fun create(
                containingDeclaration: DeclarationDescriptor,
                annotations: Annotations,
                name: Name,
                sourceElement: SourceElement,
                visibility: Visibility
        ): TypeAliasDescriptorImpl =
                TypeAliasDescriptorImpl(containingDeclaration, annotations, name, sourceElement, visibility)
    }
}
