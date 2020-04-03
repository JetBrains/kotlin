/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.ScopeSessionKey
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.name.FqName

fun MutableList<FirScope>.addImportingScopes(file: FirFile, session: FirSession, scopeSession: ScopeSession) {
    this += createImportingScopes(file, session, scopeSession)
}

private object FirDefaultStarImportingScopeKey : ScopeSessionKey<DefaultImportPriority, FirScope>()
private object FirDefaultSimpleImportingScopeKey : ScopeSessionKey<DefaultImportPriority, FirScope>()
private object FileImportingScopeKey : ScopeSessionKey<FirFile, ListStorageFirScope>()

private class ListStorageFirScope(val result: List<FirScope>) : FirScope()

fun createImportingScopes(
    file: FirFile,
    session: FirSession,
    scopeSession: ScopeSession
): List<FirScope> = scopeSession.getOrBuild(file, FileImportingScopeKey) {
    ListStorageFirScope(doCreateImportingScopes(file, session, scopeSession))
}.result

private fun doCreateImportingScopes(
    file: FirFile,
    session: FirSession,
    scopeSession: ScopeSession
): List<FirScope> {
    return listOf(
        // from low priority to high priority
        scopeSession.getOrBuild(DefaultImportPriority.LOW, FirDefaultStarImportingScopeKey) {
            FirDefaultStarImportingScope(session, scopeSession, priority = DefaultImportPriority.LOW)
        },
        scopeSession.getOrBuild(DefaultImportPriority.HIGH, FirDefaultStarImportingScopeKey) {
            FirDefaultStarImportingScope(session, scopeSession, priority = DefaultImportPriority.HIGH)
        },
        FirExplicitStarImportingScope(file.imports, session, scopeSession),
        scopeSession.getOrBuild(DefaultImportPriority.LOW, FirDefaultSimpleImportingScopeKey) {
            FirDefaultSimpleImportingScope(session, scopeSession, priority = DefaultImportPriority.LOW)
        },
        scopeSession.getOrBuild(DefaultImportPriority.HIGH, FirDefaultSimpleImportingScopeKey) {
            FirDefaultSimpleImportingScope(session, scopeSession, priority = DefaultImportPriority.HIGH)
        },
        scopeSession.getOrBuild(file.packageFqName, PACKAGE_MEMBER) {
            FirPackageMemberScope(file.packageFqName, session)
        },
        // TODO: explicit simple importing scope should have highest priority (higher than inner scopes added in process)
        FirExplicitSimpleImportingScope(file.imports, session, scopeSession)
    )
}

private val PACKAGE_MEMBER = scopeSessionKey<FqName, FirPackageMemberScope>()

