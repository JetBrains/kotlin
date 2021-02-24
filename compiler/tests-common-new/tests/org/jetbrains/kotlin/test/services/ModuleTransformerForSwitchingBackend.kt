/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.model.DependencyDescription
import org.jetbrains.kotlin.test.model.DependencyKind
import org.jetbrains.kotlin.test.model.DependencyRelation
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.impl.TestModuleStructureImpl

/**
 * This transformers is used for transforming target backend
 *   in tests with exactly two modules in it
 */
@TestInfrastructureInternals
class ModuleTransformerForSwitchingBackend(
    val backendForLib: TargetBackend,
    val backendForMain: TargetBackend
) : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure): TestModuleStructure {
        if (moduleStructure.modules.size != 2) error("Test should contain only one module")
        val (first, second) = moduleStructure.modules

        return TestModuleStructureImpl(
            listOf(
                first.copy(targetBackend = backendForLib),
                second.copy(targetBackend = backendForMain)
            ),
            moduleStructure.originalTestDataFiles
        )
    }
}
