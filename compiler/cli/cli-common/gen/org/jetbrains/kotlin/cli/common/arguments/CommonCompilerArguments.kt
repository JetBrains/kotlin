/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.kotlin.config.LanguageFeature

// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
// Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/CommonCompilerArguments.kt
// DO NOT MODIFY IT MANUALLY.

abstract class CommonCompilerArguments : CommonToolArguments() {
    @get:Transient
    var autoAdvanceLanguageVersion: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    @get:Transient
    var autoAdvanceApiVersion: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-language-version",
        valueDescription = "<version>",
        description = "Provide source compatibility with the specified version of Kotlin.",
    )
    var languageVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-api-version",
        valueDescription = "<version>",
        description = "Allow using declarations from only the specified version of bundled libraries.",
    )
    var apiVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-kotlin-home",
        valueDescription = "<path>",
        description = "Path to the Kotlin compiler home directory used for the discovery of runtime libraries.",
    )
    var kotlinHome: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-header",
        description = """Enable header compilation mode.
In this mode, the compiler produces class files that only contain the 'skeleton' of the classes to be
compiled but the method bodies of all the implementations are empty.  This is used to speed up parallel compilation
build systems where header libraries can be used to replace downstream dependencies for which we only need to
see the type names and method signatures required to compile a given translation unit.""",
    )
    var headerMode: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-progressive",
        deprecatedName = "-Xprogressive",
        description = """Enable progressive compiler mode.
In this mode, deprecations and bug fixes for unstable code take effect immediately
instead of going through a graceful migration cycle.
Code written in progressive mode is backward compatible; however, code written without
progressive mode enabled may cause compilation errors in progressive mode.""",
    )
    var progressiveMode: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-script",
        description = "Evaluate the given Kotlin script (*.kts) file.",
    )
    var script: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xrepl",
        description = "Run Kotlin REPL (deprecated)",
    )
    var repl: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-opt-in",
        deprecatedName = "-Xopt-in",
        valueDescription = "<fq.name>",
        description = "Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.",
    )
    var optIn: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-inline",
        description = "Disable method inlining.",
    )
    var noInline: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xskip-metadata-version-check",
        description = "Allow loading classes with bad metadata versions and pre-release classes.",
    )
    var skipMetadataVersionCheck: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xskip-prerelease-check",
        description = "Allow loading pre-release classes.",
    )
    var skipPrereleaseCheck: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-kotlin-package",
        description = "Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.",
    )
    var allowKotlinPackage: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xstdlib-compilation",
        description = "Enables special features which are relevant only for stdlib compilation.",
    )
    var stdlibCompilation: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xreport-output-files",
        description = "Report the source-to-output file mapping.",
    )
    var reportOutputFiles: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xplugin",
        valueDescription = "<path>",
        description = "Load plugins from the given classpath.",
    )
    var pluginClasspaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-P",
        valueDescription = "plugin:<pluginId>:<optionName>=<value>",
        description = "Pass an option to a plugin.",
    )
    var pluginOptions: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcompiler-plugin",
        valueDescription = "<path1>,<path2>[=<optionName>=<value>,<optionName>=<value>]",
        description = "Register a compiler plugin.",
        delimiter = Argument.Delimiters.none,
    )
    var pluginConfigurations: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcompiler-plugin-order",
        valueDescription = "<pluginId1>><pluginId2>",
        description = """Specify an execution order constraint for compiler plugins.
Order constraint can be specified using the 'pluginId' of compiler plugins.
The first specified plugin will be executed before the second plugin.
Multiple constraints can be specified by repeating this option. Cycles in constraints will cause an error.""",
        delimiter = Argument.Delimiters.none,
    )
    var pluginOrderConstraints: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xmulti-platform",
        description = "Enable language support for multiplatform projects.",
    )
    @Enables(LanguageFeature.MultiPlatformProjects)
    var multiPlatform: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xno-check-actual",
        description = "Do not check for the presence of the 'actual' modifier in multiplatform projects.",
    )
    var noCheckActual: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xintellij-plugin-root",
        valueDescription = "<path>",
        description = "Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.",
    )
    var intellijPluginRoot: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xnew-inference",
        description = "Enable the new experimental generic type inference algorithm.",
    )
    @Enables(LanguageFeature.NewInference)
    @Enables(LanguageFeature.SamConversionPerArgument)
    @Enables(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType)
    @Enables(LanguageFeature.DisableCompatibilityModeForNewInference)
    var newInference: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xinline-classes",
        description = "Enable experimental inline classes.",
    )
    @Enables(LanguageFeature.InlineClasses)
    var inlineClasses: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xreport-perf",
        description = "Report detailed performance statistics.",
    )
    var reportPerf: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdetailed-perf",
        description = """Enable more detailed performance statistics (Experimental).
For Native, the performance report includes execution time and lines processed per second for every individual lowering.
For WASM and JS, the performance report includes execution time and lines per second for each lowering of the first stage of compilation.""",
    )
    var detailedPerf: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdump-perf",
        valueDescription = "<path>",
        description = """Dump detailed performance statistics to the specified file in plain text, JSON or markdown format (it's detected by the file's extension).
Also, it supports the placeholder `*` and directory for generating file names based on the module being compiled and the current time stamp.
Example: `path/to/dir/*.log` creates logs like `path/to/dir/my-module_2025-06-20-12-22-32.log` in plain text format, `path/to/dir/` creates logs like `path/to/dir/my-log_2025-06-20-12-22-32.json`.""",
    )
    var dumpPerf: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-XXdump-model",
        valueDescription = "<dir>",
        description = "Dump compilation model to specified directory for use in modularized tests.",
    )
    var dumpArgumentsDir: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xmetadata-version",
        description = "Change the metadata version of the generated binary files.",
    )
    var metadataVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xcommon-sources",
        valueDescription = "<path>",
        description = """Sources of the common module that need to be compiled together with this module in multiplatform mode.
They should be a subset of sources passed as free arguments.""",
    )
    var commonSources: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xlist-phases",
        description = "List backend phases.",
    )
    var listPhases: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdisable-phases",
        description = "Disable backend phases.",
    )
    var disablePhases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xverbose-phases",
        description = "Be verbose while performing the given backend phases.",
    )
    var verbosePhases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-dump-before",
        description = "Dump the backend's state before these phases.",
    )
    var phasesToDumpBefore: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-dump-after",
        description = "Dump the backend's state after these phases.",
    )
    var phasesToDumpAfter: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-dump",
        description = "Dump the backend's state both before and after these phases.",
    )
    var phasesToDump: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdump-directory",
        description = "Dump the backend state into this directory.",
    )
    var dumpDirectory: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xdump-fqname",
        description = "Dump the declaration with the given FqName.",
    )
    var dumpOnlyFqName: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xphases-to-validate-before",
        description = "Validate the backend's state before these phases.",
    )
    var phasesToValidateBefore: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-validate-after",
        description = "Validate the backend's state after these phases.",
    )
    var phasesToValidateAfter: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-validate",
        description = "Validate the backend's state both before and after these phases.",
    )
    var phasesToValidate: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xverify-ir",
        valueDescription = "{none|warning|error}",
        description = "IR verification mode (no verification by default).",
    )
    var verifyIr: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xverify-ir-visibility",
        description = "Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.",
    )
    var verifyIrVisibility: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xprofile-phases",
        description = "Profile backend phases.",
    )
    var profilePhases: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcheck-phase-conditions",
        description = "Check pre- and postconditions of IR lowering phases.",
    )
    var checkPhaseConditions: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-k2",
        description = "Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet.",
        isObsolete = true,
    )
    var useK2: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Deprecated("This flag is deprecated")
    @Argument(
        value = "-Xuse-fir-experimental-checkers",
        description = "Enable experimental frontend IR checkers that are not yet ready for production.",
    )
    var useFirExperimentalCheckers: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fir-ic",
        description = """Compile using frontend IR internal incremental compilation.
Warning: This feature is not yet production-ready.""",
    )
    var useFirIC: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fir-lt",
        description = "Compile using the LightTree parser with the frontend IR.",
    )
    var useFirLT: Boolean = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xmetadata-klib",
        deprecatedName = "-Xexpect-actual-linker",
        description = "Produce a klib that only contains the metadata of declarations.",
    )
    var metadataKlib: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdisable-default-scripting-plugin",
        description = "Don't enable the scripting plugin by default.",
    )
    var disableDefaultScriptingPlugin: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xexplicit-api",
        valueDescription = "{strict|warning|disable}",
        description = """Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
Use the 'warning' level to issue warnings instead of errors.""",
    )
    var explicitApi: String = "disable"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-XXexplicit-return-types",
        valueDescription = "{strict|warning|disable}",
        description = """Force the compiler to report errors on all public API declarations without an explicit return type.
Use the 'warning' level to issue warnings instead of errors.
This flag partially enables functionality of `-Xexplicit-api` flag, so please don't use them altogether""",
    )
    var explicitReturnTypes: String = "disable"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xreturn-value-checker",
        valueDescription = "{check|full|disable}",
        description = "Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.",
    )
    var returnValueChecker: String = "disable"
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-version-warnings",
        description = "Suppress warnings about outdated, inconsistent, or experimental language or API versions.",
    )
    var suppressVersionWarnings: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-api-version-greater-than-language-version-error",
        description = """Suppress error about API version greater than language version.
Warning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.""",
    )
    var suppressApiVersionGreaterThanLanguageVersionError: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xexpect-actual-classes",
        description = """'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning.""",
    )
    var expectActualClasses: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xconsistent-data-class-copy-visibility",
        description = "The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914",
    )
    @Enables(LanguageFeature.DataClassCopyRespectsConstructorVisibility)
    var consistentDataClassCopyVisibility: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xunrestricted-builder-inference",
        description = "Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.",
    )
    @Enables(LanguageFeature.UnrestrictedBuilderInference)
    var unrestrictedBuilderInference: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcontext-receivers",
        description = "Enable experimental context receivers.",
    )
    @Enables(LanguageFeature.ContextReceivers)
    var contextReceivers: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcontext-parameters",
        description = "Enable experimental context parameters.",
    )
    @Enables(LanguageFeature.ContextParameters)
    var contextParameters: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcontext-sensitive-resolution",
        description = "Enable experimental context-sensitive resolution.",
    )
    @Enables(LanguageFeature.ContextSensitiveResolutionUsingExpectedType)
    var contextSensitiveResolution: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xnon-local-break-continue",
        description = "Enable experimental non-local break and continue.",
    )
    @Enables(LanguageFeature.BreakContinueInInlineLambdas)
    var nonLocalBreakContinue: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdata-flow-based-exhaustiveness",
        description = "Enable `when` exhaustiveness improvements that rely on data-flow analysis.",
    )
    @Enables(LanguageFeature.DataFlowBasedExhaustiveness)
    var dataFlowBasedExhaustiveness: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xexplicit-backing-fields",
        description = "Enable experimental language support for explicit backing fields.",
    )
    @Enables(LanguageFeature.ExplicitBackingFields)
    var explicitBackingFields: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdirect-java-actualization",
        description = "Enable experimental direct Java actualization support.",
    )
    @Enables(LanguageFeature.DirectJavaActualization)
    var directJavaActualization: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xmulti-dollar-interpolation",
        description = "Enable experimental multi-dollar interpolation.",
    )
    @Enables(LanguageFeature.MultiDollarInterpolation)
    var multiDollarInterpolation: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xenable-incremental-compilation",
        description = "Enable incremental compilation.",
    )
    var incrementalCompilation: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xrender-internal-diagnostic-names",
        description = "Render the internal names of warnings and errors.",
    )
    var renderInternalDiagnosticNames: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-any-scripts-in-source-roots",
        description = "Allow compiling scripts along with regular Kotlin sources.",
    )
    @Disables(LanguageFeature.SkipStandaloneScriptsInSourceRoots)
    var allowAnyScriptsInSourceRoots: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xreport-all-warnings",
        description = "Report all warnings even if errors are found.",
    )
    var reportAllWarnings: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfragments",
        valueDescription = "<fragment name>",
        description = "Declare all known fragments of a multiplatform compilation.",
    )
    var fragments: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfragment-sources",
        valueDescription = "<fragment name>:<path>",
        description = "Add sources to a specific fragment of a multiplatform compilation.",
    )
    var fragmentSources: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfragment-refines",
        valueDescription = "<fromModuleName>:<onModuleName>",
        description = "Declare that <fromModuleName> refines <onModuleName> with the dependsOn/refines relation.",
    )
    var fragmentRefines: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfragment-dependency",
        valueDescription = "<fragment name>:<path>",
        description = """Declare common klib dependencies for the specific fragment.
This argument is required for any HMPP module except the platform leaf module: it takes dependencies from -cp/-libraries.
The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
""",
        delimiter = Argument.Delimiters.none,
    )
    var fragmentDependencies: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfragment-friend-dependency",
        valueDescription = "<fragment name>:<path>",
        description = """Declare common klib friend dependencies for the specific fragment.
This argument can be specified for any HMPP module except the platform leaf module: it takes dependencies from the platform specific friend module arguments.
The argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation
""",
        delimiter = Argument.Delimiters.none,
    )
    var fragmentFriendDependencies: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xseparate-kmp-compilation",
        description = "Enables the separated compilation scheme, in which common source sets are analyzed against their own dependencies",
    )
    var separateKmpCompilationScheme: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xignore-const-optimization-errors",
        description = "Ignore all compilation exceptions while optimizing some constant expressions.",
    )
    var ignoreConstOptimizationErrors: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdont-warn-on-error-suppression",
        description = "Don't report warnings when errors are suppressed. This only affects K2.",
    )
    var dontWarnOnErrorSuppression: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xwhen-guards",
        description = "Enable experimental language support for when guards.",
    )
    @Enables(LanguageFeature.WhenGuards)
    var whenGuards: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xnested-type-aliases",
        description = "Enable experimental language support for nested type aliases.",
    )
    @Enables(LanguageFeature.NestedTypeAliases)
    var nestedTypeAliases: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-warning",
        valueDescription = "<WARNING_NAME>",
        description = "Suppress specified warning module-wide. This option is deprecated in favor of \"-Xwarning-level\" flag",
    )
    var suppressedDiagnostics: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xwarning-level",
        valueDescription = "<WARNING_NAME>:(error|warning|disabled)",
        description = """Set the severity of the given warning.
- `error` level raises the severity of a warning to error level (similar to -Werror but more granular)
- `disabled` level suppresses reporting of a warning (similar to -nowarn but more granular)
- `warning` level overrides -nowarn and -Werror for this specific warning (the warning will be reported/won't be considered as an error)""",
    )
    var warningLevels: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xannotation-default-target",
        valueDescription = "first-only|first-only-warn|param-property",
        description = """Change the default annotation targets for constructor properties:
-Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:';
-Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;
-Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';
default: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.""",
    )
    @Disables(LanguageFeature.AnnotationDefaultTargetMigrationWarning, "first-only")
    @Enables(LanguageFeature.AnnotationDefaultTargetMigrationWarning, "first-only-warn")
    @Disables(LanguageFeature.PropertyParamAnnotationDefaultTargetMode, "first-only")
    @Disables(LanguageFeature.PropertyParamAnnotationDefaultTargetMode, "first-only-warn")
    @Enables(LanguageFeature.PropertyParamAnnotationDefaultTargetMode, "param-property")
    var annotationDefaultTarget: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-XXdebug-level-compiler-checks",
        description = "Enable debug level compiler checks. ATTENTION: these checks can slow compiler down or even crash it.",
    )
    var debugLevelCompilerChecks: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xannotation-target-all",
        description = "Enable experimental language support for @all: annotation use-site target.",
    )
    @Enables(LanguageFeature.AnnotationAllUseSiteTarget)
    var annotationTargetAll: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-XXlenient-mode",
        description = "Lenient compiler mode. When actuals are missing, placeholder declarations are generated.",
    )
    var lenientMode: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-reified-type-in-catch",
        description = "Allow 'catch' parameters to have reified types.",
    )
    @Enables(LanguageFeature.AllowReifiedTypeInCatchClause)
    var allowReifiedTypeInCatch: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-contracts-on-more-functions",
        description = "Allow contracts on some operators and accessors, and allow checks for erased types.",
    )
    @Enables(LanguageFeature.AllowCheckForErasedTypesInContracts)
    @Enables(LanguageFeature.AllowContractsOnSomeOperators)
    @Enables(LanguageFeature.AllowContractsOnPropertyAccessors)
    var allowContractsOnMoreFunctions: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-condition-implies-returns-contracts",
        description = "Allow contracts that specify a limited conditional returns postcondition.",
    )
    @Enables(LanguageFeature.ConditionImpliesReturnsContracts)
    var allowConditionImpliesReturnsContracts: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-holdsin-contract",
        description = "Allow contracts that specify a condition that holds true inside a lambda argument.",
    )
    @Enables(LanguageFeature.HoldsInContracts)
    var allowHoldsinContract: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xname-based-destructuring",
        valueDescription = "only-syntax|name-mismatch|complete",
        description = """Enables the following destructuring features:
-Xname-based-destructuring=only-syntax:   Enables syntax for positional destructuring with square brackets and the full form of name-based destructuring with parentheses;
-Xname-based-destructuring=name-mismatch: Reports warnings when short form positional destructuring of data classes uses names that don't match the property names;
-Xname-based-destructuring=complete:      Enables short-form name-based destructuring with parentheses;""",
    )
    @Enables(LanguageFeature.NameBasedDestructuring, "only-syntax")
    @Enables(LanguageFeature.NameBasedDestructuring, "name-mismatch")
    @Enables(LanguageFeature.NameBasedDestructuring, "complete")
    @Enables(LanguageFeature.DeprecateNameMismatchInShortDestructuringWithParentheses, "name-mismatch")
    @Enables(LanguageFeature.DeprecateNameMismatchInShortDestructuringWithParentheses, "complete")
    @Enables(LanguageFeature.EnableNameBasedDestructuringShortForm, "complete")
    var nameBasedDestructuring: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-XXLanguage",
        valueDescription = "[+-]LanguageFeatureName",
        description = """Enables/disables specified language feature.
Warning: this flag is not intended for production use. If you want to configure the language behaviour use the
-language-version or corresponding experimental feature flags.""",
        delimiter = Argument.Delimiters.none,
    )
    var manuallyConfiguredFeatures: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @get:Transient
    abstract val configurator: CommonCompilerArgumentsConfigurator

    // Used only for serialize and deserialize settings. Don't use in other places!
    class DummyImpl : CommonCompilerArguments() {
        override fun copyOf(): Freezable = copyCommonCompilerArguments(this, DummyImpl())

        @get:Transient
        @field:kotlin.jvm.Transient
        override val configurator: CommonCompilerArgumentsConfigurator = CommonCompilerArgumentsConfigurator()
    }
}
