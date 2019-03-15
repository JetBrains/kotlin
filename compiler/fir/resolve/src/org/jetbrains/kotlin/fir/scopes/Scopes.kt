/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.scopes.impl.*

fun FirCompositeScope.addImportingScopes(file: FirFile, session: FirSession) {
    scopes += listOf(
        // from low priority to high priority
        FirDefaultStarImportingScope(session),
        FirExplicitStarImportingScope(file.imports, session),
        FirDefaultSimpleImportingScope(session),
        FirSelfImportingScope(file.packageFqName, session),
        // TODO: explicit simple importing scope should have highest priority (higher than inner scopes added in process)
        FirExplicitSimpleImportingScope(file.imports, session)
    )
}
