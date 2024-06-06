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
 * A declaration provider for a given scope. It can be created via [KotlinDeclarationProviderFactory].
 *
 * It may be called frequently, so implementations should cache the results.
 */
public abstract class KotlinDeclarationProvider : KotlinComposableProvider {
    public abstract fun getClassLikeDeclarationByClassId(classId: ClassId): KtClassLikeDeclaration?

    public abstract fun getAllClassesByClassId(classId: ClassId): Collection<KtClassOrObject>
    public abstract fun getAllTypeAliasesByClassId(classId: ClassId): Collection<KtTypeAlias>

    public abstract fun getTopLevelKotlinClassLikeDeclarationNamesInPackage(packageFqName: FqName): Set<Name>

    public abstract fun getTopLevelProperties(callableId: CallableId): Collection<KtProperty>
    public abstract fun getTopLevelFunctions(callableId: CallableId): Collection<KtNamedFunction>

    public abstract fun getTopLevelCallableFiles(callableId: CallableId): Collection<KtFile>

    public abstract fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>

    public abstract fun findFilesForFacadeByPackage(packageFqName: FqName): Collection<KtFile>

    public abstract fun findFilesForFacade(facadeFqName: FqName): Collection<KtFile>

    /**
     * Currently we want only classes from libraries ([org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile])
     */
    public abstract fun findInternalFilesForFacade(facadeFqName: FqName): Collection<KtFile>

    public abstract fun findFilesForScript(scriptFqName: FqName): Collection<KtScript>

    /**
     * Calculates the set of package names which can be provided by this declaration provider.
     *
     * The set may contain false positives. `null` may be returned if the package set is too expensive or impossible to compute.
     *
     * [computePackageNames] is used as the default implementation for [computePackageNamesWithTopLevelClassifiers] and
     * [computePackageNamesWithTopLevelCallables] if either returns `null`. It depends on the declaration provider whether it's worth
     * computing separate package sets for classifiers and callables, or just one set containing all package names.
     */
    public open fun computePackageNames(): Set<String>? = null

    /**
     * Whether the declaration provider has a specific implementation of [computePackageNamesWithTopLevelClassifiers]. This allows the
     * Analysis API backend to determine whether classifier package sets are computed and cached separately or with [computePackageNames].
     */
    public abstract val hasSpecificClassifierPackageNamesComputation: Boolean

    /**
     * Calculates the set of package names which contain classifiers and can be provided by this declaration provider.
     *
     * The set may contain false positives. `null` may be returned if the package set is too expensive or impossible to compute.
     */
    public open fun computePackageNamesWithTopLevelClassifiers(): Set<String>? = computePackageNames()

    /**
     * Whether the declaration provider has a specific implementation of [computePackageNamesWithTopLevelCallables]. This allows the
     * Analysis API backend to determine whether callable package sets are computed and cached separately or with [computePackageNames].
     */
    public abstract val hasSpecificCallablePackageNamesComputation: Boolean

    /**
     * Calculates the set of package names which contain callables and can be provided by this declaration provider.
     *
     * The set may contain false positives. `null` may be returned if the package set is too expensive or impossible to compute.
     */
    public open fun computePackageNamesWithTopLevelCallables(): Set<String>? = computePackageNames()
}
