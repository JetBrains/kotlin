/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl

class FirDefaultStarImportingScope(session: FirSession, lookupInFir: Boolean = false) :
    FirAbstractStarImportingScope(session, lookupInFir) {

    // TODO: put languageVersionSettings into FirSession?
    override val starImports = session.moduleInfo?.analyzerServices?.getDefaultImports(LanguageVersionSettingsImpl.DEFAULT, true)
        ?.filter { it.isAllUnder }
        ?.map {
            FirResolvedImportImpl(
                session,
                FirImportImpl(session, null, it.fqName, isAllUnder = true, aliasName = null),
                it.fqName,
                null
            )
        } ?: emptyList()
}
