/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubindex

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.caches.trackers.KotlinCodeBlockModificationListener
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.util.*

class SubpackagesIndexService(private val project: Project) {

    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
        {
            CachedValueProvider.Result(
                SubpackagesIndex(KotlinExactPackagesIndex.getInstance().getAllKeys(project)),
                KotlinCodeBlockModificationListener.getInstance(project).kotlinOutOfCodeBlockTracker
            )
        },
        false
    )

    inner class SubpackagesIndex(allPackageFqNames: Collection<String>) {
        // a map from any existing package (in kotlin) to a set of subpackages (not necessarily direct) containing files
        private val allPackageFqNames = hashSetOf<FqName>()
        private val fqNameByPrefix = MultiMap.createSet<FqName, FqName>()
        private val oocbCount = KotlinCodeBlockModificationListener.getInstance(project).kotlinOutOfCodeBlockTracker.modificationCount

        init {
            for (fqNameAsString in allPackageFqNames) {
                val fqName = FqName(fqNameAsString)
                this.allPackageFqNames.add(fqName)

                var prefix = fqName
                while (!prefix.isRoot) {
                    prefix = prefix.parent()
                    fqNameByPrefix.putValue(prefix, fqName)
                }
            }
        }

        fun hasSubpackages(fqName: FqName, scope: GlobalSearchScope): Boolean {
            return fqNameByPrefix[fqName].any { packageWithFilesFqName ->
                PackageIndexUtil.containsFilesWithExactPackage(packageWithFilesFqName, scope, project)
            }
        }

        fun packageExists(fqName: FqName): Boolean = fqName in allPackageFqNames || fqNameByPrefix.containsKey(fqName)

        fun getSubpackages(fqName: FqName, scope: GlobalSearchScope, nameFilter: (Name) -> Boolean): Collection<FqName> {
            val possibleFilesFqNames = fqNameByPrefix[fqName]
            val existingSubPackagesShortNames = HashSet<Name>()
            val len = fqName.pathSegments().size
            for (filesFqName in possibleFilesFqNames) {
                val candidateSubPackageShortName = filesFqName.pathSegments()[len]
                if (candidateSubPackageShortName in existingSubPackagesShortNames || !nameFilter(candidateSubPackageShortName)) continue

                val existsInThisScope = PackageIndexUtil.containsFilesWithExactPackage(filesFqName, scope, project)
                if (existsInThisScope) {
                    existingSubPackagesShortNames.add(candidateSubPackageShortName)
                }
            }

            return existingSubPackagesShortNames.map { fqName.child(it) }
        }

        override fun toString() = "SubpackagesIndex: OOCB on creation $oocbCount, all packages size ${allPackageFqNames.size}"
    }

    companion object {
        fun getInstance(project: Project): SubpackagesIndex {
            return ServiceManager.getService(project, SubpackagesIndexService::class.java)!!.cachedValue.value!!
        }
    }
}
