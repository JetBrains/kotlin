/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer

class FirDefaultSimpleImportingScope(session: FirSession) : FirAbstractSimpleImportingScope(session) {

    private fun FirImportImpl.resolve(importResolveTransformer: FirImportResolveTransformer) =
        importResolveTransformer.transformImport(this, null).single as FirResolvedImportImpl

    override val simpleImports = run {
        val importResolveTransformer = FirImportResolveTransformer(session)
        session.moduleInfo?.compilerServices?.getDefaultImports(LanguageVersionSettingsImpl.DEFAULT, true)
            ?.filter { !it.isAllUnder }
            ?.map {
                FirImportImpl(session, null, it.fqName, isAllUnder = false, aliasName = null)
                    .resolve(importResolveTransformer)
            }?.groupBy { it.importedName!! } ?: emptyMap()
    }
}
