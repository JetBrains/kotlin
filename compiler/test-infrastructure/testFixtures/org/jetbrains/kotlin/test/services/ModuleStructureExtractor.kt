/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.test.TestInfrastructureInternals
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer

abstract class ModuleStructureExtractor @OptIn(TestInfrastructureInternals::class) constructor(
    protected val testServices: TestServices,
    protected val additionalSourceProviders: List<AdditionalSourceProvider>,
    protected val moduleStructureTransformers: List<ModuleStructureTransformer>
) {
    abstract fun splitTestDataByModules(
        testDataFileName: String,
        directivesContainer: DirectivesContainer,
    ): TestModuleStructure

    companion object {
        const val DEFAULT_MODULE_NAME = "main"
        val CINTEROP_SOURCE_EXTENSIONS = setOf("c", "cpp", "m", "mm")
    }
}

/**
 * The prefix which [org.jetbrains.kotlin.test.directives.ModuleStructureDirectives.ESCAPE_MODULE_NAME] adds to the names
 * of the modules of the current test, so that the modules of different tests of a grouped batch don't clash.
 */
val TestServices.escapedModuleNamePrefix: String
    get() {
        (val className, val methodName, val _ = tags) = testInfo
        val classPart = className.substringAfter("$").replace("$", ".")
        return "$classPart.$methodName."
    }
