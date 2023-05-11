/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDelegatingCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirCachedSymbolNamesProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * A [FirSymbolNamesProvider] that fetches top-level names from a Kotlin [declarationProvider].
 */
internal open class LLFirKotlinSymbolNamesProvider(
    private val declarationProvider: KotlinDeclarationProvider,
) : FirSymbolNamesProvider() {
    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<String> =
        declarationProvider
            .getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
            .mapTo(mutableSetOf()) { it.asString() }

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        declarationProvider.computePackageSetWithTopLevelCallableDeclarations()

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
        declarationProvider.getTopLevelCallableNamesInPackage(packageFqName)

    companion object {
        fun cached(session: FirSession, declarationProvider: KotlinDeclarationProvider): FirCachedSymbolNamesProvider =
            FirDelegatingCachedSymbolNamesProvider(session, LLFirKotlinSymbolNamesProvider(declarationProvider))
    }
}
