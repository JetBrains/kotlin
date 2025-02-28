/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.MultipleModuleDataProvider
import java.nio.file.Path
import java.nio.file.Paths

class DependencyListForCliModule(
    val regularDependencies: List<FirModuleData>,
    val dependsOnDependencies: List<FirModuleData>,
    val friendDependencies: List<FirModuleData>,
    val moduleDataProvider: ModuleDataProvider,
) {
    companion object {
        inline fun build(init: Builder.() -> Unit = {}): DependencyListForCliModule {
            return Builder().apply(init).build()
        }

        inline fun build(
            binaryModuleData: BinaryModuleData,
            init: Builder.BuilderForDefaultDependenciesModule.() -> Unit = {},
        ): DependencyListForCliModule {
            return build { defaultDependenciesSet(binaryModuleData, init) }
        }
    }

    class Builder {
        private val allRegularDependencies: MutableSet<FirBinaryDependencyModuleData> = mutableSetOf()
        private val allFriendDependencies: MutableSet<FirBinaryDependencyModuleData> = mutableSetOf()
        private val allDependsOnDependencies: MutableSet<FirBinaryDependencyModuleData> = mutableSetOf()

        private val filtersMap: MutableMap<FirBinaryDependencyModuleData, MutableSet<Path>> = mutableMapOf()

        fun dependencies(moduleData: FirBinaryDependencyModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allRegularDependencies)
        }

        fun friendDependencies(moduleData: FirBinaryDependencyModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allFriendDependencies)
        }

        fun dependsOnDependencies(moduleData: FirBinaryDependencyModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allDependsOnDependencies)
        }

        inline fun defaultDependenciesSet(binaryModuleData: BinaryModuleData, init: BuilderForDefaultDependenciesModule.() -> Unit) {
            BuilderForDefaultDependenciesModule(binaryModuleData).apply(init)
        }

        inner class BuilderForDefaultDependenciesModule(val binaryModuleData: BinaryModuleData) {
            init {
                allRegularDependencies += binaryModuleData.regular
                allDependsOnDependencies += binaryModuleData.dependsOn
                allFriendDependencies += binaryModuleData.friend
            }

            fun dependencies(paths: Collection<String>) {
                dependencies(binaryModuleData.regular, paths)
            }

            fun friendDependencies(paths: Collection<String>) {
                friendDependencies(binaryModuleData.friend, paths)
            }

            fun dependsOnDependencies(paths: Collection<String>) {
                dependsOnDependencies(binaryModuleData.dependsOn, paths)
            }
        }

        private fun dependencies(
            moduleData: FirBinaryDependencyModuleData,
            paths: Collection<String>,
            destination: MutableSet<FirBinaryDependencyModuleData>,
        ) {
            destination.add(moduleData)
            if (paths.isEmpty()) return
            val filterSet = filtersMap.getOrPut(moduleData) { mutableSetOf() }
            paths.mapTo(filterSet) {
                Paths.get(it).toAbsolutePath()
            }
        }

        fun build(): DependencyListForCliModule {
            val pathFiltersMap: MutableMap<FirModuleData, LibraryPathFilter> = filtersMap
                .filterValues { it.isNotEmpty() }
                .mapValues { LibraryPathFilter.LibraryList(it.value) }
                // we need to put dependsOn and friend filters on top, as they are usually
                // duplicated in regular dependencies
                .entries.sortedBy {
                    when (it.key) {
                        in allDependsOnDependencies -> 1
                        in allFriendDependencies -> 2
                        in allRegularDependencies -> 3
                        else -> 4
                    }
                }.associate { (key, value) -> key to value }
                .toMutableMap()

            fun Collection<FirBinaryDependencyModuleData>.filterUsedModules(): MutableList<FirBinaryDependencyModuleData> =
                this.filterTo(mutableListOf()) { it in pathFiltersMap }

            val regularDependencies = allRegularDependencies.filterUsedModules()
            val friendDependencies = allFriendDependencies.filterUsedModules()
            val dependsOnDependencies = allDependsOnDependencies.filterUsedModules()

            // fallback in case no explicit dependencies were added
            allRegularDependencies.singleOrNull()?.let {
                pathFiltersMap.putIfAbsent(it, LibraryPathFilter.TakeAll)
                if (it !in regularDependencies) {
                    regularDependencies += it
                }
            }

            val moduleDataProvider = MultipleModuleDataProvider(pathFiltersMap)
            return DependencyListForCliModule(
                regularDependencies = regularDependencies,
                dependsOnDependencies = dependsOnDependencies,
                friendDependencies = friendDependencies,
                moduleDataProvider
            )
        }
    }
}
