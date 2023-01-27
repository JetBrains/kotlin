/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.Global
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler

object FirDiagnosticsDirectives : SimpleDirectivesContainer() {
    val DUMP_CFG by directive(
        description = """
            Dumps control flow graphs of all declarations to `testName.dot` file
            This directive may be applied only to all modules
        """.trimIndent(),
        applicability = Global
    )

    val RENDERER_CFG_LEVELS by directive(
        description = "Render leves of nodes in CFG dump",
        applicability = Global
    )

    val FIR_DUMP by directive(
        description = """
            Dumps resulting fir to `testName.fir` file
        """.trimIndent(),
        applicability = Global
    )

    val FIR_IDENTICAL by directive(
        description = "Contents of fir test data file and FE 1.0 are identical",
        applicability = Global
    )

    val USE_LIGHT_TREE by directive(
        description = "Enables light tree parser instead of PSI"
    )

    val RENDER_DIAGNOSTICS_MESSAGES by directive(
        description = "Forces diagnostic arguments to be rendered"
    )

    val FIR_DISABLE_LAZY_RESOLVE_CHECKS by directive(
        description = "Temporary disables lazy resolve checks until the lazy resolve contract violation is fixed"
    )

    val COMPARE_WITH_LIGHT_TREE by directive(
        description = """
            Enable comparing diagnostics between PSI and light tree modes
            For enabling light tree mode use $USE_LIGHT_TREE directive
        """.trimIndent(),
        applicability = Global
    )

    val WITH_EXTENDED_CHECKERS by directive(
        description = "Enable extended checkers"
    )

    val SCOPE_DUMP by stringDirective(
        description = """
            Dump hierarchies of overrides of classes listed in arguments
            Syntax: SCOPE_DUMP: some.package.ClassName:foo;bar, some.package.OtherClass
                                            ^^^                           ^^^
                             members foo and bar from ClassName            |
                                                                all members from OtherClass
            Enables ${FirScopeDumpHandler::class}
        """.trimIndent()
    )

    val ENABLE_PLUGIN_PHASES by directive(
        description = "Enable plugin phases"
    )

    val IGNORE_LEAKED_INTERNAL_TYPES by stringDirective(
        description = """
            Ignore failures in ${FirResolvedTypesVerifier::class}.
            Directive must contain description of ignoring in argument
        """.trimIndent()
    )
}
