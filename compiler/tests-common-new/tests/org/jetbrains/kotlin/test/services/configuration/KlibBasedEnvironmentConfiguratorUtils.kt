/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.configuration

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.test.frontend.classic.moduleDescriptorProvider
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import java.io.File

interface KlibBasedEnvironmentConfiguratorUtils {
    fun getKlibArtifactSimpleName(testServices: TestServices, moduleName: String): String {
        val testName = testServices.testInfo.methodName.removePrefix("test").decapitalizeAsciiOnly()
        val outputFileSuffix = if (moduleName == ModuleStructureExtractor.DEFAULT_MODULE_NAME) "" else "-$moduleName"
        return testName + outputFileSuffix
    }

    fun getKlibArtifactFile(testServices: TestServices, moduleName: String): File {
        return getKlibOutputDir(testServices).resolve(getKlibArtifactSimpleName(testServices, moduleName) + ".klib")
    }

    fun getKlibOutputDir(testServices: TestServices): File {
        return testServices.temporaryDirectoryManager.getOrCreateTempDirectory(OUTPUT_KLIB_DIR_NAME)
    }

    /**
     * Return the list of [KotlinLibrary] with all transitive dependencies recursively resolved.
     * Note: This list contains stdlib at the first position if there is any.
     */
    fun getDependencyLibrariesFor(module: TestModule, testServices: TestServices): List<KotlinLibrary> = buildList {
        getDependencyModulesFor(module, testServices).forEach { moduleDescriptor ->
            val library = testServices.libraryProvider.getCompiledLibraryByDescriptor(moduleDescriptor)
            if (library.isAnyPlatformStdlib)
                add(0, library)
            else
                add(library)
        }
    }

    fun getDependencyModulesFor(module: TestModule, testServices: TestServices): Set<ModuleDescriptorImpl> {
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

    // TODO (KT-65837): Used only in Kotlin/Native. To be removed later.
    fun getAllDependenciesMappingFor(module: TestModule, testServices: TestServices): Map<KotlinLibrary, List<KotlinLibrary>> {
        val mapping: Map<ModuleDescriptor, KotlinLibrary> = getDependencyModulesFor(module, testServices)
            .associateWith { testServices.libraryProvider.getCompiledLibraryByDescriptor(it) }

        return mapping.entries.associate { (descriptor, library) ->
            library to descriptor.allDependencyModules.filter { it != descriptor }.map { mapping.getValue(it) }
        }
    }

    companion object {
        private const val OUTPUT_KLIB_DIR_NAME = "outputKlibDir"
    }
}
