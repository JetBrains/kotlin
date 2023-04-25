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
        inline fun build(binaryModuleData: BinaryModuleData, init: Builder.() -> Unit = {}): DependencyListForCliModule {
            return Builder(binaryModuleData).apply(init).build()
        }
    }

    class Builder(val binaryModuleData: BinaryModuleData) {
        private val allRegularDependencies = mutableListOf<FirModuleData>()
        private val allFriendsDependencies = mutableListOf<FirModuleData>()
        private val allDependsOnDependencies = mutableListOf<FirModuleData>()

        private val filtersMap =
            listOf(
                binaryModuleData.dependsOn,
                binaryModuleData.friends,
                binaryModuleData.regular
            ).associateWithTo(mutableMapOf<FirModuleData, MutableSet<Path>>()) { mutableSetOf() }

        fun dependency(vararg path: Path) {
            filtersMap.getValue(binaryModuleData.regular) += path
        }

        fun dependency(moduleData: FirModuleData, vararg path: Path) {
            filtersMap.getOrPut(moduleData) {
                allRegularDependencies.add(moduleData)
                mutableSetOf()
            } += path
        }

        fun dependency(vararg path: String) {
            path.mapTo(filtersMap.getValue(binaryModuleData.regular)) { Paths.get(it) }
        }

        @JvmName("dependenciesString")
        fun dependencies(paths: Collection<String>) {
            paths.mapTo(filtersMap.getValue(binaryModuleData.regular)) { Paths.get(it) }
        }

        @JvmName("dependenciesString")
        fun dependencies(moduleData: FirModuleData, paths: Collection<String>) {
            paths.mapTo(
                filtersMap.getOrPut(moduleData) {
                    allRegularDependencies.add(moduleData)
                    mutableSetOf()
                }
            ) {
                Paths.get(it)
            }
        }

        @JvmName("friendDependenciesString")
        fun friendDependencies(paths: Collection<String>) {
            paths.mapTo(filtersMap.getValue(binaryModuleData.friends)) { Paths.get(it) }
        }

        @JvmName("dependsOnDependenciesString")
        fun dependsOnDependencies(paths: Collection<String>) {
            paths.mapTo(filtersMap.getValue(binaryModuleData.dependsOn)) { Paths.get(it) }
        }

        fun dependencies(paths: Collection<Path>) {
            filtersMap.getValue(binaryModuleData.regular) += paths
        }

        fun friendDependencies(paths: Collection<Path>) {
            filtersMap.getValue(binaryModuleData.friends) += paths
        }

        fun dependsOnDependencies(paths: Collection<Path>) {
            filtersMap.getValue(binaryModuleData.dependsOn) += paths
        }

        fun sourceFriendsDependencies(modules: Collection<FirModuleData>) {
            allFriendsDependencies += modules
        }

        fun sourceDependsOnDependencies(modules: Collection<FirModuleData>) {
            allDependsOnDependencies += modules
        }

        fun build(): DependencyListForCliModule {
            val pathFiltersMap: MutableMap<FirModuleData, LibraryPathFilter> = filtersMap
                .filterValues { it.isNotEmpty() }
                .mapValues { LibraryPathFilter.LibraryList(it.value) }
                .toMutableMap()

            allRegularDependencies += binaryModuleData.regular
            if (pathFiltersMap.isEmpty()) {
                return DependencyListForCliModule(
                    allRegularDependencies,
                    dependsOnDependencies = allDependsOnDependencies,
                    friendsDependencies = allFriendsDependencies,
                    SingleModuleDataProvider(binaryModuleData.regular)
                )
            }
            if (binaryModuleData.friends in pathFiltersMap) {
                allFriendsDependencies += binaryModuleData.friends
            }
            if (binaryModuleData.dependsOn in pathFiltersMap) {
                allDependsOnDependencies += binaryModuleData.dependsOn
            }

            val moduleDataProvider = MultipleModuleDataProvider(pathFiltersMap)
            pathFiltersMap.putIfAbsent(binaryModuleData.regular, LibraryPathFilter.TakeAll)
            return DependencyListForCliModule(
                allRegularDependencies,
                allDependsOnDependencies,
                allFriendsDependencies,
                moduleDataProvider
            )
        }
    }
}
