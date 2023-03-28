/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * An [LLFirSymbolProviderNameCache] that fetches top-level names from a Kotlin [declarationProvider].
 */
internal class LLFirKotlinSymbolProviderNameCache(
    firSession: FirSession,
    private val declarationProvider: KotlinDeclarationProvider,
) : LLFirSymbolProviderNameCache(firSession) {
    override fun computeClassifierNames(packageFqName: FqName): Set<String> =
        declarationProvider
            .getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
            .mapTo(mutableSetOf()) { it.asString() }

    override fun computeCallableNames(packageFqName: FqName): Set<Name> =
        declarationProvider.getTopLevelCallableNamesInPackage(packageFqName)
}
