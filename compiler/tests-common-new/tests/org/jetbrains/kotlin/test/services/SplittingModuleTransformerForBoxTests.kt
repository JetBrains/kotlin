/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.builders.RegisteredDirectivesBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

/**
 * This transformers is used for transforming test with several files
 *   into test with two modules, the second one containing the last file.
 *
 * Used when the same test sets are run both in a single module mode (as in IrBlackBoxInlineCodegenTest)
 * and in multi-module mode (as in IrCompileKotlinAgainstInlineKotlinTest).
 *
 * If the test is already multimodule, do nothing.
 */
@TestInfrastructureInternals
class SplittingModuleTransformerForBoxTests : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure {
        if (moduleStructure.modules.size > 1) {
            // The test is already multimodule, no need to split it into modules further.
            return moduleStructure
        }
        val module = moduleStructure.modules.single()
        val realFiles = module.files.filterNot { it.isAdditional }
        if (realFiles.size < 2) error("Test should contain at least two files")
        val additionalFiles = module.files.filter { it.isAdditional }
        val firstModuleFiles = realFiles.dropLast(1)
        val secondModuleFile = realFiles.last()
        val firstModule = TestModule(
            name = "lib",
            module.targetPlatform,
            module.targetBackend,
            module.frontendKind,
            module.backendKind,
            module.binaryKind,
            files = firstModuleFiles + additionalFiles,
            allDependencies = emptyList(),
            module.directives,
            module.languageVersionSettings
        )

        val secondModule = TestModule(
            name = "main",
            module.targetPlatform,
            module.targetBackend,
            module.frontendKind,
            module.backendKind,
            module.binaryKind,
            files = listOf(secondModuleFile) + additionalFiles,
            allDependencies = listOf(DependencyDescription("lib", DependencyKind.Binary, DependencyRelation.FriendDependency)),
            RegisteredDirectivesBuilder(module.directives).apply {
                -CodegenTestDirectives.IGNORE_FIR_DIAGNOSTICS
            }.build(),
            module.languageVersionSettings
        )
        return TestModuleStructureImpl(listOf(firstModule, secondModule), moduleStructure.originalTestDataFiles)
    }
}
