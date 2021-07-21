/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.test.base

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.fir.low.level.api.KtPackageProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

internal class KtPackageProviderTestImpl(
    scope: GlobalSearchScope,
    files: Collection<KtFile>
) : KtPackageProvider() {

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

    override fun isPackageExists(packageFqName: FqName): Boolean {
        return packageFqName in packageToSubPackageNames
    }

    override fun getKotlinSubPackageFqNames(packageFqName: FqName): Set<Name> {
        return packageToSubPackageNames[packageFqName] ?: emptySet()
    }
}