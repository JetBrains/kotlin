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
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.defaultImportsProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirDefaultSimpleImportingScope(
    session: FirSession,
    scopeSession: ScopeSession,
    val priority: DefaultImportPriority,
    private val excludedImportNames: Set<FqName>,
) : FirAbstractSimpleImportingScope(session, scopeSession) {

    private fun FirImport.resolve(importResolveTransformer: FirImportResolveTransformer) =
        importResolveTransformer.transformImport(this, null) as? FirResolvedImport

    override val simpleImports: Map<Name, List<FirResolvedImport>> = run {
        val importResolveTransformer = FirImportResolveTransformer(session)
        val defaultImportsProvider = session.defaultImportsProvider
        val allDefaultImports = priority.getAllDefaultImports(defaultImportsProvider, LanguageVersionSettingsImpl.DEFAULT)
        allDefaultImports
            ?.filter { !it.isAllUnder && it.fqName !in excludedImportNames && it.fqName !in defaultImportsProvider.excludedImports }
            ?.mapNotNull {
                buildImport {
                    importedFqName = it.fqName
                    isAllUnder = false
                    isPackage = false
                }.resolve(importResolveTransformer)
            }
            ?.groupBy { it.importedName!! }
            ?: emptyMap()
    }

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirDefaultSimpleImportingScope {
        return FirDefaultSimpleImportingScope(newSession, newScopeSession, priority, excludedImportNames)
    }
}
