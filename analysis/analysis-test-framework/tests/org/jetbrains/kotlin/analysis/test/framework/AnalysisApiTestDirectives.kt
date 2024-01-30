/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object AnalysisApiTestDirectives : SimpleDirectivesContainer() {
    val MODULE_KIND by enumDirective<TestModuleKind>(
        "Overrides the kind of `KtModule` that is built from the associated test module",
        applicability = DirectiveApplicability.Module,
    )

    val DISABLE_DEPENDED_MODE by directive("Analysis in dependent mode should not be run in this test")
    val IGNORE_FE10 by directive("FE10 Analysis API implementation test should mot be run")
    val IGNORE_FIR by directive("FIR Analysis API implementation test should mot be run")

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest.findMainFile
     */
    val MAIN_FILE_NAME by stringDirective(
        description = "The name of the main file",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * @see org.jetbrains.kotlin.analysis.test.framework.base.AbstractAnalysisApiBasedTest.findMainModule
     */
    val MAIN_MODULE by directive(
        description = "Mark the module as main",
        applicability = DirectiveApplicability.Module,
    )

    val CONTEXT_MODULE by stringDirective(
        description = "Specifies the module name which should be treated as a context module for the current one",
        applicability = DirectiveApplicability.Module
    )
}
