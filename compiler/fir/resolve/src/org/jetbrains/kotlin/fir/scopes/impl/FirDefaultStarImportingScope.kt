/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.resolve.ScopeSession

class FirDefaultStarImportingScope(
    session: FirSession,
    scopeSession: ScopeSession,
    priority: DefaultImportPriority
) : FirAbstractStarImportingScope(session, scopeSession, lookupInFir = false) {

    // TODO: put languageVersionSettings into FirSession?
    override val starImports = run {
        val analyzerServices = session.moduleInfo?.analyzerServices
        val allDefaultImports = priority.getAllDefaultImports(analyzerServices, LanguageVersionSettingsImpl.DEFAULT)
        allDefaultImports
            ?.filter { it.isAllUnder }
            ?.map {
                FirResolvedImportImpl(
                    FirImportImpl(null, it.fqName, isAllUnder = true, aliasName = null),
                    it.fqName,
                    null
                )
            } ?: emptyList()
    }
}
