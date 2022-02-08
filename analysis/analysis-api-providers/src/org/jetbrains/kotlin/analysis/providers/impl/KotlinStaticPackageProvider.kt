/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProvider
import org.jetbrains.kotlin.analysis.providers.KotlinPackageProviderFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

public class KotlinStaticPackageProvider(
    scope: GlobalSearchScope,
    files: Collection<KtFile>
) : KotlinPackageProvider() {
    private val packageToSubPackageNames: Map<FqName, Set<Name>> = run {
        val filesInScope = files.filter { scope.contains(it.virtualFile) }
        val packages = mutableMapOf<FqName, MutableSet<Name>>()
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

    override fun doKotlinPackageExists(packageFqName: FqName): Boolean {
        return packageFqName in packageToSubPackageNames
    }

    override fun getKotlinSubPackageFqNames(packageFqName: FqName): Set<Name> {
        return packageToSubPackageNames[packageFqName] ?: emptySet()
    }
}

public class KotlinStaticPackageProviderFactory(private val files: Collection<KtFile>) : KotlinPackageProviderFactory() {
    override fun createPackageProvider(searchScope: GlobalSearchScope): KotlinPackageProvider {
        return KotlinStaticPackageProvider(searchScope, files)
    }
}