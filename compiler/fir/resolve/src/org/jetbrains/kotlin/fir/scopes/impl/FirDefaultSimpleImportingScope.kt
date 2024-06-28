/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.scopes.defaultImportProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirDefaultSimpleImportingScope(
    session: FirSession,
    scopeSession: ScopeSession,
    priority: DefaultImportPriority,
    private val excludedImportNames: Set<FqName>,
) : FirAbstractSimpleImportingScope(session, scopeSession) {

    private fun FirImport.resolve(importResolveTransformer: FirImportResolveTransformer) =
        importResolveTransformer.transformImport(this, null) as? FirResolvedImport

    override val simpleImports: Map<Name, List<FirResolvedImport>> = run {
        val importResolveTransformer = FirImportResolveTransformer(session)
        val defaultImportProvider = session.defaultImportProvider
        val allDefaultImports = priority.getAllDefaultImports(defaultImportProvider, LanguageVersionSettingsImpl.DEFAULT)
        allDefaultImports
            ?.filter { !it.isAllUnder && it.fqName !in excludedImportNames && it.fqName !in defaultImportProvider.excludedImports }
            ?.mapNotNull {
                buildImport {
                    importedFqName = it.fqName
                    isAllUnder = false
                }.resolve(importResolveTransformer)
            }
            ?.groupBy { it.importedName!! }
            ?: emptyMap()
    }
}
