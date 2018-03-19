/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.UnambiguousFqName
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.resolve.FirQualifierResolver
import org.jetbrains.kotlin.fir.scopes.FirImportingScope
import org.jetbrains.kotlin.name.Name

class FirExplicitImportingScope(imports: List<FirImport>) : FirImportingScope {

    // TODO: Resolve imports! Instead of computing it resolution results here
    private val simpleImports =
        imports.filter { !it.isAllUnder && it.aliasName == null && it.importedFqName != null }
            .mapNotNull { it.resolve() }
            .groupBy { it.classFqName.shortName() }

    private fun FirImport.resolve(): UnambiguousFqName? {
        val session = session
        val qualifierResolver = FirQualifierResolver.getInstance(session)
        return qualifierResolver.resolveImport(importedFqName!!)
    }

    override fun processClassifiersByName(name: Name, processor: (UnambiguousFqName) -> Boolean): Boolean {
        val imports = simpleImports[name] ?: return true
        for (import in imports) {
            if (!processor(import)) {
                return false
            }
        }
        return true
    }

}