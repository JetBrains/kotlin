/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFSImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile


object PackageIndexUtil {
    @JvmStatic
    fun getSubPackageFqNames(
        packageFqName: FqName,
        scope: GlobalSearchScope,
        project: Project,
        nameFilter: (Name) -> Boolean
    ): Collection<FqName> {
        return SubpackagesIndexService.getInstance(project).getSubpackages(packageFqName, scope, nameFilter)
    }

    @JvmStatic
    fun findFilesWithExactPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): Collection<KtFile> {
        return KotlinExactPackagesIndex.getInstance().get(packageFqName.asString(), project, searchScope)
    }

    @JvmStatic
    fun packageExists(
        packageFqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): Boolean {

        val subpackagesIndex = SubpackagesIndexService.getInstance(project)
        if (!subpackagesIndex.packageExists(packageFqName)) {
            return false
        }

        return containsFilesWithExactPackage(packageFqName, searchScope, project) ||
                subpackagesIndex.hasSubpackages(packageFqName, searchScope)
    }

    @JvmStatic
    fun containsFilesWithExactPackage(
        packageFqName: FqName,
        searchScope: GlobalSearchScope,
        project: Project
    ): Boolean {
        val ids = StubIndex.getInstance().getContainingIds(
            KotlinExactPackagesIndex.getInstance().key,
            packageFqName.asString(),
            project,
            searchScope
        )
        val fs = PersistentFS.getInstance() as PersistentFSImpl
        while (ids.hasNext()) {
            val file = fs.findFileByIdIfCached(ids.next())
            if (file != null && file in searchScope) {
                return true
            }
        }
        return false
    }
}
