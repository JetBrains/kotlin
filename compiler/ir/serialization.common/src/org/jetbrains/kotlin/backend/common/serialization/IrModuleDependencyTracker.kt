/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization

import org.jetbrains.kotlin.backend.common.IrModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addIfNotNull

// TODO: This is a temporary measure that should be removed in the future (KT-77244).
class IrModuleDependencyTracker {
    private val trackedModules: MutableMap<IrModuleFragment, /* dependencies */ MutableSet<IrModuleFragment>> = mutableMapOf()

    fun addModuleForTracking(module: IrModuleFragment) {
        val oldValue = trackedModules.put(module, mutableSetOf())
        check(oldValue == null) { "Module ${module.name} is already present in ${this::class}" }
    }

    fun trackDependency(fromModule: IrModuleFragment, toModule: IrModuleFragment) {
        if (fromModule !== toModule) {
            val dependencies = trackedModules[fromModule] ?: error("No module data for ${fromModule.name} in ${this::class}")
            dependencies.add(toModule)
        }
    }

    fun reverseTopoOrder(moduleDependencies: IrModuleDependencies): IrModuleDependencies {
        val modulesToSort = moduleDependencies.all.toSet()

        val untrackedModules = trackedModules.keys - modulesToSort
        check(untrackedModules.isEmpty()) {
            "The following modules are not being tracked in ${this::class}: ${untrackedModules.joinToString { it.name.asString() }}"
        }

        if (modulesToSort.size <= 1)
            return moduleDependencies

        val sortedModules: List<IrModuleFragment> = DFS.topologicalOrder(modulesToSort) { module -> trackedModules.getValue(module) }
            .filter { it in modulesToSort } // Avoid accidentally adding dependencies that were not in [IrModuleDependencies.all].
            .reversed()
            .let { sortedModules ->
                // The stdlib and included libraries are special. They need to be at the fixed places.
                buildList {
                    addIfNotNull(moduleDependencies.stdlib)
                    sortedModules.filterTo(this) { it != moduleDependencies.stdlib && it != moduleDependencies.included }
                    addIfNotNull(moduleDependencies.included)
                }
            }

        return moduleDependencies.copy(all = sortedModules)
    }
}
