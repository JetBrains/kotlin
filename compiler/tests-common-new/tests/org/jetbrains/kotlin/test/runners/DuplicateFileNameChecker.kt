/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.services.DefaultsProvider
import org.jetbrains.kotlin.test.services.ModuleStructureTransformer
import org.jetbrains.kotlin.test.services.TestModuleStructure

// TODO remove when duplicate files names are supported by prefix their path with the module name KT-63252
@OptIn(TestInfrastructureInternals::class)
object DuplicateFileNameChecker : ModuleStructureTransformer() {
    override fun transformModuleStructure(moduleStructure: TestModuleStructure, defaultsProvider: DefaultsProvider): TestModuleStructure {
        val files = mutableSetOf<String>()

        for (module in moduleStructure.modules) {
            for (file in module.files) {
                if (!files.add(file.name)) {
                    throw IllegalStateException("Duplicate file name: ${file.name}")
                }
            }
        }

        return moduleStructure
    }
}
