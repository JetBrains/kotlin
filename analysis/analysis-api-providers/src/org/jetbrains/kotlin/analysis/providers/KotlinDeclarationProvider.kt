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
 * A declaration provider for a given scope. Can be created via [KotlinDeclarationProviderFactory].
 * May be called frequently, so for implementations it is better to cache results.
 */
public abstract class KotlinDeclarationProvider {
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

    public abstract fun computePackageSetWithTopLevelCallableDeclarations(): Set<String>
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
public abstract class KotlinDeclarationProviderMerger {
    /**
     * Merges [declarationProviders] if possible, creating a combined declaration provider that should be more efficient compared to calling
     * separate declaration providers. Not all given declaration providers might be mergeable, or there might be multiple separate sets of
     * declaration providers which can be merged individually, so the resulting declaration provider may be a composite Kotlin declaration
     * provider.
     */
    public abstract fun mergeDeclarationProviders(declarationProviders: List<KotlinDeclarationProvider>): KotlinDeclarationProvider

    public companion object {
        public fun getInstance(project: Project): KotlinDeclarationProviderMerger =
            project.getService(KotlinDeclarationProviderMerger::class.java)
    }
}

public fun Project.createDeclarationProvider(scope: GlobalSearchScope, module: KtModule?): KotlinDeclarationProvider =
    KotlinDeclarationProviderFactory.getInstance(this).createDeclarationProvider(scope, module)

public fun Project.mergeDeclarationProviders(declarationProviders: List<KotlinDeclarationProvider>): KotlinDeclarationProvider =
    KotlinDeclarationProviderMerger.getInstance(this).mergeDeclarationProviders(declarationProviders)
