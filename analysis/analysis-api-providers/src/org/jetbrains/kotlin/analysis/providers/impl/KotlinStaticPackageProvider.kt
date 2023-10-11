/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderMerger
import org.jetbrains.kotlin.analysis.providers.createPackageProvider
import org.jetbrains.kotlin.analysis.providers.impl.packageProviders.CompositeKotlinPackageProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

public class KotlinStaticPackageProvider(
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

public class KotlinStaticPackageProviderFactory(
    private val project: Project,
    private val files: Collection<KtFile>
) : KotlinPackageProviderFactory() {
    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        return KotlinStaticPackageProvider(project, searchScope, files)
    }
}

public class KotlinStaticPackageProviderMerger(private val project: Project) : KotlinPackageProviderMerger() {
    override fun merge(providers: List<KotlinPackageProvider>): KotlinPackageProvider =
        providers.mergeSpecificProviders<_, KotlinStaticPackageProvider>(CompositeKotlinPackageProvider.factory) { targetProviders ->
            val combinedScope = GlobalSearchScope.union(targetProviders.map { it.scope })
            project.createPackageProvider(combinedScope).apply {
                check(this is KotlinStaticPackageProvider) {
                    "`${KotlinStaticPackageProvider::class.simpleName}` can only be merged into a combined package provider of the same type."
                }
            }
        }
}
