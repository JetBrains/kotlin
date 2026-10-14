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
 * Extracts the helper file added by [ReflectionPackageNameAdditionalSourceProvider] from the test
 * module it was attached to and places it into a dedicated module named [HELPERS_MODULE_NAME].
 *
 * Every original test module then gets a regular `DependencyKind.Binary` dependency on the new
 * helpers module, as `BatchingPackageInserter` annotates the files of all of them.
 *
 * This allows `BatchingPackageInserter` to annotate grouped backward compatibility tests
 * with the annotation `kotlin.internal.ReflectionPackageName` without it present in stdlib older than v2.5.
 * This annotation is used in backends from v2.5 to keep its original package name for the reflection.
 * The current stdlib contains this annotation as well, so it is necessary not to link the helper module into the executable,
 * to prevent `IllegalStateException: IrClassSymbolImpl is already bound. Signature: helpers/...` at link time.
 */
@OptIn(TestInfrastructureInternals::class)
object ReflectionPackageNameHelperModuleTransformer : ModuleStructureTransformer() {
    const val HELPERS_MODULE_NAME: String = "reflectionPackageNameHelper"

    override fun transformModuleStructure(
        moduleStructure: TestModuleStructure,
        defaultsProvider: DefaultsProvider
    ): TestModuleStructure {
        val originalModules = moduleStructure.modules

        // 1. Find the helper file. [ReflectionPackageNameAdditionalSourceProvider] attaches it to a single module,
        //    and only when the test is going to be grouped, so its absence means there is nothing to do.
        val helperFiles = originalModules.flatMap { module -> module.files.filter { isReflectionPackageNameHelperFile(it) } }
        if (helperFiles.isEmpty()) return moduleStructure
        val helperFile = helperFiles.singleOrNull()
            ?: testInfraError("Expected a single ReflectionPackageName helper file, got ${helperFiles.size}")

        // 2. Build the new helpers module.
        //    - Uses the same language version settings as the first original module.
        //    - Has no dependencies of its own.
        val firstModule = originalModules.first()
        val helpersModule = TestModule(
            name = HELPERS_MODULE_NAME,
            files = listOf(helperFile),
            allDependencies = emptyList(),
            directives = firstModule.directives,
            languageVersionSettings = firstModule.languageVersionSettings,
        )

        val helpersDependency = DependencyDescription(
            dependencyModule = helpersModule,
            kind = DependencyKind.Binary,
            relation = DependencyRelation.RegularDependency,
        )

        // 3. Strip helper files from each original module and add the dependency.
        //    Rewrite dependencies recursively so every edge points to the final rewritten module
        //    instance, rather than to an intermediate copy with stale dependencies.
        val originalModulesByName = originalModules.associateBy { it.name }
        val rewrittenModulesByName = mutableMapOf(HELPERS_MODULE_NAME to helpersModule)

        fun rewriteModule(module: TestModule): TestModule {
            rewrittenModulesByName[module.name]?.let { return it }

            val rewrittenDependencies = module.allDependencies.map { dependency ->
                val dependencyModule = when (val dependencyModuleName = dependency.dependencyModule.name) {
                    HELPERS_MODULE_NAME -> helpersModule
                    else -> originalModulesByName[dependencyModuleName]?.let(::rewriteModule)
                        ?: testInfraError("Module $dependencyModuleName not found while rewriting dependencies of ${module.name}")
                }
                dependency.copy(dependencyModule = dependencyModule)
            }
            val newDependencies = if (rewrittenDependencies.any { it.dependencyModule.name == HELPERS_MODULE_NAME }) {
                rewrittenDependencies
            } else {
                rewrittenDependencies + helpersDependency
            }

            return module.copy(
                files = module.files.filterNot { isReflectionPackageNameHelperFile(it) },
                allDependencies = newDependencies,
            ).also { rewrittenModulesByName[module.name] = it }
        }

        val rewrittenModules = originalModules.map(::rewriteModule)

        return TestModuleStructureImpl(
            modules = listOf(helpersModule) + rewrittenModules,
            originalTestDataFiles = moduleStructure.originalTestDataFiles,
        )
    }

    /**
     * Returns true if the given module is the dedicated helper module this transformer synthesizes.
     * A grouping-stage facade must neither collect its KLIB as a per-test output nor link it.
     */
    fun isHelperModule(module: TestModule): Boolean = module.name == HELPERS_MODULE_NAME

    /**
     * Returns true if the given file is the synthetic ReflectionPackageName helper file produced
     * by [ReflectionPackageNameAdditionalSourceProvider].
     */
    private fun isReflectionPackageNameHelperFile(file: TestFile): Boolean {
        if (!file.isAdditional) return false
        // Cheap content check: helper files start with `package kotlin.internal`.
        val content = file.originalContent
        // Either it begins with `package kotlin.internal` (first line), or contains it as a
        // non-commented line at the top.
        val lineSequence = content.lineSequence()
        val firstNonEmptyLine = lineSequence.firstOrNull { it.isNotBlank() } ?: return false
        return firstNonEmptyLine.trim() == "package kotlin.internal" &&
                lineSequence.any { it.contains("annotation class ReflectionPackageName") }
    }
}
