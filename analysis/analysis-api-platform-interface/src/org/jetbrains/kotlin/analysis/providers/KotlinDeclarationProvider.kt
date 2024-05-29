/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.project.structure.KtModule
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

public abstract class KotlinDeclarationProviderFactory {
    public abstract fun createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KtModule?): KotlinDeclarationProvider

    public companion object {
        public fun getInstance(project: Project): KotlinDeclarationProviderFactory =
            project.getService(KotlinDeclarationProviderFactory::class.java)
    }
}

/**
 * While the main declaration provider created via [createDeclarationProvider] is scope-based, there are other declaration providers which
 * are not, such as file-based declaration providers. Not all declarations provided by such declaration providers can be provided by the
 * main declaration provider, even if the correct scope is provided (such as a file-based scope). For example, the main declaration provider
 * may be based on an index which doesn't contain the declarations provided by file-based declaration providers.
 *
 * Hence, [KotlinDeclarationProvider]s cannot just be combined by combining the scopes of all declaration providers and calling
 * [createDeclarationProvider]. [KotlinDeclarationProviderMerger] should implement proper merging logic that takes these concerns into
 * account.
 */
public abstract class KotlinDeclarationProviderMerger : KotlinComposableProviderMerger<KotlinDeclarationProvider> {
    public companion object {
        public fun getInstance(project: Project): KotlinDeclarationProviderMerger =
            project.getService(KotlinDeclarationProviderMerger::class.java)
    }
}

/**
 * Creates a [KotlinDeclarationProvider] providing symbols within the given [scope].
 *
 * The [contextualModule] is the module which contains the symbols to be provided, if applicable. The declaration provider may use the
 * contextual module to provide declarations differently, such as providing alternative declarations for an outsider module. Some
 * functionality such as package set computation may also depend on the contextual module, as the declaration provider may require
 * additional information not available in the [scope].
 */
public fun Project.createDeclarationProvider(scope: GlobalSearchScope, contextualModule: KtModule?): KotlinDeclarationProvider =
    KotlinDeclarationProviderFactory.getInstance(this).createDeclarationProvider(scope, contextualModule)

public fun Project.mergeDeclarationProviders(declarationProviders: List<KotlinDeclarationProvider>): KotlinDeclarationProvider =
    KotlinDeclarationProviderMerger.getInstance(this).merge(declarationProviders)
