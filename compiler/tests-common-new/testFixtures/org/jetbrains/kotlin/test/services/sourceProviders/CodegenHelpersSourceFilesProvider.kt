/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services.sourceProviders

import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.AdditionalSourceProvider
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider

class CodegenHelpersSourceFilesProvider(testServices: TestServices) : AdditionalSourceProvider(testServices) {
    companion object {
        private val HELPERS_PATH = "codegen/helpers"
        private val CLASSIC_BACKEND_PATH = "$HELPERS_PATH/CodegenTestHelpersOldBackend.kt"
        private val IR_BACKEND_PATH = "$HELPERS_PATH/CodegenTestHelpersIR.kt"
    }

    override val directiveContainers: List<DirectivesContainer> =
        listOf(CodegenTestDirectives)

    @OptIn(ExperimentalStdlibApi::class)
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure
    ): List<TestFile> {
        if (CodegenTestDirectives.WITH_HELPERS !in module.directives) return emptyList()
        val targetBackend = testServices.defaultsProvider.targetBackend ?: return emptyList()
        return buildList {
            val helpersPath = if (targetBackend.isIR) {
                IR_BACKEND_PATH
            } else {
                CLASSIC_BACKEND_PATH
            }
            add(this::class.java.classLoader.getResource(helpersPath)!!.toTestFile())
        }
    }
}
