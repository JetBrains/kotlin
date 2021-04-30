/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.deserialization.LibraryPathFilter
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.MultipleModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.SingleModuleDataProvider
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import java.nio.file.Path
import java.nio.file.Paths

class DependencyListForCliModule(
    val platform: TargetPlatform,
    val analyzerServices: PlatformDependentAnalyzerServices,
    val regularDependencies: List<FirModuleData>,
    val dependsOnDependencies: List<FirModuleData>,
    val friendsDependencies: List<FirModuleData>,
    val moduleDataProvider: ModuleDataProvider,
) {
    companion object {
        fun createDependencyModuleData(
            name: Name,
            platform: TargetPlatform,
            analyzerServices: PlatformDependentAnalyzerServices
        ): FirModuleData {
            return FirModuleDataImpl(
                name,
                dependencies = emptyList(),
                dependsOnDependencies = emptyList(),
                friendDependencies = emptyList(),
                platform,
                analyzerServices,
            )
        }

        inline fun build(
            mainModuleName: Name,
            platform: TargetPlatform,
            analyzerServices: PlatformDependentAnalyzerServices,
            init: Builder.() -> Unit = {}
        ): DependencyListForCliModule {
            return Builder(mainModuleName, platform, analyzerServices).apply(init).build()
        }
    }

    class Builder(val mainModuleName: Name, val platform: TargetPlatform, val analyzerServices: PlatformDependentAnalyzerServices) {
        private fun createData(name: String): FirModuleData = createDependencyModuleData(Name.special(name), platform, analyzerServices)

        private val binaryRegularDependenciesModuleData: FirModuleData = createData("<regular dependencies of $mainModuleName>")
        private val binaryDependsOnModuleData: FirModuleData = createData("<dependsOn dependencies of $mainModuleName")
        private val binaryFriendsModuleData: FirModuleData = createData("<friends dependencies of $mainModuleName")

        private val allRegularDependencies = mutableListOf<FirModuleData>()
        private val allFriendsDependencies = mutableListOf<FirModuleData>()
        private val allDependsOnDependencies = mutableListOf<FirModuleData>()

        private val filtersMap: Map<FirModuleData, MutableSet<Path>> =
            listOf(
                binaryDependsOnModuleData,
                binaryFriendsModuleData,
                binaryRegularDependenciesModuleData
            ).map { it to mutableSetOf<Path>() }.toMap()

        fun dependency(vararg path: Path) {
            filtersMap.getValue(binaryRegularDependenciesModuleData) += path
        }

        fun friendDependency(vararg path: Path) {
            filtersMap.getValue(binaryFriendsModuleData) += path
        }

        fun dependsOnDependency(vararg path: Path) {
            filtersMap.getValue(binaryDependsOnModuleData) += path
        }

        fun dependency(vararg path: String) {
            path.mapTo(filtersMap.getValue(binaryRegularDependenciesModuleData)) { Paths.get(it) }
        }

        fun friendDependency(vararg path: String) {
            path.mapTo(filtersMap.getValue(binaryFriendsModuleData)) { Paths.get(it) }
        }

        fun dependsOnDependency(vararg path: String) {
            path.mapTo(filtersMap.getValue(binaryDependsOnModuleData)) { Paths.get(it) }
        }

        @JvmName("dependenciesString")
        fun dependencies(paths: Collection<String>) {
            paths.mapTo(filtersMap.getValue(binaryRegularDependenciesModuleData)) { Paths.get(it) }
        }

        @JvmName("friendDependenciesString")
        fun friendDependencies(paths: Collection<String>) {
            paths.mapTo(filtersMap.getValue(binaryFriendsModuleData)) { Paths.get(it) }
        }

        @JvmName("dependsOnDependenciesString")
        fun dependsOnDependencies(paths: Collection<String>) {
            paths.mapTo(filtersMap.getValue(binaryDependsOnModuleData)) { Paths.get(it) }
        }

        fun dependencies(paths: Collection<Path>) {
            filtersMap.getValue(binaryRegularDependenciesModuleData) += paths
        }

        fun friendDependencies(paths: Collection<Path>) {
            filtersMap.getValue(binaryFriendsModuleData) += paths
        }

        fun dependsOnDependencies(paths: Collection<Path>) {
            filtersMap.getValue(binaryDependsOnModuleData) += paths
        }

        fun sourceDependencies(modules: Collection<FirModuleData>) {
            allRegularDependencies += modules
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

            allRegularDependencies += binaryRegularDependenciesModuleData
            if (pathFiltersMap.isEmpty()) {
                return DependencyListForCliModule(
                    platform,
                    analyzerServices,
                    allRegularDependencies,
                    dependsOnDependencies = allDependsOnDependencies,
                    friendsDependencies = allFriendsDependencies,
                    SingleModuleDataProvider(binaryRegularDependenciesModuleData)
                )
            }
            if (binaryFriendsModuleData in pathFiltersMap) {
                allFriendsDependencies += binaryFriendsModuleData
            }
            if (binaryDependsOnModuleData in pathFiltersMap) {
                allDependsOnDependencies += binaryDependsOnModuleData
            }

            val moduleDataProvider = MultipleModuleDataProvider(pathFiltersMap)
            pathFiltersMap.putIfAbsent(binaryRegularDependenciesModuleData, LibraryPathFilter.TakeAll)
            return DependencyListForCliModule(
                platform,
                analyzerServices,
                allRegularDependencies,
                allDependsOnDependencies,
                allFriendsDependencies,
                moduleDataProvider
            )
        }
    }
}
