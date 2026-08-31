/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jklib.test.irText

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.ModuleStructureTransformer
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl
import org.jetbrains.kotlin.test.services.isKtFile

@TestInfrastructureInternals
class SplittingModuleTransformerForJKlibTests : ModuleStructureTransformer() {
    override fun transformModuleStructure(
        moduleStructure: TestModuleStructure,
        defaultsProvider: DefaultsProvider
    ): TestModuleStructure {
        if (moduleStructure.modules.size > 1) {
            return moduleStructure
        }
        val module = moduleStructure.modules.single()
        val realFiles = module.files.filterNot { it.isAdditional }
        val ktFiles = realFiles.filter { it.isKtFile }
        if (ktFiles.size < 2) return moduleStructure

        val mainFile = ktFiles.find { it.name == "main.kt" || it.name == "test.kt" } ?: ktFiles.last()
        val libFiles = module.files.filter { it != mainFile }

        val firstModule = TestModule(
            name = "lib",
            files = libFiles,
            allDependencies = emptyList(),
            directives = module.directives,
            languageVersionSettings = module.languageVersionSettings
        )

        val secondModule = TestModule(
            name = "main",
            files = listOf(mainFile),
            allDependencies = listOf(DependencyDescription(firstModule, DependencyKind.Binary, DependencyRelation.FriendDependency)),
            directives = module.directives,
            languageVersionSettings = module.languageVersionSettings
        )

        return TestModuleStructureImpl(listOf(firstModule, secondModule), moduleStructure.originalTestDataFiles)
    }
}
