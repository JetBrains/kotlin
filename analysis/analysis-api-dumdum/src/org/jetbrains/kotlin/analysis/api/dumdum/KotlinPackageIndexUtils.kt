// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names.KotlinPartialPackageNamesIndex
import org.jetbrains.kotlin.analysis.api.dumdum.index.FileBasedIndex
import org.jetbrains.kotlin.analysis.api.dumdum.index.getValues
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.TopPackageNamesProvider

object KotlinPackageIndexUtils {

    fun getSubPackageFqNames(
        fileBasedIndex: FileBasedIndex,
        packageFqName: FqName,
        scope: GlobalSearchScope,
        nameFilter: (Name) -> Boolean,
    ): Collection<FqName> = getSubpackages(fileBasedIndex, packageFqName, scope, nameFilter)

//    fun findFilesWithExactPackage(
//        stubIndex: StubIndex,
//        packageFqName: FqName,
//        searchScope: GlobalSearchScope,
//        project: Project
//    ): Collection<KtFile> = KotlinExactPackagesIndex.get(stubIndex, packageFqName.asString(), project, searchScope)

    /**
     * Return true if exists package with exact [fqName] OR there are some subpackages of [fqName]
     */
    fun packageExists(fileBasedIndex: FileBasedIndex, fqName: FqName, project: Project): Boolean =
        packageExists(fileBasedIndex, fqName, GlobalSearchScope.allScope(project))

    /**
     * Return true if package [packageFqName] exists or some subpackages of [packageFqName] exist in [searchScope]
     */
    fun packageExists(
        fileBasedIndex: FileBasedIndex,
        packageFqName: FqName,
        searchScope: GlobalSearchScope,
    ): Boolean {
        if (certainlyDoesNotExist(packageFqName, searchScope)) return false
        return !fileBasedIndex.processValues(KotlinPartialPackageNamesIndex.NAME, packageFqName, searchScope) {
            false
        }
    }

    private fun certainlyDoesNotExist(
        packageFqName: FqName,
        searchScope: GlobalSearchScope,
    ): Boolean {
        val provider = searchScope as? TopPackageNamesProvider ?: return false
        val topPackageNames = provider.topPackageNames ?: return false
        val packageFqNameTopLevelPackage = packageFqName.asString().substringBefore(".")
        return packageFqNameTopLevelPackage !in topPackageNames
    }

    /**
     * Return all direct subpackages of package [fqName].
     *
     * I.e. if there are packages `a.b`, `a.b.c`, `a.c`, `a.c.b` for `fqName` = `a` it returns
     * `a.b` and `a.c`
     *
     * Follow the contract of [com.intellij.psi.PsiElementFinder#getSubPackages]
     */
    fun getSubpackages(
        fileBasedIndex: FileBasedIndex,
        fqName: FqName,
        scope: GlobalSearchScope,
        nameFilter: (Name) -> Boolean,
    ): Collection<FqName> {
        if (certainlyDoesNotExist(fqName, scope)) return emptySet()

        val result = hashSetOf<FqName>()

        // use getValues() instead of processValues() because the latter visits each file in the package and that could be slow if there are a lot of files
        val values = fileBasedIndex.getValues(KotlinPartialPackageNamesIndex.NAME, fqName, scope)
        for (subPackageName in values) {
            if (subPackageName != null && nameFilter(subPackageName)) {
                result.add(fqName.child(subPackageName))
            }
        }

        return result
    }
}

