/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.packages

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderMerger
import org.jetbrains.kotlin.analysis.api.platform.packages.createPackageProvider
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackageProviderBase
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinCompositePackageProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

class KotlinStandalonePackageProvider(
    project: Project,
    internal val scope: GlobalSearchScope,
    files: Collection<KtFile>
) : KotlinPackageProviderBase(project, scope) {
    private val kotlinPackageToSubPackages: Map<FqName, Set<Name>> = run {
        val filesInScope = files.filter { scope.contains(it.virtualFile) }
        val packages: MutableMap<FqName, MutableSet<Name>> = mutableMapOf() // the explicit type is here to workaround KTIJ-21172
        filesInScope.forEach { file ->
            var currentPackage = FqName.ROOT
            for (subPackage in file.packageFqName.pathSegments()) {
                packages.getOrPut(currentPackage) { mutableSetOf() } += subPackage
                currentPackage = currentPackage.child(subPackage)
            }
            packages.computeIfAbsent(currentPackage) { mutableSetOf() }
        }
        packages
    }

    override fun doesKotlinOnlyPackageExist(packageFqName: FqName): Boolean {
        return packageFqName.isRoot || packageFqName in kotlinPackageToSubPackages
    }

    override fun getKotlinOnlySubPackagesFqNames(packageFqName: FqName, nameFilter: (Name) -> Boolean): Set<Name> {
        return kotlinPackageToSubPackages[packageFqName]?.filterTo(mutableSetOf()) { nameFilter(it) } ?: emptySet()
    }
}

class KotlinStandalonePackageProviderFactory(
    private val project: Project,
    private val files: Collection<KtFile>
) : KotlinPackageProviderFactory() {
    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        return KotlinStandalonePackageProvider(project, searchScope, files)
    }
}

class KotlinStandalonePackageProviderMerger(private val project: Project) : KotlinPackageProviderMerger() {
    override fun merge(providers: List<KotlinPackageProvider>): KotlinPackageProvider =
        providers.mergeSpecificProviders<_, KotlinStandalonePackageProvider>(KotlinCompositePackageProvider.factory) { targetProviders ->
            val combinedScope = GlobalSearchScope.union(targetProviders.map { it.scope })
            project.createPackageProvider(combinedScope).apply {
                check(this is KotlinStandalonePackageProvider) {
                    "`${KotlinStandalonePackageProvider::class.simpleName}` can only be merged into a combined package provider of the same type."
                }
            }
        }
}
