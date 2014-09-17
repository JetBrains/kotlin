/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.caches.resolve

import com.intellij.openapi.module.Module
import com.intellij.util.containers.MultiMap
import com.intellij.openapi.project.Project
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleOrderEntry
import gnu.trove.THashSet
import com.intellij.util.containers.Queue

fun IdeaModuleInfo.getDependentModules(): Set<IdeaModuleInfo> {
    if (this is ModuleTestSourceInfo) {
        return getDependentsByModule(this.module, true).toSet()
    }
    else if (this is ModuleProductionSourceInfo) {
        return getDependentsByModule(this.module, false).toSet()
    }
    else {
        //TODO:
        return setOf()
    }
}

private fun getDependentsByModule(module: Module, includeTests: Boolean): Collection<IdeaModuleInfo> {
    val dependents = getDependents(module)
    if (includeTests) {
        return dependents.flatMap { listOf(module.testSourceInfo(), module.productionSourceInfo()) }
    }
    else {
        return dependents.map { module.productionSourceInfo() }
    }
}


private fun getDependents(module: Module): Set<Module> {
    val result = THashSet<Module>()
    result.add(module)

    val processedExporting = THashSet<Module>()

    val index = getModuleIndex(module.getProject())

    val walkingQueue = Queue<Module>(10)
    walkingQueue.addLast(module)

    while (!walkingQueue.isEmpty()) {
        val current = walkingQueue.pullFirst()
        processedExporting.add(current!!)
        result.addAll(index.plainUsages.get(current))
        for (dependent in index.exportingUsages.get(current)) {
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
        val index = ModuleIndex()
        for (module in ModuleManager.getInstance(project).getModules()) {
            for (orderEntry in ModuleRootManager.getInstance(module).getOrderEntries()) {
                if (orderEntry is ModuleOrderEntry) {
                    val referenced = (orderEntry as ModuleOrderEntry).getModule()
                    if (referenced != null) {
                        val map = if ((orderEntry as ModuleOrderEntry).isExported()) index.exportingUsages else index.plainUsages
                        map.putValue(referenced, module)
                    }
                }
            }
        }
        return index
    }