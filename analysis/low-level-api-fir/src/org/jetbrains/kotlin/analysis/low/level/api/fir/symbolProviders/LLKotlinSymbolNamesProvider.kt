/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.symbolProviders

import org.jetbrains.kotlin.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.providers.FirCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirDelegatingCachedSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.name.ClassId
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
        if (isExcludedKotlinPackage(packageFqName)) return emptySet()

        return declarationProvider.getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName)
    }

    private val nativeClassLikeDeclarationCheck: ((classId: ClassId) -> Boolean)? =
        declarationProvider.mayHaveTopLevelKotlinClassLikeDeclaration

    override val hasSpecificTopLevelClassifierCheck: Boolean
        get() = nativeClassLikeDeclarationCheck != null

    override fun mayHaveTopLevelClassifier(classId: ClassId): Boolean {
        val nativeCheck = nativeClassLikeDeclarationCheck ?: return super.mayHaveTopLevelClassifier(classId)

        val topLevelClassId = if (classId.isNestedClass) classId.outermostClassId else classId

        // Symbol providers can potentially provide symbols for special names. Hence, special names have to be allowed.
        if (topLevelClassId.shortClassName.isSpecial) return true

        return !isExcludedKotlinPackage(topLevelClassId.packageFqName) && nativeCheck(topLevelClassId)
    }

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = declarationProvider.hasSpecificCallablePackageNamesComputation

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        declarationProvider
            .computePackageNamesWithTopLevelCallables()
            ?.excludeKotlinPackageNamesIfNecessary()

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> {
        if (isExcludedKotlinPackage(packageFqName)) return emptySet()

        return declarationProvider.getTopLevelCallableNamesInPackage(packageFqName).ifEmpty { emptySet() }
    }

    private val nativeCallableCheck: ((packageFqName: FqName, name: Name) -> Boolean)? =
        declarationProvider.mayHaveTopLevelCallable

    override val hasSpecificTopLevelCallableCheck: Boolean
        get() = nativeCallableCheck != null

    override fun mayHaveTopLevelCallable(packageFqName: FqName, name: Name): Boolean {
        val nativeCheck = nativeCallableCheck ?: return super.mayHaveTopLevelCallable(packageFqName, name)

        // Symbol providers can potentially provide symbols for special names. Hence, special names have to be allowed.
        if (name.isSpecial) return true

        return !isExcludedKotlinPackage(packageFqName) && nativeCheck(packageFqName, name)
    }

    private fun isExcludedKotlinPackage(packageFqName: FqName): Boolean =
        allowKotlinPackage == false && packageFqName.isKotlinPackage()

    private fun Set<String>.excludeKotlinPackageNamesIfNecessary(): Set<String> {
        if (allowKotlinPackage == false && any { it.isKotlinPackage() }) {
            return filterToSetOrEmpty { !it.isKotlinPackage() }
        }
        return this
    }

    companion object {
        fun cached(
            session: FirSession,
            declarationProvider: KotlinDeclarationProvider,
            allowKotlinPackage: Boolean? = null,
        ): FirCachedSymbolNamesProvider =
            FirDelegatingCachedSymbolNamesProvider(session, LLFirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage))
    }
}
