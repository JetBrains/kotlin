/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.name.FqName

open class FirExplicitStarImportingScope private constructor(
    session: FirSession,
    scopeSession: ScopeSession,
    override val starImports: List<FirResolvedImport>,
    excludedImportNames: Set<FqName>
) : FirAbstractStarImportingScope(session, scopeSession, lookupInFir = true, excludedImportNames) {
    constructor(
        imports: List<FirImport>,
        session: FirSession,
        scopeSession: ScopeSession,
        excludedImportNames: Set<FqName>
    ) : this(
        session, scopeSession,
        starImports = imports.filterIsInstance<FirResolvedImport>().filter { it.isAllUnder },
        excludedImportNames
    )

    override val scopeOwnerLookupNames: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        starImports.mapTo(LinkedHashSet()) { it.packageFqName.asString() }.toList()
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirExplicitStarImportingScope {
        return FirExplicitStarImportingScope(newSession, newScopeSession, starImports, excludedImportNames)
    }
}
