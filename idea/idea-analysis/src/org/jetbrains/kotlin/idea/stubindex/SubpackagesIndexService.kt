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
import org.jetbrains.kotlin.name.FqName
import com.intellij.psi.search.GlobalSearchScope
import java.util.HashSet
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.name.Name
import com.intellij.openapi.components.ServiceManager

public class SubpackagesIndexService(private val project: Project) {

    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
            {
                CachedValueProvider.Result(
                        SubpackagesIndex(JetExactPackagesIndex.getInstance().getAllKeys(project)),
                        PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
            },
            false
    )

    public inner class SubpackagesIndex(allPackageFqNames: Collection<String>) {
        // a map from any existing package (in kotlin) to a set of subpackages (not necessarily direct) containing files
        private val fqNameByPrefix = MultiMap.createSet<FqName, FqName>();

        {
            for (fqNameAsString in allPackageFqNames) {
                val fqName = FqName(fqNameAsString)
                var prefix = fqName
                while (!prefix.isRoot()) {
                    prefix = prefix.parent()
                    fqNameByPrefix.putValue(prefix, fqName)
                }
            }
        }

        public fun hasSubpackages(fqName: FqName, scope: GlobalSearchScope): Boolean {
            return fqNameByPrefix[fqName].any { packageWithFilesFqName ->
                PackageIndexUtil.containsFilesWithExactPackage(packageWithFilesFqName, scope, project)
            }
        }

        public fun getSubpackages(fqName: FqName, scope: GlobalSearchScope): Collection<FqName> {
            val possibleFilesFqNames = fqNameByPrefix[fqName]
            val existingSubPackagesShortNames = HashSet<Name>()
            val len = fqName.pathSegments().size()
            for (filesFqName in possibleFilesFqNames) {
                val candidateSubPackageShortName = filesFqName.pathSegments()[len]
                if (candidateSubPackageShortName in existingSubPackagesShortNames) {
                    continue
                }
                val existsInThisScope = PackageIndexUtil.containsFilesWithExactPackage(filesFqName, scope, project)
                if (existsInThisScope) {
                    existingSubPackagesShortNames.add(candidateSubPackageShortName)
                }
            }

            return existingSubPackagesShortNames.map { fqName.child(it) }
        }
    }

    default object {
        public fun getInstance(project: Project): SubpackagesIndex {
            return ServiceManager.getService(project, javaClass<SubpackagesIndexService>())!!.cachedValue.getValue()!!
        }
    }
}
