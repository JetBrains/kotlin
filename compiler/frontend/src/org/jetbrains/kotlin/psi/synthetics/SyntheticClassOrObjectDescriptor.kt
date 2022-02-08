/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.resolve.lazy.data.KtClassOrObjectInfo
import org.jetbrains.kotlin.resolve.lazy.data.KtScriptInfo
import org.jetbrains.kotlin.resolve.lazy.declarations.ClassMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.lazy.descriptors.ClassResolutionScopesSupport
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.AbstractClassTypeConstructor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner

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
        private val visibility: DescriptorVisibility,
        override val annotations: Annotations,
        constructorVisibility: DescriptorVisibility,
        private val kind: ClassKind,
        private val isCompanionObject: Boolean
) : ClassDescriptorBase(c.storageManager, containingDeclaration, name, source, false), ClassDescriptorWithResolutionScopes {
    val syntheticDeclaration: KtPureClassOrObject = SyntheticDeclaration(parentClassOrObject, name.asString())

    private val thisDescriptor: SyntheticClassOrObjectDescriptor get() = this // code readability

    private lateinit var typeParameters: List<TypeParameterDescriptor>
    public var secondaryConstructors: List<ClassConstructorDescriptor> = emptyList()

    private val typeConstructor = SyntheticTypeConstructor(c.storageManager)
    private val resolutionScopesSupport = ClassResolutionScopesSupport(thisDescriptor, c.storageManager, c.languageVersionSettings, { outerScope })
    private val syntheticSupertypes =
        mutableListOf<KotlinType>().apply { c.syntheticResolveExtension.addSyntheticSupertypes(thisDescriptor, this) }
    private val unsubstitutedMemberScope =
        LazyClassMemberScope(c, SyntheticClassMemberDeclarationProvider(syntheticDeclaration), this, c.trace)
    private val _unsubstitutedPrimaryConstructor =
        c.storageManager.createLazyValue { createUnsubstitutedPrimaryConstructor(constructorVisibility) }

    @JvmOverloads
    fun initialize(
        typeParameters: List<TypeParameterDescriptor> = emptyList()
    ) {
        this.typeParameters = typeParameters
    }

    override fun getModality() = modality
    override fun getVisibility() = visibility
    override fun getKind() = kind
    override fun isCompanionObject() = isCompanionObject
    override fun isInner() = false
    override fun isData() = false
    override fun isInline() = false
    override fun isExpect() = false
    override fun isActual() = false
    override fun isFun() = false
    override fun isValue() = false

    override fun getCompanionObjectDescriptor(): ClassDescriptorWithResolutionScopes? = null
    override fun getTypeConstructor(): TypeConstructor = typeConstructor
    override fun getUnsubstitutedPrimaryConstructor() = _unsubstitutedPrimaryConstructor()
    override fun getConstructors() = listOf(_unsubstitutedPrimaryConstructor()) + secondaryConstructors
    override fun getDeclaredTypeParameters() = typeParameters
    override fun getStaticScope() = MemberScope.Empty
    override fun getUnsubstitutedMemberScope(kotlinTypeRefiner: KotlinTypeRefiner) = unsubstitutedMemberScope
    override fun getSealedSubclasses() = emptyList<ClassDescriptor>()
    override fun getInlineClassRepresentation(): InlineClassRepresentation<SimpleType>? = null

    init {
        assert(modality != Modality.SEALED) { "Implement getSealedSubclasses() for this class: ${this::class.java}" }
    }

    override fun getDeclaredCallableMembers(): List<CallableMemberDescriptor> =
        DescriptorUtils.getAllDescriptors(unsubstitutedMemberScope).filterIsInstance<CallableMemberDescriptor>().filter {
            it.kind != CallableMemberDescriptor.Kind.FAKE_OVERRIDE
        }

    override fun getScopeForClassHeaderResolution(): LexicalScope = resolutionScopesSupport.scopeForClassHeaderResolution()
    override fun getScopeForConstructorHeaderResolution(): LexicalScope = resolutionScopesSupport.scopeForConstructorHeaderResolution()
    override fun getScopeForCompanionObjectHeaderResolution(): LexicalScope =
        resolutionScopesSupport.scopeForCompanionObjectHeaderResolution()

    override fun getScopeForMemberDeclarationResolution(): LexicalScope = resolutionScopesSupport.scopeForMemberDeclarationResolution()
    override fun getScopeForStaticMemberDeclarationResolution(): LexicalScope =
        resolutionScopesSupport.scopeForStaticMemberDeclarationResolution()

    override fun getScopeForInitializerResolution(): LexicalScope =
        throw UnsupportedOperationException("Not supported for synthetic class or object")

    override fun toString(): String = "synthetic class " + name.toString() + " in " + containingDeclaration

    private fun createUnsubstitutedPrimaryConstructor(constructorVisibility: DescriptorVisibility): ClassConstructorDescriptor {
        val constructor = DescriptorFactory.createPrimaryConstructorForObject(thisDescriptor, source)
        constructor.visibility = constructorVisibility
        constructor.returnType = getDefaultType()
        return constructor
    }

    private inner class SyntheticTypeConstructor(storageManager: StorageManager) : AbstractClassTypeConstructor(storageManager) {
        override fun getParameters(): List<TypeParameterDescriptor> = typeParameters
        override fun isDenotable(): Boolean = true
        override fun getDeclarationDescriptor(): ClassDescriptor = thisDescriptor
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
        override fun getDestructuringDeclarationsEntries(name: Name): Collection<KtDestructuringDeclarationEntry> = emptyList()
        override fun getClassOrObjectDeclarations(name: Name): Collection<KtClassOrObjectInfo<*>> = emptyList()
        override fun getScriptDeclarations(name: Name): Collection<KtScriptInfo> = emptyList()
        override fun getTypeAliasDeclarations(name: Name): Collection<KtTypeAlias> = emptyList()
        override fun getDeclarationNames() = emptySet<Name>()
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
        override fun getContextReceivers(): List<KtContextReceiver> = emptyList()

        override fun getPsiOrParent() = _parent.psiOrParent
        override fun getParent() = _parent.psiOrParent
        @Suppress("USELESS_ELVIS")
        override fun getContainingKtFile() =
        // in theory `containingKtFile` is `@NotNull` but in practice EA-114080
            _parent.containingKtFile ?: throw IllegalStateException("containingKtFile was null for $_parent of ${_parent.javaClass}")

        override fun getBody(): KtClassBody? = null
    }
}

fun KtPureElement.findClassDescriptor(bindingContext: BindingContext): ClassDescriptor = when (this) {
    is PsiElement -> BindingContextUtils.getNotNull(bindingContext, BindingContext.CLASS, this)
    is SyntheticClassOrObjectDescriptor.SyntheticDeclaration -> descriptor()
    else -> throw IllegalArgumentException("$this shall be PsiElement or SyntheticClassOrObjectDescriptor.SyntheticDeclaration")
}
