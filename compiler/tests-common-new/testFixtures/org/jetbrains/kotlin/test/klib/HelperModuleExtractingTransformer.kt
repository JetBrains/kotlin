/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.ModuleStructureTransformer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl
import org.jetbrains.kotlin.test.testInfraError

/**
 * Moves the helper files of a test, as told by [isHelperFile], out of its modules into a dedicated module named
 * [helpersModuleName], which is put first and which every original module then gets a regular binary dependency on.
 *
 * The helpers module has no dependencies of its own, and it takes the directives and the language version settings
 * of the first original module. A test without helper files is left as it is.
 */
@OptIn(TestInfrastructureInternals::class)
abstract class HelperModuleExtractingTransformer : ModuleStructureTransformer() {
    protected abstract val helpersModuleName: String

    protected abstract fun isHelperFile(file: TestFile): Boolean

    /** Chooses the files of the helpers module out of the [helperFiles] of all the modules, in the order of the modules. */
    protected open fun selectHelpersModuleFiles(helperFiles: List<TestFile>): List<TestFile> = helperFiles

    final override fun transformModuleStructure(
        moduleStructure: TestModuleStructure,
        defaultsProvider: DefaultsProvider
    ): TestModuleStructure {
        val originalModules = moduleStructure.modules
        val helperFiles = originalModules.flatMap { module -> module.files.filter(::isHelperFile) }
        if (helperFiles.isEmpty()) return moduleStructure

        val firstModule = originalModules.first()
        val helpersModule = TestModule(
            name = helpersModuleName,
            files = selectHelpersModuleFiles(helperFiles),
            allDependencies = emptyList(),
            directives = firstModule.directives,
            languageVersionSettings = firstModule.languageVersionSettings,
        )
        val helpersDependency = DependencyDescription(
            dependencyModule = helpersModule,
            kind = DependencyKind.Binary,
            relation = DependencyRelation.RegularDependency,
        )

        // Rewrite dependencies recursively so every edge points to the final rewritten module
        // instance, rather than to an intermediate copy with stale dependencies.
        val originalModulesByName = originalModules.associateBy { it.name }
        val rewrittenModulesByName = mutableMapOf(helpersModuleName to helpersModule)

        fun rewriteModule(module: TestModule): TestModule {
            rewrittenModulesByName[module.name]?.let { return it }

            val rewrittenDependencies = module.allDependencies.map { dependency ->
                val dependencyModuleName = dependency.dependencyModule.name
                val dependencyModule = rewrittenModulesByName[dependencyModuleName]
                    ?: originalModulesByName[dependencyModuleName]?.let(::rewriteModule)
                    ?: testInfraError("Module $dependencyModuleName not found while rewriting dependencies of ${module.name}")
                dependency.copy(dependencyModule = dependencyModule)
            }
            val newDependencies = if (rewrittenDependencies.any { it.dependencyModule.name == helpersModuleName }) {
                rewrittenDependencies
            } else {
                rewrittenDependencies + helpersDependency
            }

            return module.copy(
                files = module.files.filterNot(::isHelperFile),
                allDependencies = newDependencies,
            ).also { rewrittenModulesByName[module.name] = it }
        }

        return TestModuleStructureImpl(
            modules = listOf(helpersModule) + originalModules.map(::rewriteModule),
            originalTestDataFiles = moduleStructure.originalTestDataFiles,
        )
    }
}
