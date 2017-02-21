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

package org.jetbrains.kotlin.backend.jvm.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.LazyClassReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*

open class KnownPackageFragmentDescriptor(moduleDescriptor: ModuleDescriptor, fqName: FqName) :
        PackageFragmentDescriptorImpl(moduleDescriptor, fqName) {
    override fun getMemberScope(): MemberScope = MemberScope.Empty
}

open class KnownClassDescriptor(
        private val name: Name,
        private val containingDeclaration: DeclarationDescriptor,
        private val sourceElement: SourceElement,
        private val kind: ClassKind,
        private val modality: Modality,
        private val visibility: Visibility,
        override val annotations: Annotations
) : ClassDescriptor {
    init {
        assert(modality != Modality.SEALED) { "Implement getSealedSubclasses() for this class: ${this::class.java}" }
    }

    private lateinit var typeConstructor: TypeConstructor
    private lateinit var supertypes: List<KotlinType>
    private lateinit var defaultType: SimpleType
    private lateinit var declaredTypeParameters: List<TypeParameterDescriptor>

    private val thisAsReceiverParameter = LazyClassReceiverParameterDescriptor(this)

    fun initialize(declaredTypeParameters: List<TypeParameterDescriptor>, supertypes: List<KotlinType>) {
        this.declaredTypeParameters = declaredTypeParameters
        this.supertypes = supertypes
        this.typeConstructor = ClassTypeConstructorImpl(this, true, declaredTypeParameters, supertypes)
        this.defaultType = TypeUtils.makeUnsubstitutedType(this, unsubstitutedMemberScope)
    }

    companion object {
        fun createClass(
                name: Name,
                containingDeclaration: DeclarationDescriptor,
                supertypes: List<KotlinType>,
                modality: Modality = Modality.FINAL,
                visibility: Visibility = Visibilities.PUBLIC,
                annotations: Annotations = Annotations.EMPTY
        ) =
                KnownClassDescriptor(
                        name, containingDeclaration,
                        SourceElement.NO_SOURCE, ClassKind.CLASS,
                        modality, visibility,
                        annotations
                ).apply {
                    initialize(emptyList(), supertypes)
                }

        inline fun createClassWithTypeParameters(
                name: Name,
                containingDeclaration: DeclarationDescriptor,
                supertypes: List<KotlinType>,
                modality: Modality = Modality.FINAL,
                visibility: Visibility = Visibilities.PUBLIC,
                annotations: Annotations = Annotations.EMPTY,
                createTypeParameters: (ClassDescriptor) -> List<TypeParameterDescriptor>
        ) =
                KnownClassDescriptor(
                        name, containingDeclaration,
                        SourceElement.NO_SOURCE, ClassKind.CLASS,
                        modality, visibility,
                        annotations
                ).apply {
                    initialize(createTypeParameters(this), supertypes)
                }

        fun createClassWithTypeParameters(
                name: Name,
                containingDeclaration: DeclarationDescriptor,
                supertypes: List<KotlinType>,
                typeParameterNames: List<Name>,
                modality: Modality = Modality.FINAL,
                visibility: Visibility = Visibilities.PUBLIC,
                annotations: Annotations = Annotations.EMPTY
        ) =
                createClassWithTypeParameters(name, containingDeclaration, supertypes, modality, visibility, annotations) { classDescriptor ->
                    typeParameterNames.mapIndexed { index, name ->
                        TypeParameterDescriptorImpl.createWithDefaultBound(
                                classDescriptor, Annotations.EMPTY, true, Variance.INVARIANT, name, index
                        )
                    }
                }
    }

    override fun getCompanionObjectDescriptor(): ClassDescriptor? = null
    override fun getConstructors(): Collection<ClassConstructorDescriptor> = emptyList()
    override fun getContainingDeclaration(): DeclarationDescriptor = containingDeclaration
    override fun getDeclaredTypeParameters(): List<TypeParameterDescriptor> = declaredTypeParameters
    override fun getKind(): ClassKind = kind
    override fun getSealedSubclasses(): Collection<ClassDescriptor> = emptyList()

    override fun getMemberScope(typeArguments: MutableList<out TypeProjection>): MemberScope = MemberScope.Empty
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope = MemberScope.Empty
    override fun getStaticScope(): MemberScope = MemberScope.Empty
    override fun getUnsubstitutedInnerClassesScope(): MemberScope = MemberScope.Empty
    override fun getUnsubstitutedMemberScope(): MemberScope = MemberScope.Empty

    override fun getUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor? = null

    override fun substitute(substitutor: TypeSubstitutor): ClassDescriptor = error("Class $this can't be substituted")

    override fun getThisAsReceiverParameter(): ReceiverParameterDescriptor = thisAsReceiverParameter

    override fun getModality(): Modality = modality
    override fun getOriginal(): ClassDescriptor = this
    override fun getName(): Name = name
    override fun getVisibility(): Visibility = visibility
    override fun getSource(): SourceElement = sourceElement
    override fun getTypeConstructor(): TypeConstructor = typeConstructor
    override fun getDefaultType(): SimpleType = defaultType

    override fun isCompanionObject(): Boolean = false
    override fun isData(): Boolean = false
    override fun isInner(): Boolean = false
    override fun isHeader(): Boolean = false
    override fun isImpl(): Boolean = false
    override fun isExternal(): Boolean = false

    override fun <R : Any?, D : Any?> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R {
        return visitor.visitClassDescriptor(this, data)
    }

    override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>) {
        visitor.visitClassDescriptor(this, null)
    }

    override fun toString(): String =
            "KnownClassDescriptor($fqNameUnsafe)"
}
