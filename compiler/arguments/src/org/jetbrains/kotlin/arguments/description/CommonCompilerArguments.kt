/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.defaultTrue
import org.jetbrains.kotlin.arguments.dsl.types.*
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.LanguageFeature

val actualCommonCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonCompilerArguments) {
    compilerArgument {
        name = "language-version"
        description = "Provide source compatibility with the specified version of Kotlin.".asReleaseDependent()
        argumentType = KotlinVersionType(
            defaultValue = null.asReleaseDependent()
        )
        argumentTypeDescription = "<version>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_3,
            stabilizedVersion = KotlinReleaseVersion.v1_0_3,
        )
    }

    compilerArgument {
        name = "api-version"
        description = "Allow using declarations from only the specified version of bundled libraries.".asReleaseDependent()
        argumentType = KotlinVersionType(
            defaultValue = null.asReleaseDependent()
        )
        argumentTypeDescription = "<version>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_5,
            stabilizedVersion = KotlinReleaseVersion.v1_0_5,
        )
    }

    compilerArgument {
        name = "kotlin-home"
        description = "Path to the Kotlin compiler home directory used for the discovery of runtime libraries.".asReleaseDependent()
        argumentType = StringType.defaultNull
        argumentTypeDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_50,
            stabilizedVersion = KotlinReleaseVersion.v1_1_50,
        )
    }

    compilerArgument {
        name = "progressive"
        deprecatedName = "Xprogressive"
        compilerName = "progressiveMode"
        description = """
                Enable progressive compiler mode.
                In this mode, deprecations and bug fixes for unstable code take effect immediately
                instead of going through a graceful migration cycle.
                Code written in progressive mode is backward compatible; however, code written without
                progressive mode enabled may cause compilation errors in progressive mode.
                """.trimIndent().asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_50,
            stabilizedVersion = KotlinReleaseVersion.v1_3_0,
        )
    }

    compilerArgument {
        name = "script"
        description = "Evaluate the given Kotlin script (*.kts) file.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
            stabilizedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }

    compilerArgument {
        name = "Xrepl"
        compilerName = "repl"
        description = "Run Kotlin REPL (deprecated)".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "opt-in"
        deprecatedName = "Xopt-in"
        description =
            "Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        argumentTypeDescription = "<fq.name>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
            stabilizedVersion = KotlinReleaseVersion.v1_6_0,
        )
    }

    compilerArgument {
        name = "Xno-inline"
        description = "Disable method inlining.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "Xskip-metadata-version-check"
        description = "Allow loading classes with bad metadata versions and pre-release classes.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_2,
        )
    }

    compilerArgument {
        name = "Xskip-prerelease-check"
        description = "Allow loading pre-release classes.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }

    compilerArgument {
        name = "Xallow-kotlin-package"
        description =
            "Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    compilerArgument {
        name = "Xstdlib-compilation"
        description = "Enables special features which are relevant only for stdlib compilation.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }

    compilerArgument {
        name = "Xreport-output-files"
        description = "Report the source-to-output file mapping.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
        )
    }

    compilerArgument {
        name = "Xplugin"
        compilerName = "pluginClasspaths"
        description = "Load plugins from the given classpath.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        argumentTypeDescription = "<path>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "P"
        compilerName = "pluginOptions"
        description = "Pass an option to a plugin.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        argumentTypeDescription = "plugin:<pluginId>:<optionName>=<value>".asReleaseDependent()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0,
        )
    }

    compilerArgument {
        name = "Xcompiler-plugin"
        compilerName = "pluginConfigurations"
        description = "Register a compiler plugin.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        argumentTypeDescription = "<path1>,<path2>[=<optionName>=<value>,<optionName>=<value>]".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_8_0,
        )
    }

    compilerArgument {
        name = "Xcompiler-plugin-order"
        compilerName = "pluginOrderConstraints"
        description = """
            Specify an execution order constraint for compiler plugins.
            Order constraint can be specified using the 'pluginId' of compiler plugins.
            The first specified plugin will be executed before the second plugin.
            Multiple constraints can be specified by repeating this option. Cycles in constraints will cause an error.
            """.trimIndent().asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        argumentTypeDescription = "<pluginId1>><pluginId2>".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xmulti-platform"
        description = "Enable language support for multiplatform projects.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.MultiPlatformProjects))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_0,
        )
    }

    compilerArgument {
        name = "Xno-check-actual"
        description = "Do not check for the presence of the 'actual' modifier in multiplatform projects.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_60,
        )
    }

    compilerArgument {
        name = "Xintellij-plugin-root"
        description =
            "Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.".asReleaseDependent()
        argumentTypeDescription = "<path>".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_1_3,
        )
    }

    compilerArgument {
        name = "Xnew-inference"
        description = "Enable the new experimental generic type inference algorithm.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.NewInference),
            Enables(LanguageFeature.SamConversionPerArgument),
            Enables(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType),
            Enables(LanguageFeature.DisableCompatibilityModeForNewInference),
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_20,
        )
    }

    compilerArgument {
        name = "Xinline-classes"
        description = "Enable experimental inline classes.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.InlineClasses)
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_50,
        )
    }

    compilerArgument {
        name = "Xreport-perf"
        description = "Report detailed performance statistics.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_50,
        )
    }

    compilerArgument {
        name = "Xdetailed-perf"
        description = ("Enable more detailed performance statistics (Experimental).\n" +
                "For Native, the performance report includes execution time and lines processed per second for every individual lowering.\n" +
                "For WASM and JS, the performance report includes execution time and lines per second for each lowering of the first stage of compilation.").asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xdump-perf"
        description =
            ("Dump detailed performance statistics to the specified file in plain text, JSON or markdown format (it's detected by the file's extension).\n" +
                    "Also, it supports the placeholder `*` and directory for generating file names based on the module being compiled and the current time stamp.\n" +
                    "Example: `path/to/dir/*.log` creates logs like `path/to/dir/my-module_2025-06-20-12-22-32.log` in plain text format, `path/to/dir/` creates logs like `path/to/dir/my-log_2025-06-20-12-22-32.json`.").asReleaseDependent()
        argumentTypeDescription = "<path>".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_50,
        )
    }

    compilerArgument {
        name = "XXdump-model"
        compilerName = "dumpArgumentsDir"
        description = "Dump compilation model to specified directory for use in modularized tests.".asReleaseDependent()
        argumentTypeDescription = "<dir>".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
        )
    }


    compilerArgument {
        name = "Xmetadata-version"
        description = "Change the metadata version of the generated binary files.".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_70,
        )
    }


    compilerArgument {
        name = "Xcommon-sources"
        description = """
                Sources of the common module that need to be compiled together with this module in multiplatform mode.
                They should be a subset of sources passed as free arguments.
                """.trimIndent().asReleaseDependent()
        argumentTypeDescription = "<path>".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_2_70,
        )
    }


    compilerArgument {
        name = "Xlist-phases"
        description = "List backend phases.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_20,
        )
    }


    compilerArgument {
        name = "Xdisable-phases"
        description = "Disable backend phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_20,
        )
    }


    compilerArgument {
        name = "Xverbose-phases"
        description = "Be verbose while performing the given backend phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_20,
        )
    }


    compilerArgument {
        name = "Xphases-to-dump-before"
        description = "Dump the backend's state before these phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_20,
        )
    }


    compilerArgument {
        name = "Xphases-to-dump-after"
        description = "Dump the backend's state after these phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_20,
        )
    }


    compilerArgument {
        name = "Xphases-to-dump"
        description = "Dump the backend's state both before and after these phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_20,
        )
    }


    compilerArgument {
        name = "Xdump-directory"
        description = "Dump the backend state into this directory.".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_50,
        )
    }


    compilerArgument {
        name = "Xdump-fqname"
        compilerName = "dumpOnlyFqName"
        description = "Dump the declaration with the given FqName.".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_50,
        )
    }


    compilerArgument {
        name = "Xphases-to-validate-before"
        description = "Validate the backend's state before these phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }


    compilerArgument {
        name = "Xphases-to-validate-after"
        description = "Validate the backend's state after these phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }


    compilerArgument {
        name = "Xphases-to-validate"
        description = "Validate the backend's state both before and after these phases.".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }


    compilerArgument {
        name = "Xverify-ir"
        description = "IR verification mode (no verification by default).".asReleaseDependent()
        argumentTypeDescription = "{none|warning|error}".asReleaseDependent()
        argumentType = StringType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }


    compilerArgument {
        name = "Xverify-ir-visibility"
        description =
            "Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }


    compilerArgument {
        name = "Xverify-ir-nested-offsets"
        description =
            "Check that offsets of nested IR elements conform to offsets of their containers. Only has effect if '-Xverify-ir' is not 'none'.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_20,
        )
    }


    compilerArgument {
        name = "Xprofile-phases"
        description = "Profile backend phases.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_20,
        )
    }


    compilerArgument {
        name = "Xcheck-phase-conditions"
        description = "Check pre- and postconditions of IR lowering phases.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_40,
        )
    }

    compilerArgument {
        name = "Xuse-fir-experimental-checkers"
        description = "Enable experimental frontend IR checkers that are not yet ready for production.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Deprecated("This flag is deprecated")
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
            deprecatedVersion = KotlinReleaseVersion.v2_2_20,
        )
    }


    compilerArgument {
        name = "Xuse-fir-ic"
        compilerName = "useFirIC"
        description =
            "Compile using frontend IR internal incremental compilation.\nWarning: This feature is not yet production-ready.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
        )
    }


    compilerArgument {
        name = "Xuse-fir-lt"
        compilerName = "useFirLT"
        description = "Compile using the LightTree parser with the frontend IR.".asReleaseDependent()
        argumentType = BooleanType.defaultTrue

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
        )
    }


    compilerArgument {
        name = "Xmetadata-klib"
        deprecatedName = "Xexpect-actual-linker"
        description = "Produce a klib that only contains the metadata of declarations.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }


    compilerArgument {
        name = "Xdisable-default-scripting-plugin"
        description = "Don't enable the scripting plugin by default.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }


    compilerArgument {
        name = "Xexplicit-api"
        description = """Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
Use the 'warning' level to issue warnings instead of errors.""".asReleaseDependent()
        argumentTypeDescription = ReleaseDependent(
            current = ExplicitApiMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}")
        )
        argumentType = KotlinExplicitApiModeType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_3_70,
        )
    }


    compilerArgument {
        name = "XXexplicit-return-types"
        description = """Force the compiler to report errors on all public API declarations without an explicit return type.
Use the 'warning' level to issue warnings instead of errors.
This flag partially enables functionality of `-Xexplicit-api` flag, so please don't use them altogether""".asReleaseDependent()
        argumentTypeDescription = ReleaseDependent(
            current = ExplicitApiMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}")
        )
        argumentType = KotlinExplicitApiModeType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }


    compilerArgument {
        name = "Xreturn-value-checker"
        description =
            "Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.".asReleaseDependent()
        argumentTypeDescription = ReleaseDependent(
            current = ReturnValueCheckerMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}") { it.modeState }
        )
        argumentType = ReturnValueCheckerModeType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }


    compilerArgument {
        name = "Xsuppress-version-warnings"
        description = "Suppress warnings about outdated, inconsistent, or experimental language or API versions.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_0,
        )
    }


    compilerArgument {
        name = "Xsuppress-api-version-greater-than-language-version-error"
        description =
            "Suppress error about API version greater than language version.\nWarning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }


    compilerArgument {
        name = "Xexpect-actual-classes"
        description =
            """'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.""".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_20,
        )
    }


    compilerArgument {
        name = "Xconsistent-data-class-copy-visibility"
        description =
            "The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.DataClassCopyRespectsConstructorVisibility))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }


    compilerArgument {
        name = "Xunrestricted-builder-inference"
        description =
            "Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.UnrestrictedBuilderInference))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_5_30,
        )
    }


    compilerArgument {
        name = "Xcontext-receivers"
        description = "Enable experimental context receivers.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ContextReceivers))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_6_20,
        )
    }


    compilerArgument {
        name = "Xcontext-parameters"
        description = "Enable experimental context parameters.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ContextParameters))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }


    compilerArgument {
        name = "Xexplicit-context-arguments"
        description = "Enable explicit passing of context arguments using named argument syntax.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ExplicitContextArguments))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0,
        )
    }


    compilerArgument {
        name = "Xcontext-sensitive-resolution"
        description = "Enable experimental context-sensitive resolution.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ContextSensitiveResolutionUsingExpectedType))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }


    compilerArgument {
        name = "Xnon-local-break-continue"
        description = "Enable experimental non-local break and continue.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.BreakContinueInInlineLambdas))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
        )
    }


    compilerArgument {
        name = "Xdata-flow-based-exhaustiveness"
        description = "Enable `when` exhaustiveness improvements that rely on data-flow analysis.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.DataFlowBasedExhaustiveness))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20,
        )
    }


    compilerArgument {
        name = "Xexplicit-backing-fields"
        description = "Enable experimental language support for explicit backing fields.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ExplicitBackingFields))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }


    compilerArgument {
        name = "Xdirect-java-actualization"
        description = "Enable experimental direct Java actualization support.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.DirectJavaActualization))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
        )
    }


    compilerArgument {
        name = "Xmulti-dollar-interpolation"
        description = "Enable experimental multi-dollar interpolation.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.MultiDollarInterpolation))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }


    compilerArgument {
        name = "Xenable-incremental-compilation"
        compilerName = "incrementalCompilation"
        description = "Enable incremental compilation.".asReleaseDependent()
        argumentType = BooleanType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
        )
    }


    compilerArgument {
        name = "Xrender-internal-diagnostic-names"
        description = "Render the internal names of warnings and errors.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_0,
        )
    }


    compilerArgument {
        name = "Xallow-any-scripts-in-source-roots"
        description = "Allow compiling scripts along with regular Kotlin sources.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Disables(LanguageFeature.SkipStandaloneScriptsInSourceRoots)
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_7_20,
        )
    }


    compilerArgument {
        name = "Xreport-all-warnings"
        description = "Report all warnings even if errors are found.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }


    compilerArgument {
        name = "Xfragments"
        description = "Declare all known fragments of a multiplatform compilation.".asReleaseDependent()
        argumentTypeDescription = "<fragment name>".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
        )
    }


    compilerArgument {
        name = "Xfragment-sources"
        description = "Add sources to a specific fragment of a multiplatform compilation.".asReleaseDependent()
        argumentTypeDescription = "<fragment name>:<path>".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
        )
    }


    compilerArgument {
        name = "Xfragment-refines"
        description = "Declare that <fromModuleName> refines <onModuleName> with the dependsOn/refines relation.".asReleaseDependent()
        argumentTypeDescription = "<fromModuleName>:<onModuleName>".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
        )
    }

    compilerArgument {
        name = "Xfragment-dependency"
        compilerName = "fragmentDependencies"
        argumentTypeDescription = "<fragment name>:<path>".asReleaseDependent()
        description = """Declare common klib dependencies for the specific fragment.
This argument is required for any HMPP module except the platform leaf module: it takes dependencies from -cp/-libraries.
The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
""".asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        delimiter = KotlinCompilerArgument.Delimiter.None

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20,
        )
    }

    compilerArgument {
        name = "Xfragment-friend-dependency"
        compilerName = "fragmentFriendDependencies"
        argumentTypeDescription = "<fragment name>:<path>".asReleaseDependent()
        description = """Declare common klib friend dependencies for the specific fragment.
This argument can be specified for any HMPP module except the platform leaf module: it takes dependencies from the platform specific friend module arguments.
The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
""".asReleaseDependent()
        argumentType = StringArrayType.defaultNull
        delimiter = KotlinCompilerArgument.Delimiter.None

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xseparate-kmp-compilation"
        compilerName = "separateKmpCompilationScheme"
        description = "Enables the separated compilation scheme, in which common source sets are analyzed against their own dependencies".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20,
        )
    }


    compilerArgument {
        name = "Xignore-const-optimization-errors"
        description = "Ignore all compilation exceptions while optimizing some constant expressions.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_9_0,
        )
    }


    compilerArgument {
        name = "Xdont-warn-on-error-suppression"
        description = "Don't report warnings when errors are suppressed. This only affects K2.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
        )
    }


    compilerArgument {
        name = "Xwhen-guards"
        description = "Enable experimental language support for when guards.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.WhenGuards)
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }


    compilerArgument {
        name = "Xnested-type-aliases"
        description = "Enable experimental language support for nested type aliases.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.NestedTypeAliases)
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "Xlocal-type-aliases"
        description = "Enable experimental language support for local type aliases.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.LocalTypeAliases)
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0,
        )
    }

    compilerArgument {
        name = "Xsuppress-warning"
        compilerName = "suppressedDiagnostics"
        description =
            "Suppress specified warning module-wide. This option is deprecated in favor of \"-Xwarning-level\" flag".asReleaseDependent()
        argumentTypeDescription = "<WARNING_NAME>".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_0,
        )
    }


    compilerArgument {
        name = "Xwarning-level"
        compilerName = "warningLevels"
        description = """Set the severity of the given warning.
- `error` level raises the severity of a warning to error level (similar to -Werror but more granular)
- `disabled` level suppresses reporting of a warning (similar to -nowarn but more granular)
- `warning` level overrides -nowarn and -Werror for this specific warning (the warning will be reported/won't be considered as an error)""".asReleaseDependent()
        argumentTypeDescription = "<WARNING_NAME>:(error|warning|disabled)".asReleaseDependent()
        argumentType = StringArrayType.defaultNull

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }


    compilerArgument {
        name = "Xannotation-default-target"
        description = """Change the default annotation targets for constructor properties:
-Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:';
-Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;
-Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';
default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.""".asReleaseDependent()
        argumentTypeDescription = "first-only|first-only-warn|param-property".asReleaseDependent()
        argumentType = StringType.defaultNull
        additionalAnnotations(
            Disables(LanguageFeature.AnnotationDefaultTargetMigrationWarning, "first-only"),
            Enables(LanguageFeature.AnnotationDefaultTargetMigrationWarning, "first-only-warn"),

            Disables(LanguageFeature.PropertyParamAnnotationDefaultTargetMode, "first-only"),
            Disables(LanguageFeature.PropertyParamAnnotationDefaultTargetMode, "first-only-warn"),

            Enables(LanguageFeature.PropertyParamAnnotationDefaultTargetMode, "param-property"),
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }


    compilerArgument {
        name = "XXdebug-level-compiler-checks"
        description =
            "Enable debug level compiler checks. ATTENTION: these checks can slow compiler down or even crash it.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }


    compilerArgument {
        name = "Xannotation-target-all"
        description = "Enable experimental language support for @all: annotation use-site target.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.AnnotationAllUseSiteTarget))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_1_20,
        )
    }

    compilerArgument {
        name = "XXlenient-mode"
        description = "Lenient compiler mode. When actuals are missing, placeholder declarations are generated.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }

    compilerArgument {
        name = "Xallow-reified-type-in-catch"
        description = "Allow 'catch' parameters to have reified types.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.AllowReifiedTypeInCatchClause))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20
        )
    }

    compilerArgument {
        name = "Xallow-contracts-on-more-functions"
        description = "Allow contracts on some operators and accessors, and allow checks for erased types.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.AllowCheckForErasedTypesInContracts),
            Enables(LanguageFeature.AllowContractsOnSomeOperators),
            Enables(LanguageFeature.AllowContractsOnPropertyAccessors),
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20
        )
    }

    compilerArgument {
        name = "Xallow-condition-implies-returns-contracts"
        description = "Allow contracts that specify a limited conditional returns postcondition.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ConditionImpliesReturnsContracts))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20
        )
    }

    compilerArgument {
        name = "Xallow-holdsin-contract"
        description = "Allow contracts that specify a condition that holds true inside a lambda argument.".asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.HoldsInContracts))

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_20
        )
    }

    compilerArgument {
        name = "Xname-based-destructuring"
        description = """Enables the following destructuring features:
-Xname-based-destructuring=only-syntax:   Enables syntax for positional destructuring with square brackets and the full form of name-based destructuring with parentheses;
-Xname-based-destructuring=name-mismatch: Reports warnings when short form positional destructuring of data classes uses names that don't match the property names;
-Xname-based-destructuring=complete:      Enables short-form name-based destructuring with parentheses;""".asReleaseDependent()
        argumentTypeDescription = "only-syntax|name-mismatch|complete".asReleaseDependent()
        argumentType = StringType.defaultNull
        additionalAnnotations(
            Enables(LanguageFeature.NameBasedDestructuring, "only-syntax"),
            Enables(LanguageFeature.NameBasedDestructuring, "name-mismatch"),
            Enables(LanguageFeature.NameBasedDestructuring, "complete"),
            Enables(LanguageFeature.DeprecateNameMismatchInShortDestructuringWithParentheses, "name-mismatch"),
            Enables(LanguageFeature.DeprecateNameMismatchInShortDestructuringWithParentheses, "complete"),
            Enables(LanguageFeature.EnableNameBasedDestructuringShortForm, "complete"),
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_0
        )
    }

    compilerArgument {
        name = "XXLanguage"
        description = """Enables/disables specified language feature.
Warning: this flag is not intended for production use. If you want to configure the language behaviour use the
-language-version or corresponding experimental feature flags.
        """.trimIndent().asReleaseDependent()
        argumentTypeDescription = "[+-]LanguageFeatureName".asReleaseDependent()
        compilerName = "manuallyConfiguredFeatures"
        argumentType = StringArrayType.defaultNull
        delimiter = KotlinCompilerArgument.Delimiter.None
        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_0_0
        )
    }

    compilerArgument {
        name = "Xheader-mode"
        description = """
                Enable header compilation mode.
                In this mode, the compiler produces class files that only contain the 'skeleton' of the classes to be
                compiled but the method bodies of all the implementations are empty.  This is used to speed up parallel compilation
                build systems where header libraries can be used to replace downstream dependencies for which we only need to
                see the type names and method signatures required to compile a given translation unit. Inline functions are still kept
                with bodies.
                """.trimIndent().asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_20
        )
    }

    compilerArgument {
        name = "Xheader-mode-type"
        description = """Generates output based on what it is used for:
-Xheader-mode-type=compilation: Skips the IR generation for modules that don't have inline functions.
-Xheader-mode-type=any: Can be used for any downstream dependency which doesn't require linking.
                """.trimIndent().asReleaseDependent()
        argumentType = KotlinHeaderModeType()
        argumentTypeDescription = ReleaseDependent(
            current = HeaderMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}")
        )

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0
        )
    }

    compilerArgument {
        name = "Xdont-sort-source-files"
        description = """
            Disable automatic sorting of source files.
        """.trimIndent().asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_3_20
        )
    }

    compilerArgument {
        name = "Xprint-configuration"
        description = """
            Print compiler configuration.
        """.trimIndent().asReleaseDependent()
        argumentType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_4_0
        )
    }
}
