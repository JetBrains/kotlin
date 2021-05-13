/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

/**
 * This transformers is used for transforming test with two files
 *   into test with two modules
 *
 * It will fail in case when module structure contains more than one module
 *   or not exactly two files in it
 */
@TestInfrastructureInternals
class ModuleTransformerForTwoFilesBoxTests : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure): TestModuleStructure {
        val module = moduleStructure.modules.singleOrNull() ?: error("Test should contain only one module")
        val realFiles = module.files.filterNot { it.isAdditional }
        if (realFiles.size != 2) error("Test should contain exactly two files")
        val additionalFiles = module.files.filter { it.isAdditional }
        val (first, second) = realFiles
        val firstModule = TestModule(
            name = "lib",
            module.targetPlatform,
            module.targetBackend,
            module.frontendKind,
            module.binaryKind,
            files = listOf(first) + additionalFiles,
            allDependencies = emptyList(),
            module.directives,
            module.languageVersionSettings
        )

        val secondModule = TestModule(
            name = "main",
            module.targetPlatform,
            module.targetBackend,
            module.frontendKind,
            module.binaryKind,
            files = listOf(second) + additionalFiles,
            allDependencies = listOf(DependencyDescription("lib", DependencyKind.Binary, DependencyRelation.FriendDependency)),
            module.directives,
            module.languageVersionSettings
        )
        return TestModuleStructureImpl(listOf(firstModule, secondModule), moduleStructure.originalTestDataFiles)
    }
}
