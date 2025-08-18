/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework

import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule

object AnalysisApiTestDirectives : SimpleDirectivesContainer() {
    val MODULE_KIND by enumDirective<TestModuleKind>(
        "Overrides the kind of `KaModule` that is built from the associated test module",
        applicability = DirectiveApplicability.Module,
    )

    val DISABLE_DEPENDED_MODE by directive("Analysis in dependent mode should not be run in this test")
    val IGNORE_FE10 by directive("FE10 Analysis API implementation test should not be run")
    val IGNORE_FIR by directive("FIR Analysis API implementation test should not be run")
    val IGNORE_STANDALONE by directive("Standalone implementation test should not be run")

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
        description = "Specifies the module name used to find the 'context psi element' for this module",
        applicability = DirectiveApplicability.Module
    )

    /*
    Note: the 'contextElement' can be different from the 'contextModule'.
    E.g., consider a multiplatform project where the contextElement is in 'commonMain', but the contextModule can be
    configured as 'jvmMain'
    */
    val ANALYSIS_CONTEXT_MODULE by stringDirective(
        description = "Specifies the module name which should be treated as a context module for the current one (can overwrite 'CONTEXT_MODULE')",
        applicability = DirectiveApplicability.Module
    )

    /**
     * When applied to a library (source) module, specifies that the library module should depend on a [KaLibraryFallbackDependenciesModule][org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryFallbackDependenciesModule]
     * instead of the regular dependencies set by the test infrastructure.
     *
     * A [TestModule] with fallback dependencies cannot also have explicit dependencies. This is checked by the test infrastructure.
     *
     * Library fallback dependencies aren't materialized as test modules by design, for the following reasons:
     *
     * 1. We cannot create a fallback dependencies module on its own, since it needs to be tied to its `dependentLibrary` immediately. A
     *    test module should ideally be able to exist on its own.
     * 2. Only one library module should depend on the fallback dependencies, but any number of modules can depend on a test module.
     * 3. Fallback dependencies are not resolvable, so we cannot create analysis sessions for them. This might be unexpected for a test
     *    module.
     */
    val FALLBACK_DEPENDENCIES by directive(
        description = "Specifies that the library module should depend on a fallback dependencies module instead of the regular dependencies set by the test infrastructure.",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * When applied to a library module, specifies that the module should be marked as an SDK.
     *
     * @see org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule.isSdk
     */
    val SDK_LIBRARY by directive(
        description = "Marks the library module as an SDK.",
        applicability = DirectiveApplicability.Module,
    )
}

val TestModule.hasFallbackDependencies: Boolean
    get() = directives.contains(AnalysisApiTestDirectives.FALLBACK_DEPENDENCIES)

val TestModule.isSdkLibrary: Boolean
    get() = directives.contains(AnalysisApiTestDirectives.SDK_LIBRARY)
