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

package org.jetbrains.kotlin.psi.synthetics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorBase
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.ClassResolutionScopesSupport
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor

/*
 * This class introduces all attributes that are needed for synthetic classes/object so far.
 * This list may grow in the future, adding more constructor parameters.
 * This class has its own synthetic declaration inside.
 */
class SyntheticClassOrObjectDescriptor(
        c: LazyClassContext,
        parentClassOrObject: KtPureClassOrObject,
        containingDeclaration: DeclarationDescriptor,
        name: Name,
        source: SourceElement,
        outerScope: LexicalScope,
        private val modality: Modality,
        private val visibility: Visibility,
        private val kind: ClassKind,
        private val isCompanionObject: Boolean
) : ClassDescriptorBase(c.storageManager, containingDeclaration, name, source, false), ClassDescriptorWithResolutionScopes {
    val syntheticDeclaration: KtPureClassOrObject = SyntheticDeclaration(parentClassOrObject, name.asString())

    private val thisDescriptor: SyntheticClassOrObjectDescriptor get() = this // code readability
    private val typeConstructor = SyntheticTypeConstructor(c.storageManager)
    private val resolutionScopesSupport = ClassResolutionScopesSupport(thisDescriptor, c.storageManager, { outerScope })
    private val syntheticSupertypes = mutableListOf<KotlinType>().apply { c.syntheticResolveExtension.addSyntheticSupertypes(thisDescriptor, this) }
    private val unsubstitutedMemberScope = LazyClassMemberScope(c, SyntheticClassMemberDeclarationProvider(syntheticDeclaration), this, c.trace)
    private val unsubstitutedPrimaryConstructor = createUnsubstitutedPrimaryConstructor()

    override val annotations: Annotations get() = Annotations.EMPTY

    override fun getModality() = modality
    override fun getVisibility() = visibility
    override fun getKind() = kind
    override fun isCompanionObject() = isCompanionObject
    override fun isInner() = false
    override fun isData() = false
    override fun isHeader() = false
    override fun isImpl() = false

    override fun getCompanionObjectDescriptor() = null
    override fun getTypeConstructor(): TypeConstructor = typeConstructor
    override fun getUnsubstitutedPrimaryConstructor() = unsubstitutedPrimaryConstructor
    override fun getConstructors() = listOf(unsubstitutedPrimaryConstructor)
    override fun getDeclaredTypeParameters() = emptyList<TypeParameterDescriptor>()
    override fun getStaticScope() = MemberScope.Empty
    override fun getUnsubstitutedMemberScope() = unsubstitutedMemberScope
    override fun getSealedSubclasses() = emptyList<ClassDescriptor>()

    init {
        assert(modality != Modality.SEALED) { "Implement getSealedSubclasses() for this class: ${this::class.java}" }
    }

    override fun getDeclaredCallableMembers(): List<CallableMemberDescriptor> =
        DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope).filterIsInstance<CallableMemberDescriptor>().filter {
            it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
        }

    override fun getScopeForClassHeaderResolution(): LexicalScope = resolutionScopesSupport.scopeForClassHeaderResolution()
    override fun getScopeForConstructorHeaderResolution(): LexicalScope = resolutionScopesSupport.scopeForConstructorHeaderResolution()
    override fun getScopeForCompanionObjectHeaderResolution(): LexicalScope = resolutionScopesSupport.scopeForCompanionObjectHeaderResolution()
    override fun getScopeForMemberDeclarationResolution(): LexicalScope = resolutionScopesSupport.scopeForMemberDeclarationResolution()
    override fun getScopeForStaticMemberDeclarationResolution(): LexicalScope = resolutionScopesSupport.scopeForStaticMemberDeclarationResolution()

    override fun getScopeForInitializerResolution(): LexicalScope = throw UnsupportedOperationException("Not supported for synthetic class or object")

    override fun toString(): String = "synthetic class " + name.toString() + " in " + containingDeclaration

    private fun createUnsubstitutedPrimaryConstructor(): ClassConstructorDescriptor {
        val constructor = DescriptorFactory.createPrimaryConstructorForObject(thisDescriptor, source)
        constructor.returnType = getDefaultType()
        return constructor
    }

    private inner class SyntheticTypeConstructor(storageManager: StorageManager) : AbstractClassTypeConstructor(storageManager) {
        override fun getParameters(): List<TypeParameterDescriptor> = emptyList()
        override fun isFinal(): Boolean = true
        override fun isDenotable(): Boolean = true
        override fun getDeclarationDescriptor(): ClassifierDescriptor = thisDescriptor
        override fun computeSupertypes(): Collection<KotlinType> = syntheticSupertypes
        override val supertypeLoopChecker: SupertypeLoopChecker = SupertypeLoopChecker.EMPTY
    }

    private class SyntheticClassMemberDeclarationProvider(
            override val correspondingClassOrObject: KtPureClassOrObject
    ) : ClassMemberDeclarationProvider {
        override val ownerInfo: KtClassLikeInfo? = null
        override fun getDeclarations(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean): List<KtDeclaration> = emptyList()
        override fun getFunctionDeclarations(name: Name): Collection<KtNamedFunction> = emptyList()
        override fun getPropertyDeclarations(name: Name): Collection<KtProperty> = emptyList()
        override fun getClassOrObjectDeclarations(name: Name): Collection<KtClassLikeInfo> = emptyList()
        override fun getTypeAliasDeclarations(name: Name): Collection<KtTypeAlias> = emptyList()
    }

    internal inner class SyntheticDeclaration(
            private val _parent: KtPureElement,
            private val _name: String
    ) : KtPureClassOrObject {
        fun descriptor() = thisDescriptor

        override fun getName(): String? = _name
        override fun isLocal(): Boolean = false

        override fun getDeclarations(): List<KtDeclaration> = emptyList()
        override fun getSuperTypeListEntries(): List<KtSuperTypeListEntry> = emptyList()
        override fun getCompanionObjects(): List<KtObjectDeclaration> = emptyList()

        override fun hasExplicitPrimaryConstructor(): Boolean = false
        override fun hasPrimaryConstructor(): Boolean = false
        override fun getPrimaryConstructor(): KtPrimaryConstructor? = null
        override fun getPrimaryConstructorModifierList(): KtModifierList? = null
        override fun getPrimaryConstructorParameters(): List<KtParameter> = emptyList()
        override fun getSecondaryConstructors(): List<KtSecondaryConstructor> = emptyList()

        override fun getPsiOrParent() = _parent.psiOrParent
        override fun getParent() = _parent.psiOrParent
        override fun getContainingKtFile() = _parent.containingKtFile
    }
}

fun KtPureElement.findClassDescriptor(bindingContext: BindingContext): ClassDescriptor = when (this) {
    is PsiElement -> BindingContextUtils.getNotNull(bindingContext, BindingContext.CLASS, this)
    is SyntheticClassOrObjectDescriptor.SyntheticDeclaration -> descriptor()
    else -> throw IllegalArgumentException("$this shall be PsiElement or SyntheticClassOrObjectDescriptor.SyntheticDeclaration")
}
