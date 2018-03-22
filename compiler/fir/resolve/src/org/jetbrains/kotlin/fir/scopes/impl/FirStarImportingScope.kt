/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.scopes.FirPosition
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.toSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirStarImportingScope(imports: List<FirImport>) : FirScope {

    private val starImports = imports.filterIsInstance<FirResolvedImport>().filter { it.isAllUnder }

    override fun processClassifiersByName(
        name: Name,
        position: FirPosition,
        processor: (ConeSymbol) -> Boolean
    ): Boolean {
        for (import in starImports) {
            val relativeClassName = import.relativeClassName
            val symbol = if (relativeClassName == null) {
                ClassId(import.packageFqName, name)
            } else {
                ClassId(import.packageFqName, relativeClassName.child(name), false)
            }.toSymbol()
            if (!processor(symbol)) {
                return false
            }
        }
        return true
    }
}