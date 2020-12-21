/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.directives.AdditionalFilesDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import java.io.File

class AdditionalDiagnosticsSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private const val HELPERS_PATH = "./compiler/testData/diagnostics/helpers"
        private const val CHECK_TYPE_PATH = "$HELPERS_PATH/types/checkType.kt"
    }

    override val directives: List<DirectivesContainer> =
        listOf(AdditionalFilesDirectives)

    @OptIn(ExperimentalStdlibApi::class)
    override fun produceAdditionalFiles(globalDirectives: RegisteredDirectives, module: TestModule): List<TestFile> {
        return buildList {
            if (containsDirective(globalDirectives, module, AdditionalFilesDirectives.CHECK_TYPE)) {
                add(File(CHECK_TYPE_PATH).toTestFile())
            }
        }
    }
}
