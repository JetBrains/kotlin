/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaFirSessionBoundOwner
import org.jetbrains.kotlin.analysis.api.fir.entryPointImplementation
import org.jetbrains.kotlin.analysis.api.fir.scopes.KaFirEmptyScope
import org.jetbrains.kotlin.analysis.api.fir.scopes.KaFirScope
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsResolveExtensionInfoProvider
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionTool
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.LLFirResolveExtensionToolDeclarationProvider
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.extensions.navigationTargetsProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration

internal class KaFirResolveExtensionInfoProvider(
    override val analysisSessionProvider: () -> KaFirSession
) : KaFirSessionComponent(), KaInternalsResolveExtensionInfoProvider {
    override fun resolveExtensionScopeWithTopLevelDeclarations(): KaScope = entryPointImplementation {
        val tools = analysisSession.extensionTools
        if (tools.isEmpty()) return KaFirEmptyScope(token)
        return KaFirResolveExtensionScope(analysisSession, tools)
    }

    override fun isResolveExtensionFile(virtualFile: VirtualFile): Boolean = entryPointImplementation {
        virtualFile.navigationTargetsProvider != null
    }

    override fun isFromResolveExtension(element: KtElement): Boolean = entryPointImplementation {
        element.containingKtFile.virtualFile?.let { isResolveExtensionFile(it) } == true
    }

    override fun resolveExtensionNavigationElements(element: KtElement): Collection<PsiElement> = entryPointImplementation {
        val targetsProvider = element.containingFile?.virtualFile?.navigationTargetsProvider ?: return emptyList()
        return with(targetsProvider) { analysisSession.getNavigationTargets(element) }
    }
}

private class KaFirResolveExtensionScope(
    override val analysisSession: KaFirSession,
    private val tools: List<LLFirResolveExtensionTool>,
) : KaFirScope(), KaFirSessionBoundOwner {
    init {
        require(tools.isNotEmpty())
    }


    override fun callables(nameFilter: (Name) -> Boolean): Sequence<KaCallableSymbol> = entryPointImplementation {
        getTopLevelDeclarations(nameFilter) { it.getTopLevelCallables() }
    }

    override fun callables(names: Collection<Name>): Sequence<KaCallableSymbol> = entryPointImplementation {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return callables { it in namesSet }
    }

    override fun classifiers(nameFilter: (Name) -> Boolean): Sequence<KaClassifierSymbol> = entryPointImplementation {
        getTopLevelDeclarations(nameFilter) { it.getTopLevelClassifiers() }
    }

    override fun classifiers(names: Collection<Name>): Sequence<KaClassifierSymbol> = entryPointImplementation {
        if (names.isEmpty()) return emptySequence()
        val namesSet = names.toSet()
        return classifiers { it in namesSet }
    }

    context(analysisSession: KaFirSession)
    private inline fun <D : KtNamedDeclaration, reified S : KaDeclarationSymbol> getTopLevelDeclarations(
        crossinline nameFilter: (Name) -> Boolean,
        crossinline getDeclarationsByProvider: (LLFirResolveExtensionToolDeclarationProvider) -> Sequence<D>,
    ): Sequence<S> = sequence {
        for (tool in tools) {
            for (declaration in getDeclarationsByProvider(tool.declarationProvider)) {
                val declarationName = declaration.nameAsName ?: continue
                if (!nameFilter(declarationName)) continue
                yield(declaration.symbol as S)
            }
        }
    }

    override val constructors: Sequence<KaConstructorSymbol>
        get() = entryPointImplementation { emptySequence() }

    override fun getPackageSymbols(nameFilter: (Name) -> Boolean): Sequence<KaPackageSymbol> = entryPointImplementation {
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

    override fun getPossibleCallableNames(): Set<Name> = entryPointImplementation {
        tools.flatMapTo(mutableSetOf()) { it.declarationProvider.getTopLevelCallableNames() }
    }

    override fun getPossibleClassifierNames(): Set<Name> = entryPointImplementation {
        tools.flatMapTo(mutableSetOf()) { it.declarationProvider.getTopLevelClassifierNames() }
    }
}
