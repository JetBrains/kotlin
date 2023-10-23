/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.util

import org.jetbrains.kotlin.analysis.providers.KotlinDeclarationProvider
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDelegatingCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirCachedSymbolNamesProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.filterToSetOrEmpty

/**
 * A [FirSymbolNamesProvider] that fetches top-level names from a Kotlin [declarationProvider].
 *
 * @param allowKotlinPackage Whether the associated symbol provider is allowed to provide symbols from the `kotlin` package.
 */
internal open class LLFirKotlinSymbolNamesProvider(
    private val declarationProvider: KotlinDeclarationProvider,
    private val allowKotlinPackage: Boolean? = null,
) : FirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String>? = declarationProvider.computePackageNames()?.excludeKotlinPackageNamesIfNecessary()

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificClassifierPackageNamesComputation

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        declarationProvider
            .computePackageNamesWithTopLevelClassifiers()
            ?.excludeKotlinPackageNamesIfNecessary()

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> {
        if (allowKotlinPackage == false && packageFqName.isKotlinPackage()) return emptySet()

        return declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
    }

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificCallablePackageNamesComputation

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        declarationProvider
            .computePackageNamesWithTopLevelCallables()
            ?.excludeKotlinPackageNamesIfNecessary()

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        if (allowKotlinPackage == false && packageFqName.isKotlinPackage()) return emptySet()

        return declarationProvider.getTopLevelCallableNamesInPackage(packageFqName).ifEmpty { emptySet() }
    }

    private fun Set<String>.excludeKotlinPackageNamesIfNecessary(): Set<String> {
        if (allowKotlinPackage == false && any { it.isKotlinPackage() }) {
            return filterToSetOrEmpty { !it.isKotlinPackage() }
        }
        return this
    }

    companion object {
        fun cached(session: FirSession, declarationProvider: KotlinDeclarationProvider): FirCachedSymbolNamesProvider =
            FirDelegatingCachedSymbolNamesProvider(session, LLFirKotlinSymbolNamesProvider(declarationProvider))
    }
}

private fun FqName.isKotlinPackage(): Boolean = startsWith(StandardNames.BUILT_INS_PACKAGE_NAME)
private fun String.isKotlinPackage(): Boolean = startsWith(KOTLIN_PACKAGE_PREFIX)

private const val KOTLIN_PACKAGE_PREFIX = "kotlin."

