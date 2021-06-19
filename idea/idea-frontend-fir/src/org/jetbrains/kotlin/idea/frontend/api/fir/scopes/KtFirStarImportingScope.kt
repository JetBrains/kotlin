/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.scopes

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.fir.scopes.getContainingCallableNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.getContainingClassifierNamesIfPresent
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirDefaultStarImportingScope
import org.jetbrains.kotlin.idea.fir.low.level.api.createDeclarationProvider
import org.jetbrains.kotlin.idea.frontend.api.ValidityTokenOwner
import org.jetbrains.kotlin.idea.frontend.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.cached
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.weakRef
import org.jetbrains.kotlin.idea.frontend.api.scopes.Import
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtStarImportingScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.StarImport
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.ValidityToken
import org.jetbrains.kotlin.idea.frontend.api.withValidityAssertion
import org.jetbrains.kotlin.name.Name

internal class KtFirStarImportingScope(
    firScope: FirAbstractStarImportingScope,
    private val builder: KtSymbolByFirBuilder,
    project: Project,
    override val token: ValidityToken,
) : KtStarImportingScope, ValidityTokenOwner {
    private val firScope: FirAbstractStarImportingScope by weakRef(firScope)
    override val isDefaultImportingScope: Boolean = withValidityAssertion { firScope is FirDefaultStarImportingScope }

    //todo use more concrete scope
    private val declarationProvider = project.createDeclarationProvider(GlobalSearchScope.allScope(project))

    override val imports: List<StarImport> by cached {
        firScope.starImports.map { import ->
            StarImport(
                import.packageFqName,
                import.relativeClassName,
                import.resolvedClassId
            )
        }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = emptySequence()

    // todo cache?
    @OptIn(ExperimentalStdlibApi::class)
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import: Import ->
            if (import.relativeClassName == null) { // top level callable
                declarationProvider.getFunctionsNamesInPackage(import.packageFqName) +
                        declarationProvider.getPropertyNamesInPackage(import.packageFqName)
            } else { //member
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getContainingCallableNamesIfPresent().orEmpty()
            }
        }
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import ->
            if (import.relativeClassName == null) {
                declarationProvider.getClassNamesInPackage(import.packageFqName) +
                        declarationProvider.getTypeAliasNamesInPackage(import.packageFqName)
            } else {
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getContainingClassifierNamesIfPresent().orEmpty()
            }
        }
    }

}