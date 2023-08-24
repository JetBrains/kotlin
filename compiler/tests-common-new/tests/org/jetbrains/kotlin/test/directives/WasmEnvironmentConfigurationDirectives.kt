/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object WasmEnvironmentConfigurationDirectives : SimpleDirectivesContainer() {
    val RUN_UNIT_TESTS by directive(
        description = "Run kotlin.test unit tests (function marked with @Test)",
    )

    val DISABLE_WASM_EXCEPTION_HANDLING by directive(
        description = "Generate wasm without EH proposal and test in runtime with EH turned off",
    )

    // Next directives are used only inside test system and must not be present in test file

    val PATH_TO_TEST_DIR by stringDirective(
        description = "Specify the path to directory with test files. " +
                "This path is used to copy hierarchy from test file to test dir and use the same hierarchy in output dir.",
        applicability = DirectiveApplicability.Global
    )

    val PATH_TO_ROOT_OUTPUT_DIR by stringDirective(
        description = "Specify the path to output directory, where all artifacts will be stored",
        applicability = DirectiveApplicability.Global
    )

    val TEST_GROUP_OUTPUT_DIR_PREFIX by stringDirective(
        description = "Specify the prefix directory for output directory that will contains artifacts",
        applicability = DirectiveApplicability.Global
    )

    val GENERATE_SOURCE_MAP by directive(
        description = "Enables generation of source map",
        applicability = DirectiveApplicability.Global
    )
}
