/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirDependenciesSymbolProviderImpl
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

abstract class FirAbstractStarImportingScope(val session: FirSession, lookupInFir: Boolean = true) : FirScope {

    protected abstract val starImports: List<FirResolvedImport>

    val provider = FirSymbolProvider.getInstance(session).let {
        when {
            it is FirCompositeSymbolProvider && !lookupInFir -> it.providers.find { it is FirDependenciesSymbolProviderImpl }!!
            else -> it
        }
    }

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeSymbol) -> Boolean
    ): Boolean {
        for (import in starImports) {
            val relativeClassName = import.relativeClassName
            val classId = if (relativeClassName == null) {
                ClassId(import.packageFqName, name)
            } else {
                ClassId(import.packageFqName, relativeClassName.child(name), false)
            }
            val symbol = provider.getSymbolByFqName(classId) ?: continue
            if (!processor(symbol)) {
                return false
            }
        }
        return true
    }

}