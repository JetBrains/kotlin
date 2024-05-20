/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.scopes

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.utils.cached
import org.jetbrains.kotlin.analysis.api.lifetime.withValidityAssertion
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.fir.scopes.impl.FirAbstractStarImportingScope
import org.jetbrains.kotlin.name.Name

internal class KaFirStarImportingScope(
    firScope: FirAbstractStarImportingScope,
    private val analysisSession: KaFirSession,
) : KaFirBasedScope<FirAbstractStarImportingScope>(firScope, analysisSession.firSymbolBuilder) {

    private val imports: List<StarImport> by cached {
        firScope.starImports.map { import ->
            StarImport(
                import.packageFqName,
                import.relativeParentClassName,
                import.resolvedParentClassId
            )
        }
    }

    override fun getConstructors(): Sequence<KaConstructorSymbol> = withValidityAssertion { emptySequence() }

    // todo cache?
    override fun getPossibleCallableNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import: Import ->
            if (import.relativeClassName == null) { // top level callable
                DeclarationsInPackageProvider.getTopLevelCallableNamesInPackageProvider(import.packageFqName, analysisSession)
            } else { //member
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getCallableNames().orEmpty()
            }
        }
    }

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> = withValidityAssertion {
        emptySequence()
    }

    override fun getPossibleClassifierNames(): Set<Name> = withValidityAssertion {
        imports.flatMapTo(hashSetOf()) { import ->
            if (import.relativeClassName == null) {
                DeclarationsInPackageProvider.getTopLevelClassifierNamesInPackageProvider(import.packageFqName, analysisSession)
            } else {
                val classId = import.resolvedClassId ?: error("Class id should not be null as relativeClassName is not null")
                firScope.getStaticsScope(classId)?.getClassifierNames().orEmpty()
            }
        }
    }
}
