/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedImportImpl

class FirExplicitSimpleImportingScope(
    imports: List<FirImport>,
    session: FirSession
) : FirAbstractSimpleImportingScope(session) {

    override val simpleImports =
        imports.filterIsInstance<FirResolvedImportImpl>()
            .filter { !it.isAllUnder }
            .groupBy { it.aliasName ?: it.resolvedFqName.shortClassName }
}