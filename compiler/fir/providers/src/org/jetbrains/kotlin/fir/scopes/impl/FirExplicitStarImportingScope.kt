/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession

open class FirExplicitStarImportingScope(
    imports: List<FirImport>,
    session: FirSession,
    scopeSession: ScopeSession
) : FirAbstractStarImportingScope(session, scopeSession, lookupInFir = true) {
    override val starImports = imports.filterIsInstance<FirResolvedImport>().filter { it.isAllUnder }

    override val scopeOwnerLookupNames: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        starImports.mapTo(LinkedHashSet()) { it.packageFqName.asString() }.toList()
    }
}
