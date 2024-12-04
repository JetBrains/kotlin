/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import java.io.File

interface KlibBasedEnvironmentConfiguratorUtils {
    fun getKlibArtifactSimpleName(testServices: TestServices, moduleName: String): String {
        val testName = testServices.testInfo.methodName.removePrefix("test").decapitalizeAsciiOnly()
        val outputFileSuffix = if (moduleName == ModuleStructureExtractor.DEFAULT_MODULE_NAME) "" else "-$moduleName"
        return testName + outputFileSuffix
    }

    fun getKlibArtifactFile(testServices: TestServices, moduleName: String): File {
        return getKlibOutputDir(testServices).resolve(getKlibArtifactSimpleName(testServices, moduleName))
    }

    fun getKlibOutputDir(testServices: TestServices): File {
        return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(OUTPUT_KLIB_DIR_NAME)
    }

    fun getAllRecursiveLibrariesFor(module: TestModule, testServices: TestServices): Map<KotlinLibrary, ModuleDescriptorImpl> {
        val dependencies = getAllRecursiveDependenciesFor(module, testServices)
        return dependencies.associateBy { testServices.libraryProvider.getCompiledLibraryByDescriptor(it) }
    }

    fun getAllRecursiveDependenciesFor(module: TestModule, testServices: TestServices): Set<ModuleDescriptorImpl> {
        val visited = mutableSetOf<ModuleDescriptorImpl>()
        fun getRecursive(descriptor: ModuleDescriptor) {
            descriptor.allDependencyModules.forEach {
                if (it is ModuleDescriptorImpl && it !in visited) {
                    visited += it
                    getRecursive(it)
                }
            }
        }

        getRecursive(testServices.moduleDescriptorProvider.getModuleDescriptor(module))
        return visited
    }

    fun getAllDependenciesMappingFor(module: TestModule, testServices: TestServices): Map<KotlinLibrary, List<KotlinLibrary>> {
        val allRecursiveLibraries: Map<KotlinLibrary, ModuleDescriptor> = getAllRecursiveLibrariesFor(module, testServices)
        val m2l = allRecursiveLibraries.map { it.value to it.key }.toMap()

        return allRecursiveLibraries.keys.associateWith { m ->
            val descriptor = allRecursiveLibraries[m] ?: error("No descriptor found for library ${m.libraryName}")
            descriptor.allDependencyModules.filter { it != descriptor }.map { m2l.getValue(it) }
        }
    }

    fun getKlibDependencies(module: TestModule, testServices: TestServices, kind: DependencyRelation): List<File> {
        val visited = mutableSetOf<TestModule>()
        fun getRecursive(module: TestModule, relation: DependencyRelation) {
            val dependencies = if (relation == DependencyRelation.FriendDependency) {
                module.friendDependencies
            } else {
                module.regularDependencies
            }
            dependencies
                .filter { it.kind != DependencyKind.Source }
                .map { testServices.dependencyProvider.getTestModule(it.moduleName) }.forEach {
                    if (it !in visited) {
                        visited += it
                        getRecursive(it, relation)
                    }
                }
        }
        getRecursive(module, kind)
        return visited.map { testServices.dependencyProvider.getArtifact(it, ArtifactKinds.KLib).outputFile }
    }

    companion object {
        private const val OUTPUT_KLIB_DIR_NAME = "outputKlibDir"
    }
}
