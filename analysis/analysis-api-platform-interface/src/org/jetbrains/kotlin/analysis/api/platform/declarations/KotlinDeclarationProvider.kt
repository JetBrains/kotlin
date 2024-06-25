/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.platform.declarations

import org.jetbrains.kotlin.analysis.api.platform.KotlinComposableProvider
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * A [KotlinDeclarationProvider] provides Kotlin PSI declarations for the project as well as additional information about them. The
 * declarations may come from sources, libraries in case of stubbing, or declaration generation.
 *
 * Declaration providers usually don't cover declarations of the whole project, but rather of a restricted context. This usually means being
 * limited to a specific [scope][com.intellij.psi.search.GlobalSearchScope], but the details depend on the kind of declaration provider.
 *
 * The main kind of declaration providers is created via [KotlinDeclarationProviderFactory] and it is scope-based, but there are other kinds
 * of declaration providers, such as those created by [KotlinForwardDeclarationProviderFactory].
 *
 * Declaration providers are critical for performance, so implementations should cache results.
 */
public interface KotlinDeclarationProvider : KotlinComposableProvider {
    public fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration?

    public fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject>
    public fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias>

    public fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name>

    public fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty>
    public fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction>

    public fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile>

    public fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>

    public fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile>

    public fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile>

    /**
     * Currently we want only classes from libraries ([org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile])
     */
    public fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile>

    public fun findFilesForScript(scriptFqName: FqName): Collection<KtScript>

    /**
     * Calculates the set of package names which can be provided by this declaration provider.
     *
     * The set may contain false positives. `null` may be returned if the package set is too expensive or impossible to compute.
     *
     * [computePackageNames] is used as the default implementation for [computePackageNamesWithTopLevelClassifiers] and
     * [computePackageNamesWithTopLevelCallables] if either returns `null`. It depends on the declaration provider whether it's worth
     * computing separate package sets for classifiers and callables, or just one set containing all package names.
     */
    public fun computePackageNames(): Set<String>? = null

    /**
     * Whether the declaration provider has a specific implementation of [computePackageNamesWithTopLevelClassifiers]. This allows the
     * Analysis API backend to determine whether classifier package sets are computed and cached separately or with [computePackageNames].
     */
    public val hasSpecificClassifierPackageNamesComputation: Boolean

    /**
     * Calculates the set of package names which contain classifiers and can be provided by this declaration provider.
     *
     * The set may contain false positives. `null` may be returned if the package set is too expensive or impossible to compute.
     */
    public fun computePackageNamesWithTopLevelClassifiers(): Set<String>? = computePackageNames()

    /**
     * Whether the declaration provider has a specific implementation of [computePackageNamesWithTopLevelCallables]. This allows the
     * Analysis API backend to determine whether callable package sets are computed and cached separately or with [computePackageNames].
     */
    public val hasSpecificCallablePackageNamesComputation: Boolean

    /**
     * Calculates the set of package names which contain callables and can be provided by this declaration provider.
     *
     * The set may contain false positives. `null` may be returned if the package set is too expensive or impossible to compute.
     */
    public fun computePackageNamesWithTopLevelCallables(): Set<String>? = computePackageNames()
}
