/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KtSymbolByFirBuilder
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.name.Name

internal class KtFirStarImportingScope(
    private val firScope: FirAbstractStarImportingScope,
    private val builder: KtSymbolByFirBuilder,
    private val declarationProvider: KotlinDeclarationProvider,
) : KtScope {
    override val token: KtLifetimeToken get() = builder.token

    private val imports: List<StarImport> by cached {
        firScope.starImports.map { import ->
            StarImport(
                import.packageFqName,
                import.relativeParentClassName,
                import.resolvedParentClassId
            )
        }
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = withValidityAssertion {
        firScope.getCallableSymbols(getPossibleCallableNames().filter(nameFilter), builder)
    }

    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = withValidityAssertion {
        firScope.getClassifierSymbols(getPossibleClassifierNames().filter(nameFilter), builder)
    }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = withValidityAssertion { emptySequence() }

    // todo cache?
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import: Import ->
            if (import.relativeClassName == null) { // top level callable
                declarationProvider.getTopLevelCallableNamesInPackage(import.packageFqName)
            } else { //member
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getCallableNames().orEmpty()
            }
        }
    }


    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = withValidityAssertion {
        emptySequence()
    }


    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import ->
            if (import.relativeClassName == null) {
                declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(import.packageFqName)
            } else {
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getClassifierNames().orEmpty()
            }
        }
    }

}
