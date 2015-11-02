/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.name.FqName
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.name.Name

public object PackageIndexUtil {
    @JvmStatic
    public fun getSubPackageFqNames(
            packageFqName: FqName,
            scope: GlobalSearchScope,
            project: Project,
            nameFilter: (Name) -> Boolean
    ): Collection<FqName> {
        return SubpackagesIndexService.getInstance(project).getSubpackages(packageFqName, scope, nameFilter)
    }

    @JvmStatic
    public fun findFilesWithExactPackage(
            packageFqName: FqName,
            searchScope: GlobalSearchScope,
            project: Project
    ): Collection<KtFile> {
        return KotlinExactPackagesIndex.getInstance().get(packageFqName.asString(), project, searchScope)
    }

    @JvmStatic
    public fun packageExists(
            packageFqName: FqName,
            searchScope: GlobalSearchScope,
            project: Project
    ): Boolean {
        return containsFilesWithExactPackage(packageFqName, searchScope, project) ||
               SubpackagesIndexService.getInstance(project).hasSubpackages(packageFqName, searchScope)
    }

    @JvmStatic
    public fun containsFilesWithExactPackage(
            packageFqName: FqName,
            searchScope: GlobalSearchScope,
            project: Project
    ): Boolean {
        var result = false
        StubIndex.getInstance().processElements<String, KtFile>(
                KotlinExactPackagesIndex.getInstance().getKey(), packageFqName.asString(), project, searchScope, javaClass<KtFile>()
        ) {
            result = true
            false
        }
        return result
    }

    @JvmStatic
    public fun getAllPossiblePackageClasses(project: Project): MultiMap<String, FqName> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            Result(computeAllPossiblePackageClasses(project), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
        }!!
    }

    private fun computeAllPossiblePackageClasses(project: Project): MultiMap<String, FqName> {
        val packageFqNames = KotlinExactPackagesIndex.getInstance().getAllKeys(project)
        val result = MultiMap<String, FqName>()
        for (packageFqName in packageFqNames) {
            val packageClassFqName = PackageClassUtils.getPackageClassFqName(FqName(packageFqName))
            result.putValue(packageClassFqName.shortName().asString(), packageClassFqName)
        }

        return result
    }
}
