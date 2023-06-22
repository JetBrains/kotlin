/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.FqName

private val ALL_IMPORTS = scopeSessionKey<FirFile, ListStorageFirScope>()
private val DEFAULT_STAR_IMPORT = scopeSessionKey<DefaultStarImportKey, FirDefaultStarImportingScope>()
private val DEFAULT_SIMPLE_IMPORT = scopeSessionKey<DefaultImportPriority, FirDefaultSimpleImportingScope>()

private data class DefaultStarImportKey(val priority: DefaultImportPriority, val excludedImportNames: Set<FqName>)

fun createImportingScopes(
    file: FirFile,
    session: FirSession,
    scopeSession: ScopeSession,
    useCaching: Boolean = true
): List<FirScope> = if (useCaching) {
    scopeSession.getOrBuild(file, ALL_IMPORTS) {
        ListStorageFirScope(doCreateImportingScopes(file, session, scopeSession))
    }.result
} else {
    doCreateImportingScopes(file, session, scopeSession)
}

private fun doCreateImportingScopes(
    file: FirFile,
    session: FirSession,
    scopeSession: ScopeSession
): List<FirScope> {
    file.lazyResolveToPhase(FirResolvePhase.IMPORTS)
    val excludedImportNames =
        file.imports.filter { it.aliasName != null }.mapNotNullTo(hashSetOf()) { it.importedFqName }.ifEmpty { emptySet() }
    return listOf(
        scopeSession.getOrBuild(DefaultStarImportKey(DefaultImportPriority.LOW, excludedImportNames), DEFAULT_STAR_IMPORT) {
            FirDefaultStarImportingScope(session, scopeSession, DefaultImportPriority.LOW, excludedImportNames)
        },
        scopeSession.getOrBuild(DefaultStarImportKey(DefaultImportPriority.HIGH, excludedImportNames), DEFAULT_STAR_IMPORT) {
            FirDefaultStarImportingScope(session, scopeSession, DefaultImportPriority.HIGH, excludedImportNames)
        },
        scopeSession.getOrBuild(DefaultImportPriority.KOTLIN_THROWS, DEFAULT_SIMPLE_IMPORT) {
            FirDefaultSimpleImportingScope(session, scopeSession, priority = DefaultImportPriority.KOTLIN_THROWS)
        },
        FirExplicitStarImportingScope(file.imports, session, scopeSession, excludedImportNames),

        scopeSession.getOrBuild(DefaultImportPriority.LOW, DEFAULT_SIMPLE_IMPORT) {
            FirDefaultSimpleImportingScope(session, scopeSession, priority = DefaultImportPriority.LOW)
        },
        scopeSession.getOrBuild(DefaultImportPriority.HIGH, DEFAULT_SIMPLE_IMPORT) {
            FirDefaultSimpleImportingScope(session, scopeSession, priority = DefaultImportPriority.HIGH)
        },
        scopeSession.getOrBuild(file.packageFqName, PACKAGE_MEMBER) {
            FirPackageMemberScope(file.packageFqName, session, excludedImportNames = excludedImportNames)
        },
        // TODO: explicit simple importing scope should have highest priority (higher than inner scopes added in process)
        FirExplicitSimpleImportingScope(file.imports, session, scopeSession)
    )
}

private class ListStorageFirScope(val result: List<FirScope>) : FirScope()
