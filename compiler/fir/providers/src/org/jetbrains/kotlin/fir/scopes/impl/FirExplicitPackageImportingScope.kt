/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.DelicateScopeAPI
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.plus

class FirExplicitPackageImportingScope private constructor(
    val packageImports: Map<Name, List<FqName>>
) : FirScope() {
    constructor(imports: List<FirImport>) : this(
        packageImports = imports.filterIsInstance<FirResolvedImport>()
            .filter { it.isPackage && it.importedName != null }
            .groupBy { it.aliasName ?: it.packageFqName.pathSegments().last() }
            .mapValues { (_, imports) -> imports.map { it.packageFqName } }
    )

    @DelicateScopeAPI
    override fun withReplacedSessionOrNull(newSession: FirSession, newScopeSession: ScopeSession): FirExplicitPackageImportingScope {
        return FirExplicitPackageImportingScope(packageImports)
    }
}

val List<FirExplicitPackageImportingScope>.asPackageImportMap: Map<Name, List<FqName>>
    get() = buildMap<Name, List<FqName>> {
        for (packageImport in this@asPackageImportMap) {
            for ((name, fqNames) in packageImport.packageImports) {
                put(name, fqNames + get(name).orEmpty())
            }
        }
    }
