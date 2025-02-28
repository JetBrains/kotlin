/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.MultipleModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import java.nio.file.Path
import java.nio.file.Paths

class DependencyListForCliModule(
    val regularDependencies: List<FirModuleData>,
    val dependsOnDependencies: List<FirModuleData>,
    val friendsDependencies: List<FirModuleData>,
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
        private val allRegularDependencies: MutableList<FirModuleData> = mutableListOf()
        private val allFriendsDependencies: MutableList<FirModuleData> = mutableListOf()
        private val allDependsOnDependencies: MutableList<FirModuleData> = mutableListOf()

        private val filtersMap: MutableMap<FirModuleData, MutableSet<Path>> = mutableMapOf()

        fun dependencies(moduleData: FirModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allRegularDependencies)
        }

        fun friendDependencies(moduleData: FirModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allFriendsDependencies)
        }

        fun dependsOnDependencies(moduleData: FirModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allDependsOnDependencies)
        }

        inline fun defaultDependenciesSet(binaryModuleData: BinaryModuleData, init: BuilderForDefaultDependenciesModule.() -> Unit) {
            BuilderForDefaultDependenciesModule(binaryModuleData).apply(init)
        }

        inner class BuilderForDefaultDependenciesModule(val binaryModuleData: BinaryModuleData) {
            fun dependencies(paths: Collection<String>) {
                dependencies(binaryModuleData.regular, paths)
            }

            fun friendDependencies(paths: Collection<String>) {
                friendDependencies(binaryModuleData.friends, paths)
            }

            fun dependsOnDependencies(paths: Collection<String>) {
                dependsOnDependencies(binaryModuleData.dependsOn, paths)
            }
        }

        private fun dependencies(moduleData: FirModuleData, paths: Collection<String>, destination: MutableList<FirModuleData>) {
            destination.add(moduleData)
            val filterSet = filtersMap.getOrPut(moduleData) { mutableSetOf() }
            paths.mapTo(filterSet) {
                Paths.get(it).toAbsolutePath()
            }
        }

        fun build(): DependencyListForCliModule {
            val pathFiltersMap: MutableMap<FirModuleData, LibraryPathFilter> = filtersMap
                .filterValues { it.isNotEmpty() }
                .mapValues { LibraryPathFilter.LibraryList(it.value) }
                .toMutableMap()

            if (pathFiltersMap.isEmpty() && allRegularDependencies.size == 1) {
                return DependencyListForCliModule(
                    allRegularDependencies,
                    dependsOnDependencies = allDependsOnDependencies,
                    friendsDependencies = allFriendsDependencies,
                    SingleModuleDataProvider(allRegularDependencies.single())
                )
            }

            val moduleDataProvider = MultipleModuleDataProvider(pathFiltersMap)
            allRegularDependencies.singleOrNull()?.let {
                pathFiltersMap.putIfAbsent(it, LibraryPathFilter.TakeAll)
            }
            return DependencyListForCliModule(
                allRegularDependencies,
                allDependsOnDependencies,
                allFriendsDependencies,
                moduleDataProvider
            )
        }
    }
}
