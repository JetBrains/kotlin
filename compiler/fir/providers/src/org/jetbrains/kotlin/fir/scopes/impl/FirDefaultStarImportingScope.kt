/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.builder.buildResolvedImport
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

class FirDefaultStarImportingScope(
    session: FirSession,
    scopeSession: ScopeSession,
    priority: DefaultImportPriority
) : FirAbstractStarImportingScope(
    session, scopeSession,
    lookupInFir = session.languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)
) {
    // TODO: put languageVersionSettings into FirSession?
    override val starImports = run {
        val analyzerServices = session.moduleData.analyzerServices
        val allDefaultImports = priority.getAllDefaultImports(analyzerServices, LanguageVersionSettingsImpl.DEFAULT)
        allDefaultImports
            ?.filter { it.isAllUnder }
            ?.map {
                buildResolvedImport {
                    delegate = buildImport {
                        importedFqName = it.fqName
                        isAllUnder = true
                    }
                    packageFqName = it.fqName
                }
            } ?: emptyList()
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        if (name.isSpecial || name.identifier.isNotEmpty()) {
            val containsKotlinPackage = session.firProvider.containsKotlinPackage()
            for (import in starImports) {
                for (symbol in provider.getTopLevelFunctionSymbols(import.packageFqName, name)) {
                    processor(symbol)
                }
                if (containsKotlinPackage) {
                    for (symbol in session.firProvider.symbolProvider.getTopLevelFunctionSymbols(import.packageFqName, name)) {
                        processor(symbol)
                    }
                }
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        if (name.isSpecial || name.identifier.isNotEmpty()) {
            for (import in starImports) {
                for (symbol in provider.getTopLevelPropertySymbols(import.packageFqName, name)) {
                    processor(symbol)
                }
            }
        }
    }
}
