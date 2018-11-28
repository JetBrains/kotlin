/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirResolvedImport

class FirExplicitStarImportingScope(imports: List<FirImport>, session: FirSession) : FirAbstractStarImportingScope(session) {
    override val starImports = imports.filterIsInstance<FirResolvedImport>().filter { it.isAllUnder }
}