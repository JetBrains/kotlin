/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.impl.FirImportImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedPackageStarImport
import org.jetbrains.kotlin.name.FqName

class FirDefaultStarImportingScope(session: FirSession, lookupInFir: Boolean = false) :
    FirAbstractStarImportingScope(session, lookupInFir) {
    // TODO: move relevant code in TargetPlatform from compiler:frontend and use here
    override val starImports = listOf(
        "kotlin",
        "kotlin.annotation",
        "kotlin.collections",
        "kotlin.ranges",
        "kotlin.sequences",
        "kotlin.text",
        "kotlin.io",
        "java.lang",
        // We should not (probably) import it for non-JVM projects
        "kotlin.jvm"
    ).map {
        val fqName = FqName(it)
        FirResolvedPackageStarImport(
            FirImportImpl(session, null, fqName, isAllUnder = true, aliasName = null),
            fqName
        )
    }
}