/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.directives

import org.jetbrains.kotlin.config.ConstraintsDumpFormat
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives.FIR_PARSER
import org.jetbrains.kotlin.test.directives.model.DirectiveApplicability.Global
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirResolvedTypesVerifier
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirScopeDumpHandler

object FirDiagnosticsDirectives : SimpleDirectivesContainer() {
    val DUMP_CFG by stringDirective(
        description = """
            Dumps control flow graphs of all declarations to `testName.dot` file
            This directive may be applied only to all modules.
            Syntax: DUMP_CFG(: [OPTIONS])
            
            Additional options may be enabled :
             - ${DumpCfgOption.LEVELS}: Render levels of nodes in CFG dump.
             - ${DumpCfgOption.FLOW}: Include data analysis variable information in CFG dump for debugging purposes.
        """.trimIndent(),
    )

    val DUMP_VFIR by directive(
        description = """
            Dumps verbose FIR representation to `testName.vfir` file
        """.trimIndent(),
        applicability = Global
    )

    val FIR_DUMP by directive(
        description = """
            Dumps resulting fir to `testName.fir.txt` file
        """.trimIndent(),
        applicability = Global
    )

    val FIR_IDENTICAL by directive(
        description = "Contents of fir test data file and FE 1.0 are identical",
        applicability = Global
    )

    val LATEST_LV_DIFFERENCE by directive(
        description = """
            Diagnostics differs between latest stable and latest language version
            Separate file for latest LV should be checked
        """.trimIndent()
    )

    val TEST_ALONGSIDE_K1_TESTDATA by directive(
        description = """
            This directive indicates that the test is run on the testdata,
            which is also used for K1 tests
        """.trimIndent()
    )

    val USE_LATEST_LANGUAGE_VERSION by directive(
        description = """
            Indicates that test is run with latest language version.
            This directive should not be used directly in testdata
        """.trimIndent()
    )

    val FIR_PARSER by enumDirective<FirParser>(
        description = "Defines which parser should be used for FIR compiler"
    )

    val RENDER_DIAGNOSTICS_MESSAGES by directive(
        description = "Forces diagnostic arguments to be rendered"
    )

    val FIR_DISABLE_LAZY_RESOLVE_CHECKS by directive(
        description = "Temporary disables lazy resolve checks until the lazy resolve contract violation is fixed"
    )

    val COMPARE_WITH_LIGHT_TREE by directive(
        description = "Enable comparing diagnostics between PSI and light tree modes",
        applicability = Global
    )

    val WITH_EXTRA_CHECKERS by directive(
        description = "Enable extra checkers"
    )

    val WITH_EXPERIMENTAL_CHECKERS by directive(
        description = "Enable experimental checkers"
    )

    val DUMP_CONSTRAINTS by enumDirective<ConstraintsDumpFormat>(
        description = "Render the constraints dump file"
    )

    val DISABLE_JAVA_FACADE by directive(
        description = "Disables javac for diagnostic tests containing incorrect Java code. Such tests must be fixed, but until they are, use this directive"
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

    val PLATFORM_DEPENDANT_METADATA by directive(
        description = """
            Generate separate dumps for JVM and JS load compiled kotlin tests
            See AbstractLoadedMetadataDumpHandler
        """
    )

    val RENDER_FIR_DECLARATION_ATTRIBUTES by directive(
        description = """
            Prints declaration attributes to dumps in load compiled kotlin tests
        """
    )

    val SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE by stringDirective(
        description = """
            Suppresses AbstractFirLightTreeDiagnosticsWithoutAliasExpansionTest
        """
    )

    val DISABLE_DOUBLE_CHECKING_COMMON_DIAGNOSTICS by directive(
        description = "Disables additional run of platform checkers in common environment"
    )

    val FIXATION_LOGS by directive(
        description = """
            Enables collection of fixation logs for K2 diagnostics tests
        """
    )
}

object DumpCfgOption {
    const val LEVELS = "LEVELS"
    const val FLOW = "FLOW"
}

fun TestConfigurationBuilder.configureFirParser(parser: FirParser) {
    defaultDirectives {
        FIR_PARSER with parser
    }
}
