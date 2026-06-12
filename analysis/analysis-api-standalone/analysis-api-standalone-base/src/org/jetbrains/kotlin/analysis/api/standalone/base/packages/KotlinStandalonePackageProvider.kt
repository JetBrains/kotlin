/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.base.packages

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.mergeSpecificProviders
import org.jetbrains.kotlin.analysis.api.platform.packages.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class KotlinStandalonePackageProvider(
    project: Project,
    internal val scope: GlobalSearchScope,
    matchingPackageNames: Set<FqName>
) : KotlinPackageProviderBase(project, scope) {
    private val kotlinPackageToSubPackages: Map<FqName, Set<Name>> = run {
        val packages: MutableMap<FqName, MutableSet<Name>> = mutableMapOf() // the explicit type is here to workaround KTIJ-21172
        for (packageName in matchingPackageNames) {
            var currentPackage = FqName.ROOT
            for (subPackage in packageName.pathSegments()) {
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

    override fun getKotlinOnlySubpackageNames(packageFqName: FqName): Set<Name> {
        return kotlinPackageToSubPackages[packageFqName] ?: emptySet()
    }
}

class KotlinStandalonePackageProviderFactory(
    private val project: Project,
    private val packageNamesProvider: KotlinStandalonePackageNamesProvider,
) : KotlinCachingPackageProviderFactory(project) {
    override fun createNewPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        val matchingPackageNames = packageNamesProvider.getPackageNamesInScope(searchScope) ?: emptySet()
        return KotlinStandalonePackageProvider(project, searchScope, matchingPackageNames)
    }
}

class KotlinStandalonePackageProviderMerger(private val project: Project) : KotlinPackageProviderMerger {
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
