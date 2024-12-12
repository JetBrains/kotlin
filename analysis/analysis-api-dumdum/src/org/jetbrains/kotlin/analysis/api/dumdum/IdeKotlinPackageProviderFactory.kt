// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndex
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.platform.packages.*
import org.jetbrains.kotlin.analysis.api.platform.projectStructure.KotlinGlobalSearchScopeMerger
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.concurrent.ConcurrentHashMap

internal class IdeKotlinPackageProviderFactory(
    private val project: Project,
    private val fileBasedIndex: FileBasedIndex,
) : KotlinPackageProviderFactory {
    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        return IdeKotlinPackageProvider(project, searchScope, fileBasedIndex)
    }
}

internal class IdeKotlinPackageProviderMerger(
    private val project: Project,
    private val fileBasedIndex: FileBasedIndex,
) : KotlinPackageProviderMerger {
    override fun merge(providers: List<KotlinPackageProvider>): KotlinPackageProvider =
        providers.mergeSpecificProviders<_, IdeKotlinPackageProvider>(KotlinCompositePackageProvider.factory) { targetProviders ->
            IdeKotlinPackageProvider(
                project,
                KotlinGlobalSearchScopeMerger.getInstance(project).union(targetProviders.map { it.searchScope }),
                fileBasedIndex,
            )
        }
}

private class IdeKotlinPackageProvider(
    project: Project,
    searchScope: GlobalSearchScope,
    val fileBasedIndex: FileBasedIndex,
) : KotlinPackageProviderBase(project, searchScope) {
    private val cache = //by CachedValue(project) {
//        CachedValueProvider.Result(
        ConcurrentHashMap<FqName, Boolean>()//,
//            project.createProjectWideOutOfBlockModificationTracker()
//        )
//    }

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return cache.getOrPut(packageFqName) { KotlinPackageIndexUtils.packageExists(fileBasedIndex, packageFqName, searchScope) }
    }

    override fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name> =
        KotlinPackageIndexUtils
            .getSubpackages(fileBasedIndex, packageFqName, searchScope, nameFilter)
            .mapTo(mutableSetOf()) { it.shortName() }

}