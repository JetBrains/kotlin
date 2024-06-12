/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.components.KaResolveExtensionInfoProvider
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.scopes.KaEmptyScope
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionToolDeclarationProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.navigationTargetsProvider
import org.jetbrains.kotlin.analysis.project.structure.KtModuleStructureInternals
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration

@OptIn(KtModuleStructureInternals::class)
internal class KaFirResolveExtensionInfoProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaSessionComponent<KaFirSession>(), KaResolveExtensionInfoProvider, KaFirSessionComponent {
    override val resolveExtensionScopeWithTopLevelDeclarations: KaScope
        get() = withValidityAssertion {
            val tools = analysisSession.extensionTools
            if (tools.isEmpty()) return KaEmptyScope(token)
            return KaFirResolveExtensionScope(analysisSession, tools)
        }

    @OptIn(KtModuleStructureInternals::class)
    override val VirtualFile.isResolveExtensionFile: Boolean
        get() = withValidityAssertion {
            navigationTargetsProvider != null
        }

    override val KtElement.isFromResolveExtension: Boolean
        get() = withValidityAssertion {
            containingKtFile.virtualFile?.isResolveExtensionFile == true
        }

    override val KtElement.resolveExtensionNavigationElements: Collection<PsiElement>
        get() = withValidityAssertion {
            val targetsProvider = containingFile?.virtualFile?.navigationTargetsProvider ?: return emptyList()
            return with(targetsProvider) { analysisSession.getNavigationTargets(this@resolveExtensionNavigationElements) }
        }
}

private class KaFirResolveExtensionScope(
    private val analysisSession: KaFirSession,
    private val tools: List<LLFirResolveExtensionTool>,
) : KaScope {
    init {
        require(tools.isNotEmpty())
    }

    override val token: KaLifetimeToken get() = analysisSession.token

    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = withValidityAssertion {
        getTopLevelDeclarations(nameFilter) { it.getTopLevelCallables() }
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return callables { it in namesSet }
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = withValidityAssertion {
        getTopLevelDeclarations(nameFilter) { it.getTopLevelClassifiers() }
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = withValidityAssertion {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return classifiers { it in namesSet }
    }

    private inline fun <D : KtNamedDeclaration, reified S : KaDeclarationSymbol> getTopLevelDeclarations(
        crossinline nameFilter: (Name) -> Boolean,
        crossinline getDeclarationsByProvider: (LLFirResolveExtensionToolDeclarationProvider) -> Sequence<D>,
    ): Sequence<S> = sequence {
        for (tool in tools) {
            for (declaration in getDeclarationsByProvider(tool.declarationProvider)) {
                val declarationName = declaration.nameAsName ?: continue
                if (!nameFilter(declarationName)) continue
                with(analysisSession) {
                    yield(declaration.symbol as S)
                }
            }
        }
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = withValidityAssertion { emptySequence() }

    @KaExperimentalApi
    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = withValidityAssertion {
        sequence {
            // Only emit package symbols for top-level packages (subpackages of root). This matches the behavior
            // of the root-level KtFirPackageScope.
            val seenTopLevelPackages = mutableSetOf<Name>()
            for (tool in tools) {
                for (packageName in tool.packageFilter.getAllSubPackages(FqName.ROOT)) {
                    if (seenTopLevelPackages.add(packageName) && nameFilter(packageName)) {
                        yield(analysisSession.firSymbolBuilder.createPackageSymbol(FqName.ROOT.child(packageName)))
                    }
                }
            }
        }
    }

    @KaExperimentalApi
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        tools.flatMapTo(mutableSetOf()) { it.declarationProvider.getTopLevelCallableNames() }
    }

    @KaExperimentalApi
    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        tools.flatMapTo(mutableSetOf()) { it.declarationProvider.getTopLevelClassifierNames() }
    }
}