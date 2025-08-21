/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.FirScript
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.firScriptResolutionConfigurators
import org.jetbrains.kotlin.fir.extensions.replSnippetResolveExtensions
import org.jetbrains.kotlin.fir.importTracker
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.reportImportDirectives
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.resolve.transformers.FirImportResolveTransformer
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

private val ALL_IMPORTS = scopeSessionKey<FirFile, ListStorageFirScope>()
private val DEFAULT_STAR_IMPORT = scopeSessionKey<DefaultStarImportKey, FirSingleLevelDefaultStarImportingScope>()
private val DEFAULT_SIMPLE_IMPORT = scopeSessionKey<DefaultSimpleImportKey, FirDefaultSimpleImportingScope>()
private val DEFAULT_SCRIPT_STAR_IMPORT = scopeSessionKey<FirFile, FirExplicitStarImportingScope>()
private val DEFAULT_SCRIPT_SIMPLE_IMPORT = scopeSessionKey<FirFile, FirExplicitSimpleImportingScope>()

private data class DefaultStarImportKey(val priority: DefaultImportPriority, val excludedImportNames: Set<FqName>)

private data class DefaultSimpleImportKey(val priority: DefaultImportPriority, val excludedImportNames: Set<FqName>)

fun createImportingScopes(
    file: FirFile,
    session: FirSession,
    scopeSession: ScopeSession,
    useCaching: Boolean = true,
): List<FirScope> = if (useCaching) {
    scopeSession.getOrBuild(file, ALL_IMPORTS) {
        ListStorageFirScope(computeImportingScopes(file, session, scopeSession))
    }.result
} else {
    computeImportingScopes(file, session, scopeSession)
}

internal fun computeImportingScopes(
    file: FirFile,
    session: FirSession,
    scopeSession: ScopeSession,
    includeDefaultImports: Boolean = true,
    includePackageImport: Boolean = true
): List<FirScope> {
    file.lazyResolveToPhase(FirResolvePhase.IMPORTS)
    val excludedImportNames =
        file.imports.filter { it.aliasName != null }.mapNotNullTo(hashSetOf()) { it.importedFqName }.ifEmpty { emptySet() }

    val excludedNamesInPackage =
        excludedImportNames.mapNotNullTo(mutableSetOf()) {
            if (it.parent() == file.packageFqName) it.shortName() else null
        }

    session.importTracker?.let { tracker ->
        file.imports.map { import ->
            tracker.reportImportDirectives(file.sourceFile?.path, import.importedFqName?.asString())
        }
    }

    val scriptingDefaultImports = getDefaultImportsForScripting(session, file)

    return buildList {
        if (includeDefaultImports) {
            this += FirDefaultStarImportingScope(
                scopeSession.getOrBuild(DefaultStarImportKey(DefaultImportPriority.HIGH, excludedImportNames), DEFAULT_STAR_IMPORT) {
                    FirSingleLevelDefaultStarImportingScope(session, scopeSession, DefaultImportPriority.HIGH, excludedImportNames)
                },
                scopeSession.getOrBuild(DefaultStarImportKey(DefaultImportPriority.LOW, excludedImportNames), DEFAULT_STAR_IMPORT) {
                    FirSingleLevelDefaultStarImportingScope(session, scopeSession, DefaultImportPriority.LOW, excludedImportNames)
                },
            )
            if (scriptingDefaultImports != null) {
                this += scopeSession.getOrBuild(file, DEFAULT_SCRIPT_STAR_IMPORT) {
                    FirExplicitStarImportingScope(scriptingDefaultImports.first, session, scopeSession, excludedImportNames)
                }
            }
        }

        this += FirExplicitStarImportingScope(file.imports, session, scopeSession, excludedImportNames)

        if (includeDefaultImports) {
            this += scopeSession.getOrBuild(DefaultSimpleImportKey(DefaultImportPriority.LOW, excludedImportNames), DEFAULT_SIMPLE_IMPORT) {
                FirDefaultSimpleImportingScope(session, scopeSession, priority = DefaultImportPriority.LOW, excludedImportNames)
            }
            this += scopeSession.getOrBuild(DefaultSimpleImportKey(DefaultImportPriority.HIGH, excludedImportNames), DEFAULT_SIMPLE_IMPORT) {
                FirDefaultSimpleImportingScope(session, scopeSession, priority = DefaultImportPriority.HIGH, excludedImportNames)
            }
            if (scriptingDefaultImports != null) {
                this += scopeSession.getOrBuild(file, DEFAULT_SCRIPT_SIMPLE_IMPORT) {
                    FirExplicitSimpleImportingScope(scriptingDefaultImports.second, session, scopeSession)
                }
            }
        }

        if (includePackageImport) {
            this += when {
                excludedNamesInPackage.isEmpty() ->
                    // Supposed to be the most common branch, so we cache it
                    scopeSession.getOrBuild(file.packageFqName to session, PACKAGE_MEMBER) {
                        FirPackageMemberScope(file.packageFqName, session, excludedNames = emptySet())
                    }
                else ->
                    FirPackageMemberScope(file.packageFqName, session, excludedNames = excludedNamesInPackage)
            }
        }
        // TODO: explicit simple importing scope should have highest priority (higher than inner scopes added in process)
        this += FirExplicitSimpleImportingScope(file.imports, session, scopeSession)
    }
}

private fun getDefaultImportsForScripting(session: FirSession, file: FirFile): Pair<List<FirImport>, List<FirImport>>? {

    fun List<FirImport>.transformImports(): Pair<List<FirImport>, List<FirImport>> =
        with(FirImportResolveTransformer(session)) {
            map { firImport ->
                (transformImport(firImport, data = null) as? FirResolvedImport) ?: firImport
            }.partition { it.isAllUnder }
        }

    return when (val scriptOrSnippet = file.declarations.firstOrNull()) {
        is FirScript ->
            session.extensionService.firScriptResolutionConfigurators.flatMap {
                it.getScriptDefaultImports(scriptOrSnippet).orEmpty()
            }.transformImports()
        is FirReplSnippet ->
            session.extensionService.replSnippetResolveExtensions.flatMap {
                it.getSnippetDefaultImports(file.sourceFile!!, scriptOrSnippet).orEmpty()
            }.transformImports()
        else -> null
    }
}

private class ListStorageFirScope(val result: List<FirScope>) : FirScope() {
    /*
     * such a scope should not be accessible from call-sites
     */
    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirScope? {
        shouldNotBeCalled()
    }
}
