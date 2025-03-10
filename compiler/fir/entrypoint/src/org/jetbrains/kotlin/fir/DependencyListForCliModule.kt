/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.MultipleModuleDataProvider
import org.jetbrains.kotlin.name.Name
import java.nio.file.Path
import java.nio.file.Paths

/**
 * This class represents the set of dependencies for some source module.
 *
 * [moduleDataProvider] is the service which will be used by deserialized symbol
 * provider to determine to exactly which dependency the deserialized declaration belongs.
 * There is a contract that any module data returned by [moduleDataProvider] is present in
 * either [regularDependencies], [dependsOnDependencies] or [friendDependencies].
 *
 * The constructor of [DependencyListForCliModule] is an implementation detail,
 * please use [DependencyListForCliModule.build] functions for creation of new instances.
 */
class DependencyListForCliModule @PrivateSessionConstructor constructor(
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
            mainModuleName: Name,
            init: Builder.BuilderForDefaultDependenciesModule.() -> Unit = {},
        ): DependencyListForCliModule {
            return build { defaultDependenciesSet(mainModuleName, init) }
        }
    }

    class Builder {
        private val allRegularDependencies: MutableSet<FirBinaryDependenciesModuleData> = mutableSetOf()
        private val allFriendDependencies: MutableSet<FirBinaryDependenciesModuleData> = mutableSetOf()
        private val allDependsOnDependencies: MutableSet<FirBinaryDependenciesModuleData> = mutableSetOf()

        private val filtersMap: MutableMap<FirBinaryDependenciesModuleData, MutableSet<Path>> = mutableMapOf()

        fun dependencies(moduleData: FirBinaryDependenciesModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allRegularDependencies)
        }

        fun friendDependencies(moduleData: FirBinaryDependenciesModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allFriendDependencies)
        }

        fun dependsOnDependencies(moduleData: FirBinaryDependenciesModuleData, paths: Collection<String>) {
            dependencies(moduleData, paths, allDependsOnDependencies)
        }

        inline fun defaultDependenciesSet(
            mainModuleName: Name,
            init: BuilderForDefaultDependenciesModule.() -> Unit,
        ) {
            BuilderForDefaultDependenciesModule(
                regular = createData("<regular dependencies of $mainModuleName>"),
                dependsOn = createData("<dependsOn dependencies of $mainModuleName>"),
                friend = createData("<friends dependencies of $mainModuleName>")
            ).apply(init)
        }

        fun createData(name: String): FirBinaryDependenciesModuleData = FirBinaryDependenciesModuleData(Name.special(name))

        inner class BuilderForDefaultDependenciesModule(
            val regular: FirBinaryDependenciesModuleData,
            val dependsOn: FirBinaryDependenciesModuleData,
            val friend: FirBinaryDependenciesModuleData
        ) {
            init {
                allRegularDependencies += regular
                allDependsOnDependencies += dependsOn
                allFriendDependencies += friend
            }

            fun dependencies(paths: Collection<String>) {
                dependencies(regular, paths)
            }

            fun friendDependencies(paths: Collection<String>) {
                friendDependencies(friend, paths)
            }

            fun dependsOnDependencies(paths: Collection<String>) {
                dependsOnDependencies(dependsOn, paths)
            }
        }

        private fun dependencies(
            moduleData: FirBinaryDependenciesModuleData,
            paths: Collection<String>,
            destination: MutableSet<FirBinaryDependenciesModuleData>,
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
                /*
                 * We need to put dependsOn and friend filters on top, as they are usually
                 * duplicated in regular dependencies.
                 *
                 * This is because how our `-Xfriend-path` argument works: it is a subset of -cp argument:
                 *
                 *     -cp lib1.jar lib2.jar
                 *     -Xfriend-path lib2.jar
                 *
                 * So here we pass both `lib1.jar` and `lib2.jar` as paths of regular dependencies (because they are just plain
                 * content roots) and additionally register lib2.jar path as friend root.
                 */
                .entries.sortedBy {
                    when (it.key) {
                        in allDependsOnDependencies -> 1
                        in allFriendDependencies -> 2
                        in allRegularDependencies -> 3
                        else -> 4
                    }
                }.associate { (key, value) -> key to value }
                .toMutableMap()

            fun Collection<FirBinaryDependenciesModuleData>.filterUsedModules(): MutableList<FirBinaryDependenciesModuleData> =
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
            @OptIn(PrivateSessionConstructor::class)
            return DependencyListForCliModule(
                regularDependencies = regularDependencies,
                dependsOnDependencies = dependsOnDependencies,
                friendDependencies = friendDependencies,
                moduleDataProvider
            )
        }
    }
}
