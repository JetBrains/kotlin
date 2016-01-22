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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.containers.MultiMap
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.OrderEntry
import com.intellij.openapi.roots.ModuleOrderEntry
import com.intellij.openapi.util.Condition
import java.util.LinkedHashSet
import com.intellij.openapi.roots.RootPolicy
import org.jetbrains.kotlin.utils.addIfNotNull
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.JdkOrderEntry
import com.intellij.openapi.roots.ModuleSourceOrderEntry

class LibraryDependenciesCache(private val project: Project) {

    //NOTE: used LibraryRuntimeClasspathScope as reference
    fun getLibrariesAndSdksUsedWith(library: Library): Pair<List<Library>, List<Sdk>> {
        val processedModules = LinkedHashSet<Module>()
        val condition = Condition<OrderEntry>() { orderEntry ->
            if (orderEntry is ModuleOrderEntry) {
                val module = orderEntry.module
                module != null && module !in processedModules
            }
            else {
                true
            }
        }

        val libraries = LinkedHashSet<Library>()
        val sdks = LinkedHashSet<Sdk>()

        fun collectLibrariesAndSdksAcrossDependencies(module: Module) {
            if (!processedModules.add(module)) return

            ModuleRootManager.getInstance(module).orderEntries().recursively().satisfying(condition).process(object : RootPolicy<Unit>() {
                override fun visitModuleSourceOrderEntry(moduleSourceOrderEntry: ModuleSourceOrderEntry?, value: Unit?): Unit? {
                    processedModules.addIfNotNull(moduleSourceOrderEntry?.ownerModule)
                    return Unit
                }

                override fun visitLibraryOrderEntry(libraryOrderEntry: LibraryOrderEntry?, value: Unit?): Unit? {
                    libraries.addIfNotNull(libraryOrderEntry?.library)
                    return Unit
                }

                override fun visitJdkOrderEntry(jdkOrderEntry: JdkOrderEntry?, value: Unit?): Unit? {
                    sdks.addIfNotNull(jdkOrderEntry?.jdk)
                    return Unit
                }
            }, Unit)
        }

        getLibraryUsageIndex().modulesLibraryIsUsedIn[library].forEach { module -> collectLibrariesAndSdksAcrossDependencies(module) }

        return Pair(libraries.toList(), sdks.toList())
    }

    private fun getLibraryUsageIndex(): LibraryUsageIndex {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result(LibraryUsageIndex(), ProjectRootModificationTracker.getInstance(project))
        }!!
    }

    private inner class LibraryUsageIndex {
        val modulesLibraryIsUsedIn: MultiMap<Library, Module> = MultiMap.createSet()

        init {
            ModuleManager.getInstance(project).modules.forEach {
                module ->
                ModuleRootManager.getInstance(module).orderEntries.forEach {
                    entry ->
                    if (entry is LibraryOrderEntry) {
                        val library = entry.library
                        if (library != null) {
                            modulesLibraryIsUsedIn.putValue(library, module)
                        }
                    }
                }
            }
        }
    }
}
