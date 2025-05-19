/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.analysis.api.components.*
import org.jetbrains.kotlin.analysis.api.descriptors.KaFe10Session
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.KaFe10SessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.scopes.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10FileSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KaFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KaFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KaFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KaFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KaFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseScopeContext
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseScopeImplicitReceiverValue
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaBaseCompositeScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaBaseEmptyScope
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaTypeScope
import org.jetbrains.kotlin.analysis.api.symbols.KaFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaDeclarationContainerSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.util.containingNonLocalDeclaration
import org.jetbrains.kotlin.utils.Printer

internal class KaFe10ScopeProvider(
    override val analysisSessionProvider: () -> KaFe10Session
) : KaBaseSessionComponent<KaFe10Session>(), KaScopeProvider, KaFe10SessionComponent {
    private companion object {
        val LOG = Logger.getInstance(KaFe10ScopeProvider::class.java)
    }

    override val KaDeclarationContainerSymbol.memberScope: KaScope
        get() = withValidityAssertion {
            val descriptor = getDescriptor<ClassDescriptor>(this)
                ?: return createEmptyScope()

            return KaFe10ScopeMember(descriptor.unsubstitutedMemberScope, descriptor.constructors, analysisContext)
        }

    override val KaDeclarationContainerSymbol.staticMemberScope: KaScope
        get() = withValidityAssertion {
            val descriptor = getDescriptor<ClassDescriptor>(this) ?: return createEmptyScope()
            return KaFe10ScopeMember(descriptor.staticScope, emptyList(), analysisContext)
        }

    override val KaDeclarationContainerSymbol.declaredMemberScope: KaScope
        get() = withValidityAssertion {
            val descriptor = getDescriptor<ClassDescriptor>(this)
                ?: return createEmptyScope()

            return KaFe10ScopeNonStaticMember(DeclaredMemberScope(descriptor), descriptor.constructors, analysisContext)
        }

    override val KaDeclarationContainerSymbol.staticDeclaredMemberScope: KaScope
        get() = withValidityAssertion {
            val descriptor = getDescriptor<ClassDescriptor>(this)
                ?: return createEmptyScope()

            return KaFe10ScopeMember(
                DeclaredMemberScope(descriptor.staticScope, descriptor, forDelegatedMembersOnly = false),
                emptyList(),
                analysisContext,
            )
        }

    override val KaDeclarationContainerSymbol.combinedDeclaredMemberScope: KaScope
        get() = withValidityAssertion {
            val descriptor = getDescriptor<ClassDescriptor>(this)
                ?: return createEmptyScope()

            return KaFe10ScopeMember(DeclaredMemberScope(descriptor), descriptor.constructors, analysisContext)
        }

    override val KaDeclarationContainerSymbol.delegatedMemberScope: KaScope
        get() = withValidityAssertion {
            val descriptor = getDescriptor<ClassDescriptor>(this)
                ?: return createEmptyScope()

            return KaFe10ScopeMember(DeclaredMemberScope(descriptor, forDelegatedMembersOnly = true), emptyList(), analysisContext)
        }

    private fun createEmptyScope(): KaScope {
        return KaBaseEmptyScope(token)
    }

    private class DeclaredMemberScope(
        val allMemberScope: MemberScope,
        val owner: ClassDescriptor,
        val forDelegatedMembersOnly: Boolean
    ) : MemberScope {
        constructor(owner: ClassDescriptor, forDelegatedMembersOnly: Boolean = false) :
                this(owner.unsubstitutedMemberScope, owner, forDelegatedMembersOnly)

        override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> {
            return allMemberScope.getContributedVariables(name, location).filter {
                it.isDeclaredInOwner() && it.isDelegatedIfRequired()
            }.mapToDelegatedIfRequired()
        }

        override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> {
            return allMemberScope.getContributedFunctions(name, location).filter {
                it.isDeclaredInOwner() && it.isDelegatedIfRequired()
            }.mapToDelegatedIfRequired()
        }

        override fun getFunctionNames(): Set<Name> {
            return allMemberScope.getFunctionNames().filterTo(mutableSetOf()) { name ->
                getContributedFunctions(name, NoLookupLocation.FROM_IDE).isNotEmpty()
            }
        }

        override fun getVariableNames(): Set<Name> {
            return allMemberScope.getVariableNames().filterTo(mutableSetOf()) { name ->
                getContributedVariables(name, NoLookupLocation.FROM_IDE).isNotEmpty()
            }
        }

        override fun getClassifierNames(): Set<Name>? {
            if (forDelegatedMembersOnly) return null
            return allMemberScope.getClassifierNames()?.filterTo(mutableSetOf()) { name ->
                getContributedClassifier(name, NoLookupLocation.FROM_IDE) != null
            }
        }

        override fun printScopeStructure(p: Printer) {
            allMemberScope.printScopeStructure(p)
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            if (forDelegatedMembersOnly) return null
            return allMemberScope.getContributedClassifier(name, location)?.takeIf { it.isDeclaredInOwner() }
        }

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> {
            return allMemberScope.getContributedDescriptors(kindFilter, nameFilter).filter {
                it.isDeclaredInOwner() && it.isDelegatedIfRequired()
            }.mapToDelegatedIfRequired()
        }

        private fun DeclarationDescriptor.isDelegatedIfRequired(): Boolean =
            !forDelegatedMembersOnly || this is CallableMemberDescriptor && kind == CallableMemberDescriptor.Kind.DELEGATION

        private inline fun <reified D : DeclarationDescriptor> Collection<D>.mapToDelegatedIfRequired(): Collection<D> {
            if (!forDelegatedMembersOnly) return this
            return map {
                val overridden = (it as CallableMemberDescriptor).overriddenDescriptors.firstOrNull()
                overridden?.newCopyBuilder()
                    ?.setModality(Modality.OPEN)
                    ?.setKind(CallableMemberDescriptor.Kind.DELEGATION)
                    ?.setDispatchReceiverParameter(it.dispatchReceiverParameter)
                    ?.setPreserveSourceElement()
                    ?.build() as? D ?: it
            }
        }


        private fun DeclarationDescriptor.isDeclaredInOwner() = when (this) {
            is CallableDescriptor -> dispatchReceiverParameter?.containingDeclaration == owner
            else -> containingDeclaration == owner
        }
    }

    override val KaFileSymbol.fileScope: KaScope
        get() = withValidityAssertion {
            require(this is KaFe10FileSymbol)
            return KaFe10FileScope(psi, analysisContext, token)
        }

    override val KaPackageSymbol.packageScope: KaScope
        get() = withValidityAssertion {
            require(this is KaFe10PackageSymbol)
            val packageFragments = analysisContext.resolveSession.packageFragmentProvider.packageFragments(fqName)
            val scopeDescription = "Compound scope for package \"${fqName}\""
            val chainedScope = ChainedMemberScope.create(scopeDescription, packageFragments.map { it.getMemberScope() })
            return KaFe10PackageScope(chainedScope, this, analysisContext)
        }

    override fun List<KaScope>.asCompositeScope(): KaScope = withValidityAssertion {
        return KaBaseCompositeScope.create(this, token)
    }

    override val KaType.scope: KaTypeScope?
        get() = withValidityAssertion {
            require(this is KaFe10Type)
            TODO()
        }

    override val KaTypeScope.declarationScope: KaScope
        get() = withValidityAssertion {
            TODO()
        }

    override val KaType.syntheticJavaPropertiesScope: KaTypeScope?
        get() = withValidityAssertion {
            require(this is KaFe10Type)
            TODO()
        }

    override fun KtFile.scopeContext(position: KtElement): KaScopeContext = withPsiValidityAssertion(this, position) {
        val elementToAnalyze = position.containingNonLocalDeclaration() ?: this
        val bindingContext = analysisContext.analyze(elementToAnalyze)

        val scopeKind = KaScopeKinds.LocalScope(0) // TODO
        val lexicalScope = position.getResolutionScope(bindingContext)
        if (lexicalScope != null) {
            val compositeScope = KaBaseCompositeScope.create(listOf(KaFe10ScopeLexical(lexicalScope, analysisContext)), token)
            return KaBaseScopeContext(
                listOf(KaScopeWithKindImpl(compositeScope, scopeKind)),
                collectImplicitReceivers(lexicalScope),
                token,
            )
        }

        val fileScope = analysisContext.resolveSession.fileScopeProvider.getFileResolutionScope(this)
        val compositeScope = KaBaseCompositeScope.create(listOf(KaFe10ScopeLexical(fileScope, analysisContext)), token)
        return KaBaseScopeContext(
            listOf(KaScopeWithKindImpl(compositeScope, scopeKind)),
            collectImplicitReceivers(fileScope),
            token,
        )
    }

    override val KtFile.importingScopeContext: KaScopeContext
        get() = withPsiValidityAssertion {
            val importingScopes = scopeContext(position = this)
                .scopes
                .filter { it.kind is KaScopeKind.ImportingScope }
            return KaBaseScopeContext(importingScopes, implicitValues = emptyList(), token)
        }

    private inline fun <reified T : DeclarationDescriptor> getDescriptor(symbol: KaSymbol): T? {
        return when (symbol) {
            is KaFe10DescSymbol<*> -> symbol.descriptor as? T
            is KaFe10PsiSymbol<*, *> -> symbol.descriptor as? T
            else -> {
                require(symbol is KaFe10Symbol) { "Unrecognized symbol implementation found" }
                null
            }
        }
    }

    private fun collectImplicitReceivers(scope: LexicalScope): MutableList<KaImplicitReceiver> {
        val result = mutableListOf<KaImplicitReceiver>()

        for ((index, implicitReceiver) in scope.getImplicitReceiversHierarchy().withIndex()) {
            val type = implicitReceiver.type.toKtType(analysisContext)
            val ownerDescriptor = implicitReceiver.containingDeclaration
            val owner = ownerDescriptor.toKtSymbol(analysisContext)

            if (owner == null) {
                LOG.error("Unexpected implicit receiver owner: $ownerDescriptor (${ownerDescriptor.javaClass})")
                continue
            }

            result += KaBaseScopeImplicitReceiverValue(type, owner, index)
        }

        return result
    }
}
