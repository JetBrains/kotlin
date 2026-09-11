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
 * Extracts helper files added by `ReflectionPackageNameAdditionalSourceProvider` from each test
 * module and places them into a single dedicated module named [HELPERS_MODULE_NAME].
 *
 * The original test modules then get a regular `DependencyKind.Binary` dependency on the new
 * helpers module instead of carrying the helper sources themselves.
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
        if (originalModules.isEmpty()) return moduleStructure

        // We only act when at least one module has actual helpers files attached.
        val hasHelperFile = originalModules.any { module ->
            module.files.any { isReflectionPackageNameHelperFile(it) }
        }
        if (!hasHelperFile) return moduleStructure

        // 1. Collect all helper files from all modules (deduplicated by relative path).
        val helperFilesByPath = linkedMapOf<String, TestFile>()
        originalModules.forEach { module ->
            module.files.forEach { file ->
                if (isReflectionPackageNameHelperFile(file)) {
                    helperFilesByPath.putIfAbsent(file.relativePath, file)
                }
            }
        }

        // 2. Build the new helpers module.
        //    - Uses the same language version settings as the first original module.
        //    - Has no dependencies of its own.
        val firstModule = originalModules.first()
        val helpersModule = TestModule(
            name = HELPERS_MODULE_NAME,
            files = helperFilesByPath.values.toList(),
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
     * Returns true if the given file is the synthetic ReflectionPackageName helper file produced
     * by `ReflectionPackageNameAdditionalSourceProvider`.
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
