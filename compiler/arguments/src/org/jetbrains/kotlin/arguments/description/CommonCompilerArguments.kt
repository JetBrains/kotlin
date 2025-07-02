/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.description

import org.jetbrains.kotlin.arguments.dsl.TemporaryCompilerArgumentLifecycle
import org.jetbrains.kotlin.arguments.dsl.base.*
import org.jetbrains.kotlin.arguments.dsl.defaultFalse
import org.jetbrains.kotlin.arguments.dsl.defaultNull
import org.jetbrains.kotlin.arguments.dsl.defaultTrue
import org.jetbrains.kotlin.arguments.dsl.stubLifecycle
import org.jetbrains.kotlin.arguments.dsl.types.*
import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion

val actualCommonCompilerArguments by compilerArgumentsLevel(CompilerArgumentsLevelNames.commonCompilerArguments) {
    compilerArgument {
        name = "language-version"
        description = "Provide source compatibility with the specified version of Kotlin.".asReleaseDependent()
        valueType = KotlinVersionType(
            defaultValue = null.asReleaseDependent()
        )
        valueDescription = "<version>".asReleaseDependent()

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.LANGUAGE_VERSIONS,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "api-version"
        description = "Allow using declarations from only the specified version of bundled libraries.".asReleaseDependent()
        valueType = KotlinVersionType(
            defaultValue = null.asReleaseDependent()
        )
        valueDescription = "<version>".asReleaseDependent()

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.API_VERSIONS,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = true,
            )
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "kotlin-home"
        description = "Path to the Kotlin compiler home directory used for the discovery of runtime libraries.".asReleaseDependent()
        valueType = StringType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
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
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT
            )
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "script"
        description = "Evaluate the given Kotlin script (*.kts) file.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xrepl"
        compilerName = "repl"
        description = "Run Kotlin REPL (deprecated)".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "opt-in"
        deprecatedName = "Xopt-in"
        description =
            "Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<fq.name>".asReleaseDependent()

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.EMPTY_STRING_ARRAY_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT
            )
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xno-inline"
        description = "Disable method inlining.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xskip-metadata-version-check"
        description = "Allow loading classes with bad metadata versions and pre-release classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xskip-prerelease-check"
        description = "Allow loading pre-release classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xallow-kotlin-package"
        description =
            "Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xstdlib-compilation"
        description = "Enables special features which are relevant only for stdlib compilation.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xreport-output-files"
        description = "Report the source-to-output file mapping.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xplugin"
        compilerName = "pluginClasspaths"
        description = "Load plugins from the given classpath.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path>".asReleaseDependent()

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "P"
        compilerName = "pluginOptions"
        description = "Pass an option to a plugin.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "plugin:<pluginId>:<optionName>=<value>".asReleaseDependent()

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xcompiler-plugin"
        compilerName = "pluginConfigurations"
        description = "Register a compiler plugin.".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        valueDescription = "<path1>,<path2>[=<optionName>=<value>,<optionName>=<value>]".asReleaseDependent()
        delimiter = KotlinCompilerArgument.Delimiter.None

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xmulti-platform"
        description = "Enable language support for multiplatform projects.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.MultiPlatformProjects))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xno-check-actual"
        description = "Do not check for the presence of the 'actual' modifier in multiplatform projects.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xintellij-plugin-root"
        description =
            "Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.".asReleaseDependent()
        valueDescription = "<path>".asReleaseDependent()
        valueType = StringType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xnew-inference"
        description = "Enable the new experimental generic type inference algorithm.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.NewInference),
            Enables(LanguageFeature.SamConversionPerArgument),
            Enables(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType),
            Enables(LanguageFeature.DisableCompatibilityModeForNewInference),
        )
        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xinline-classes"
        description = "Enable experimental inline classes.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.InlineClasses)
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xreport-perf"
        description = "Report detailed performance statistics.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "Xdump-perf"
        description = "Dump detailed performance statistics to the specified file.".asReleaseDependent()
        valueDescription = "<path>".asReleaseDependent()
        valueType = StringType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xmetadata-version"
        description = "Change the metadata version of the generated binary files.".asReleaseDependent()
        valueType = StringType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xcommon-sources"
        description = """
                Sources of the common module that need to be compiled together with this module in multiplatform mode.
                They should be a subset of sources passed as free arguments.
                """.trimIndent().asReleaseDependent()
        valueDescription = "<path>".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xlist-phases"
        description = "List backend phases.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xdisable-phases"
        description = "Disable backend phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xverbose-phases"
        description = "Be verbose while performing the given backend phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xphases-to-dump-before"
        description = "Dump the backend's state before these phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xphases-to-dump-after"
        description = "Dump the backend's state after these phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xphases-to-dump"
        description = "Dump the backend's state both before and after these phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xdump-directory"
        description = "Dump the backend state into this directory.".asReleaseDependent()
        valueType = StringType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xdump-fqname"
        compilerName = "dumpOnlyFqName"
        description = "Dump the declaration with the given FqName.".asReleaseDependent()
        valueType = StringType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xphases-to-validate-before"
        description = "Validate the backend's state before these phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xphases-to-validate-after"
        description = "Validate the backend's state after these phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xphases-to-validate"
        description = "Validate the backend's state both before and after these phases.".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xverify-ir"
        description = "IR verification mode (no verification by default).".asReleaseDependent()
        valueDescription = "{none|warning|error}".asReleaseDependent()
        valueType = StringType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xverify-ir-visibility"
        description =
            "Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xprofile-phases"
        description = "Profile backend phases.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xcheck-phase-conditions"
        description = "Check pre- and postconditions of IR lowering phases.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xuse-k2"
        description =
            "Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            GradleOption(
                value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
                gradleInputType = GradleInputTypes.INPUT,
                shouldGenerateDeprecatedKotlinOptions = false,
            ),
            GradleDeprecatedOption(
                message = "Compiler flag -Xuse-k2 is deprecated; please use language version 2.0 instead",
                level = DeprecationLevel.HIDDEN,
                removeAfter = LanguageVersion.KOTLIN_2_2,
            )
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xuse-fir-experimental-checkers"
        description = "Enable experimental frontend IR checkers that are not yet ready for production.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_0,
            deprecatedVersion = KotlinReleaseVersion.v2_2_20,
        )
    }


    compilerArgument {
        name = "Xuse-fir-ic"
        compilerName = "useFirIC"
        description =
            "Compile using frontend IR internal incremental compilation.\nWarning: This feature is not yet production-ready.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xuse-fir-lt"
        compilerName = "useFirLT"
        description = "Compile using the LightTree parser with the frontend IR.".asReleaseDependent()
        valueType = BooleanType.defaultTrue

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xmetadata-klib"
        deprecatedName = "Xexpect-actual-linker"
        description = "Produce a klib that only contains the metadata of declarations.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xdisable-default-scripting-plugin"
        description = "Don't enable the scripting plugin by default.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xexplicit-api"
        description = """Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
Use the 'warning' level to issue warnings instead of errors.""".asReleaseDependent()
        valueDescription = ReleaseDependent(
            current = ExplicitApiMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}")
        )
        valueType = KotlinExplicitApiModeType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v1_4_0,
        )
    }


    compilerArgument {
        name = "XXexplicit-return-types"
        description = """Force the compiler to report errors on all public API declarations without an explicit return type.
Use the 'warning' level to issue warnings instead of errors.
This flag partially enables functionality of `-Xexplicit-api` flag, so please don't use them altogether""".asReleaseDependent()
        valueDescription = ReleaseDependent(
            current = ExplicitApiMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}")
        )
        valueType = KotlinExplicitApiModeType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_0_20,
        )
    }


    compilerArgument {
        name = "Xreturn-value-checker"
        description =
            "Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.".asReleaseDependent()
        valueDescription = ReleaseDependent(
            current = ReturnValueCheckerMode.entries.joinToString(prefix = "{", separator = "|", postfix = "}") { it.modeState }
        )
        valueType = ReturnValueCheckerModeType()

        lifecycle(
            introducedVersion = KotlinReleaseVersion.v2_2_0,
        )
    }


    compilerArgument {
        name = "Xsuppress-version-warnings"
        description = "Suppress warnings about outdated, inconsistent, or experimental language or API versions.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xsuppress-api-version-greater-than-language-version-error"
        description =
            "Suppress error about API version greater than language version.\nWarning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xexpect-actual-classes"
        description =
            """'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.""".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xconsistent-data-class-copy-visibility"
        description =
            "The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.DataClassCopyRespectsConstructorVisibility))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xunrestricted-builder-inference"
        description =
            "Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.UnrestrictedBuilderInference))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xcontext-receivers"
        description = "Enable experimental context receivers.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ContextReceivers))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xcontext-parameters"
        description = "Enable experimental context parameters.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ContextParameters))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xcontext-sensitive-resolution"
        description = "Enable experimental context-sensitive resolution.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.ContextSensitiveResolutionUsingExpectedType))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xnon-local-break-continue"
        description = "Enable experimental non-local break and continue.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.BreakContinueInInlineLambdas))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "-Xdata-flow-based-exhaustiveness"
        description = "Enable `when` exhaustiveness improvements that rely on data-flow analysis.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.DataFlowBasedExhaustiveness))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xdirect-java-actualization"
        description = "Enable experimental direct Java actualization support.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.DirectJavaActualization))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xmulti-dollar-interpolation"
        description = "Enable experimental multi-dollar interpolation.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.MultiDollarInterpolation))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xenable-incremental-compilation"
        compilerName = "incrementalCompilation"
        description = "Enable incremental compilation.".asReleaseDependent()
        valueType = BooleanType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xrender-internal-diagnostic-names"
        description = "Render the internal names of warnings and errors.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xallow-any-scripts-in-source-roots"
        description = "Allow compiling scripts along with regular Kotlin sources.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Disables(LanguageFeature.SkipStandaloneScriptsInSourceRoots)
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xreport-all-warnings"
        description = "Report all warnings even if errors are found.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xfragments"
        description = "Declare all known fragments of a multiplatform compilation.".asReleaseDependent()
        valueDescription = "<fragment name>".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xfragment-sources"
        description = "Add sources to a specific fragment of a multiplatform compilation.".asReleaseDependent()
        valueDescription = "<fragment name>:<path>".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xfragment-refines"
        description = "Declare that <fromModuleName> refines <onModuleName> with the dependsOn/refines relation.".asReleaseDependent()
        valueDescription = "<fromModuleName>:<onModuleName>".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xfragment-dependency"
        compilerName = "fragmentDependencies"
        valueDescription = "<fragment name>:<path>".asReleaseDependent()
        description = """Declare common klib dependencies for the specific fragment.
This argument is required for any HMPP module except the platform leaf module: it takes dependencies from -cp/-libraries.
The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
""".asReleaseDependent()
        valueType = StringArrayType.defaultNull
        delimiter = KotlinCompilerArgument.Delimiter.None

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xseparate-kmp-compilation"
        compilerName = "separateKmpCompilationScheme"
        description = "Enables the separated compilation scheme, in which common source sets are analyzed against their own dependencies".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xignore-const-optimization-errors"
        description = "Ignore all compilation exceptions while optimizing some constant expressions.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xdont-warn-on-error-suppression"
        description = "Don't report warnings when errors are suppressed. This only affects K2.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xwhen-guards"
        description = "Enable experimental language support for when guards.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.WhenGuards)
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xnested-type-aliases"
        description = "Enable experimental language support for nested type aliases.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(
            Enables(LanguageFeature.NestedTypeAliases)
        )

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xsuppress-warning"
        compilerName = "suppressedDiagnostics"
        description =
            "Suppress specified warning module-wide. This option is deprecated in favor of \"-Xwarning-level\" flag".asReleaseDependent()
        valueDescription = "<WARNING_NAME>".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xwarning-level"
        compilerName = "warningLevels"
        description = """Set the severity of the given warning.
- `error` level raises the severity of a warning to error level (similar to -Werror but more granular)
- `disabled` level suppresses reporting of a warning (similar to -nowarn but more granular)
- `warning` level overrides -nowarn and -Werror for this specific warning (the warning will be reported/won't be considered as an error)""".asReleaseDependent()
        valueDescription = "<WARNING_NAME>:(error|warning|disabled)".asReleaseDependent()
        valueType = StringArrayType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xannotation-default-target"
        description = """Change the default annotation targets for constructor properties:
-Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:';
-Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;
-Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';
default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.""".asReleaseDependent()
        valueDescription = "first-only|first-only-warn|param-property".asReleaseDependent()
        valueType = StringType.defaultNull

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "XXdebug-level-compiler-checks"
        description =
            "Enable debug level compiler checks. ATTENTION: these checks can slow compiler down or even crash it.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }


    compilerArgument {
        name = "Xannotation-target-all"
        description = "Enable experimental language support for @all: annotation use-site target.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        additionalAnnotations(Enables(LanguageFeature.AnnotationAllUseSiteTarget))

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }

    compilerArgument {
        name = "XXlenient-mode"
        description = "Lenient compiler mode. When actuals are missing, placeholder declarations are generated.".asReleaseDependent()
        valueType = BooleanType.defaultFalse

        @OptIn(TemporaryCompilerArgumentLifecycle::class)
        stubLifecycle()
    }
}
