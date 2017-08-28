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

import com.intellij.openapi.module.Module
import com.intellij.util.containers.MultiMap
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleOrderEntry
import gnu.trove.THashSet
import com.intellij.util.containers.Queue
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.CachedValueProvider
import com.intellij.openapi.roots.ProjectRootModificationTracker
import java.util.HashSet

//NOTE: this is an approximation that may contain more module infos then the exact solution
fun ModuleSourceInfo.getDependentModules(): Set<ModuleSourceInfo> {
    val dependents = getDependents(module)
    return if (isTests()) {
        dependents.mapNotNullTo(HashSet<ModuleSourceInfo>(), Module::testSourceInfo)
    }
    else {
        dependents.flatMapTo(HashSet<ModuleSourceInfo>()) { it.correspondingModuleInfos() }
    }
}

//NOTE: getDependents adapted from com.intellij.openapi.module.impl.scopes.ModuleWithDependentsScope#buildDependents()
private fun getDependents(module: Module): Set<Module> {
    val result = THashSet<Module>()
    result.add(module)

    val processedExporting = THashSet<Module>()

    val index = getModuleIndex(module.project)

    val walkingQueue = Queue<Module>(10)
    walkingQueue.addLast(module)

    while (!walkingQueue.isEmpty) {
        val current = walkingQueue.pullFirst()
        processedExporting.add(current!!)
        result.addAll(index.plainUsages[current])
        for (dependent in index.exportingUsages[current]) {
            result.add(dependent)
            if (processedExporting.add(dependent)) {
                walkingQueue.addLast(dependent)
            }
        }
    }
    return result
}

private class ModuleIndex {
    val plainUsages = MultiMap.create<Module, Module>()
    val exportingUsages = MultiMap.create<Module, Module>()
}

private fun getModuleIndex(project: Project): ModuleIndex {
    return CachedValuesManager.getManager(project).getCachedValue(project) {
        val index = ModuleIndex()
        for (module in ModuleManager.getInstance(project).modules) {
            for (orderEntry in ModuleRootManager.getInstance(module).orderEntries) {
                if (orderEntry is ModuleOrderEntry) {
                    val referenced = orderEntry.module
                    if (referenced != null) {
                        val map = if (orderEntry.isExported) index.exportingUsages else index.plainUsages
                        map.putValue(referenced, module)
                    }
                }
            }
        }
        CachedValueProvider.Result(index, ProjectRootModificationTracker.getInstance(project))
    }!!
}
