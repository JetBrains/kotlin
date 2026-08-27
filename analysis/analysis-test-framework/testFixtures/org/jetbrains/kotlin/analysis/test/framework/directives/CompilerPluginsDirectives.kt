/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.framework.directives

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

/**
 * Directives which enable the test compiler plugin from the `:plugins:plugin-sandbox` module.
 *
 * The container is registered by `configureOptionalTestCompilerPlugin`, which also configures the plugin for the analysis session.
 * Library modules are compiled by a separate CLI compiler invocation, so the plugin is passed to it by
 * [CliTestModuleCompiler][org.jetbrains.kotlin.analysis.test.framework.services.libraries.CliTestModuleCompiler].
 */
object CompilerPluginsDirectives : SimpleDirectivesContainer() {
    val WITH_FIR_TEST_COMPILER_PLUGIN by directive(
        description = "Configure test compiler plugin from :plugins:plugin-sandbox module",
        applicability = DirectiveApplicability.Global,
    )
}
