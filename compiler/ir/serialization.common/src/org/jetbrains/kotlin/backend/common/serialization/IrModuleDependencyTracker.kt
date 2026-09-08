/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.utils.DFS

interface IrModuleDependencyTracker {
    fun addModuleForTracking(module: IrModuleFragment)
    fun trackDependency(fromModule: IrModuleFragment, toModule: IrModuleFragment)
    fun reverseTopoOrder(moduleDependencies: IrModuleDependencies): IrModuleDependencies

    companion object {
        val DISABLED = object : IrModuleDependencyTracker {
            override fun addModuleForTracking(module: IrModuleFragment) = Unit
            override fun trackDependency(fromModule: IrModuleFragment, toModule: IrModuleFragment) = Unit
            override fun reverseTopoOrder(moduleDependencies: IrModuleDependencies) = moduleDependencies
        }
    }
}

class IrModuleDependencyTrackerImpl : IrModuleDependencyTracker {
    private val trackedModules: MutableMap<IrModuleFragment, /* dependencies */ MutableSet<IrModuleFragment>> = mutableMapOf()

    private fun getAllDependencies(current: IrModuleFragment, result: MutableSet<IrModuleFragment>) {
        trackedModules.getValue(current).forEach { dependency ->
            if (result.add(dependency)) {
                getAllDependencies(dependency, result)
            }
        }
    }

    fun getAllDependencies(module: IrModuleFragment): Set<IrModuleFragment> {
        val result = mutableSetOf<IrModuleFragment>()
        getAllDependencies(module, result)
        return result
    }

    override fun addModuleForTracking(module: IrModuleFragment) {
        val oldValue = trackedModules.put(module, mutableSetOf())
        check(oldValue == null) { "Module ${module.name} is already present in ${this::class}" }
    }

    override fun trackDependency(fromModule: IrModuleFragment, toModule: IrModuleFragment) {
        if (fromModule !== toModule) {
            val dependencies = trackedModules[fromModule] ?: error("No module data for ${fromModule.name} in ${this::class}")
            dependencies.add(toModule)
        }
    }

    override fun reverseTopoOrder(moduleDependencies: IrModuleDependencies): IrModuleDependencies {
        val modulesToSort = moduleDependencies.allDependencies.toSet()

        val untrackedModules = modulesToSort - trackedModules.keys
        check(untrackedModules.isEmpty()) {
            "The following modules are not being tracked in ${this::class}: ${untrackedModules.joinToString { it.name.asString() }}"
        }

        if (modulesToSort.size <= 1)
            return moduleDependencies

        val sortedModules: List<IrModuleFragment> = DFS.topologicalOrder(modulesToSort) { module -> trackedModules.getValue(module) }
            .filter { it in modulesToSort } // Avoid accidentally adding dependencies that were not in [IrModuleDependencies.all].
            .reversed()

        return moduleDependencies.copy(allDependencies = sortedModules)
    }
}
