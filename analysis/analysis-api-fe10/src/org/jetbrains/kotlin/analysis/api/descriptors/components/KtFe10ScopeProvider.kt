/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.descriptors.components

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.analysis.api.components.KtImplicitReceiver
import org.jetbrains.kotlin.analysis.api.components.KtScopeContext
import org.jetbrains.kotlin.analysis.api.components.KtScopeProvider
import org.jetbrains.kotlin.analysis.api.descriptors.KtFe10AnalysisSession
import org.jetbrains.kotlin.analysis.api.descriptors.components.base.Fe10KtAnalysisSessionComponent
import org.jetbrains.kotlin.analysis.api.descriptors.scopes.*
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10FileSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.KtFe10PackageSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.base.KtFe10Symbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.KtFe10DescSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.descriptorBased.base.toKtType
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.KtFe10PsiSymbol
import org.jetbrains.kotlin.analysis.api.descriptors.symbols.psiBased.base.getResolutionScope
import org.jetbrains.kotlin.analysis.api.descriptors.types.base.KtFe10Type
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtEmptyScope
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KtCompositeScope
import org.jetbrains.kotlin.analysis.api.scopes.*
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.analysis.api.tokens.ValidityToken
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.withValidityAssertion
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.packageFragments
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.util.containingNonLocalDeclaration

internal class KtFe10ScopeProvider(
    override val analysisSession: KtFe10AnalysisSession
) : KtScopeProvider(), Fe10KtAnalysisSessionComponent {
    private companion object {
        val LOG = Logger.getInstance(KtFe10ScopeProvider::class.java)
    }

    override val token: ValidityToken
        get() = analysisSession.token

    override fun getMemberScope(classSymbol: KtSymbolWithMembers): KtScope = withValidityAssertion {
        val descriptor = getDescriptor<ClassDescriptor>(classSymbol)
            ?: return getEmptyScope()

        // TODO either this or declared scope should return a different set of members
        return KtFe10ScopeMember(descriptor.unsubstitutedMemberScope, analysisContext)
    }

    override fun getDeclaredMemberScope(classSymbol: KtSymbolWithMembers): KtScope = withValidityAssertion {
        val descriptor = getDescriptor<ClassDescriptor>(classSymbol)
            ?: return getEmptyScope()

        // TODO: need to return declared members only
        return KtFe10ScopeMember(descriptor.unsubstitutedMemberScope, analysisContext)
    }

    override fun getDelegatedMemberScope(classSymbol: KtSymbolWithMembers): KtScope = withValidityAssertion {
        val descriptor = getDescriptor<ClassDescriptor>(classSymbol)
            ?: return getEmptyScope()

        // TODO: need to return delegated members only
        return KtFe10ScopeMember(descriptor.unsubstitutedMemberScope, analysisContext)
    }

    override fun getStaticMemberScope(symbol: KtSymbolWithMembers): KtScope = withValidityAssertion {
        val descriptor = getDescriptor<ClassDescriptor>(symbol) ?: return getEmptyScope()
        return KtFe10ScopeMember(descriptor.staticScope, analysisContext)
    }

    override fun getEmptyScope(): KtScope = withValidityAssertion {
        KtEmptyScope(token)
    }

    override fun getFileScope(fileSymbol: KtFileSymbol): KtScope = withValidityAssertion {
        require(fileSymbol is KtFe10FileSymbol)
        val scope = analysisContext.resolveSession.fileScopeProvider.getFileResolutionScope(fileSymbol.psi)

        return KtFe10ScopeLexical(scope, analysisContext)
    }

    override fun getPackageScope(packageSymbol: KtPackageSymbol): KtScope = withValidityAssertion {
        require(packageSymbol is KtFe10PackageSymbol)
        val packageFragments = analysisContext.resolveSession.packageFragmentProvider.packageFragments(packageSymbol.fqName)
        val scopeDescription = "Compound scope for package \"${packageSymbol.fqName}\""
        val chainedScope = ChainedMemberScope.create(scopeDescription, packageFragments.map { it.getMemberScope() })
        return KtFe10PackageScope(chainedScope, packageSymbol, analysisContext)
    }

    override fun getCompositeScope(subScopes: List<KtScope>): KtScope = withValidityAssertion {
        return KtCompositeScope(subScopes, token)
    }

    override fun getTypeScope(type: KtType): KtScope = withValidityAssertion {
        require(type is KtFe10Type)
        return KtFe10ScopeMember(type.type.memberScope, analysisContext)
    }

    override fun getScopeContextForPosition(originalFile: KtFile, positionInFakeFile: KtElement): KtScopeContext = withValidityAssertion {
        val elementToAnalyze = positionInFakeFile.containingNonLocalDeclaration() ?: originalFile
        val bindingContext = analysisContext.analyze(elementToAnalyze)

        val lexicalScope = positionInFakeFile.getResolutionScope(bindingContext)
        if (lexicalScope != null) {
            val compositeScope = KtCompositeScope(listOf(KtFe10ScopeLexical(lexicalScope, analysisContext)), token)
            return KtScopeContext(compositeScope, collectImplicitReceivers(lexicalScope))
        }

        val fileScope = analysisContext.resolveSession.fileScopeProvider.getFileResolutionScope(originalFile)
        val compositeScope = KtCompositeScope(listOf(KtFe10ScopeLexical(fileScope, analysisContext)), token)
        return KtScopeContext(compositeScope, collectImplicitReceivers(fileScope))
    }

    private inline fun <reified T : DeclarationDescriptor> getDescriptor(symbol: KtSymbol): T? {
        return when (symbol) {
            is KtFe10DescSymbol<*> -> symbol.descriptor as? T
            is KtFe10PsiSymbol<*, *> -> symbol.descriptor as? T
            else -> {
                require(symbol is KtFe10Symbol) { "Unrecognized symbol implementation found" }
                null
            }
        }
    }

    private fun collectImplicitReceivers(scope: LexicalScope): MutableList<KtImplicitReceiver> {
        val result = mutableListOf<KtImplicitReceiver>()

        for (implicitReceiver in scope.getImplicitReceiversHierarchy()) {
            val type = implicitReceiver.type.toKtType(analysisContext)
            val ownerDescriptor = implicitReceiver.containingDeclaration
            val owner = ownerDescriptor.toKtSymbol(analysisContext)

            if (owner == null) {
                LOG.error("Unexpected implicit receiver owner: $ownerDescriptor (${ownerDescriptor.javaClass})")
                continue
            }

            result += KtImplicitReceiver(token, type, owner)
        }

        return result
    }
}
