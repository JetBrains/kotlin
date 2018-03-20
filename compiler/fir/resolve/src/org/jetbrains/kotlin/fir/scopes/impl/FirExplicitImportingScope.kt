/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport
import org.jetbrains.kotlin.fir.scopes.FirImportingScope
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirExplicitImportingScope(imports: List<FirImport>) : FirImportingScope {

    // TODO: Resolve imports! Instead of computing it resolution results here
    private val simpleImports =
        imports.filterIsInstance<FirResolvedImport>()
            .filter { !it.isAllUnder }
            .groupBy { it.aliasName ?: it.resolvedFqName.shortClassName }

    override fun processClassifiersByName(name: Name, processor: (ClassId) -> Boolean): Boolean {
        val imports = simpleImports[name] ?: return true
        for (import in imports) {
            if (!processor(import.resolvedFqName)) {
                return false
            }
        }
        return true
    }

}