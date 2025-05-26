/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.common.arguments

import kotlin.Array
import kotlin.Boolean
import kotlin.DeprecationLevel.HIDDEN
import kotlin.String
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.API_VERSIONS
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.BOOLEAN_FALSE_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.EMPTY_STRING_ARRAY_DEFAULT
import org.jetbrains.kotlin.cli.common.arguments.DefaultValue.LANGUAGE_VERSIONS
import org.jetbrains.kotlin.cli.common.arguments.GradleInputTypes.INPUT
import org.jetbrains.kotlin.config.LanguageFeature.AnnotationAllUseSiteTarget
import org.jetbrains.kotlin.config.LanguageFeature.BreakContinueInInlineLambdas
import org.jetbrains.kotlin.config.LanguageFeature.ContextParameters
import org.jetbrains.kotlin.config.LanguageFeature.ContextReceivers
import org.jetbrains.kotlin.config.LanguageFeature.ContextSensitiveResolutionUsingExpectedType
import org.jetbrains.kotlin.config.LanguageFeature.DataClassCopyRespectsConstructorVisibility
import org.jetbrains.kotlin.config.LanguageFeature.DirectJavaActualization
import org.jetbrains.kotlin.config.LanguageFeature.DisableCompatibilityModeForNewInference
import org.jetbrains.kotlin.config.LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType
import org.jetbrains.kotlin.config.LanguageFeature.InlineClasses
import org.jetbrains.kotlin.config.LanguageFeature.MultiDollarInterpolation
import org.jetbrains.kotlin.config.LanguageFeature.MultiPlatformProjects
import org.jetbrains.kotlin.config.LanguageFeature.NestedTypeAliases
import org.jetbrains.kotlin.config.LanguageFeature.NewInference
import org.jetbrains.kotlin.config.LanguageFeature.SamConversionPerArgument
import org.jetbrains.kotlin.config.LanguageFeature.SkipStandaloneScriptsInSourceRoots
import org.jetbrains.kotlin.config.LanguageFeature.UnrestrictedBuilderInference
import org.jetbrains.kotlin.config.LanguageFeature.WhenGuards
import org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_2_2
import com.intellij.util.xmlb.annotations.Transient as AnnotationsTransient
import kotlin.jvm.Transient as JvmTransient

/**
 * This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator
 * Please declare arguments in compiler/arguments/src/org/jetbrains/kotlin/arguments/description/CommonCompilerArguments.kt
 * DO NOT MODIFY IT MANUALLY.
 */
public abstract class CommonCompilerArguments : CommonToolArguments() {
  @get:AnnotationsTransient
  public var autoAdvanceLanguageVersion: Boolean = true
    set(`value`) {
      checkFrozen()
      field = value
    }

  @get:AnnotationsTransient
  public var autoAdvanceApiVersion: Boolean = true
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = LANGUAGE_VERSIONS,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-language-version",
    valueDescription = "<version>",
    description = "Provide source compatibility with the specified version of Kotlin.",
  )
  public var languageVersion: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = API_VERSIONS,
    gradleInputType = INPUT,
    shouldGenerateDeprecatedKotlinOptions = true,
  )
  @Argument(
    value = "-api-version",
    valueDescription = "<version>",
    description = "Allow using declarations from only the specified version of bundled libraries.",
  )
  public var apiVersion: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-kotlin-home",
    valueDescription = "<path>",
    description = "Path to the Kotlin compiler home directory used for the discovery of runtime libraries.",
  )
  public var kotlinHome: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
  )
  @Argument(
    value = "-progressive",
    deprecatedName = "-Xprogressive",
    description = "Enable progressive compiler mode.\nIn this mode, deprecations and bug fixes for unstable code take effect immediately\ninstead of going through a graceful migration cycle.\nCode written in progressive mode is backward compatible; however, code written without\nprogressive mode enabled may cause compilation errors in progressive mode.",
  )
  public var progressiveMode: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-script",
    description = "Evaluate the given Kotlin script (*.kts) file.",
  )
  public var script: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xrepl",
    description = "Run Kotlin REPL (deprecated)",
  )
  public var repl: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = EMPTY_STRING_ARRAY_DEFAULT,
    gradleInputType = INPUT,
  )
  @Argument(
    value = "-opt-in",
    deprecatedName = "-Xopt-in",
    valueDescription = "<fq.name>",
    description = "Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name.",
  )
  public var optIn: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-inline",
    description = "Disable method inlining.",
  )
  public var noInline: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xskip-metadata-version-check",
    description = "Allow loading classes with bad metadata versions and pre-release classes.",
  )
  public var skipMetadataVersionCheck: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xskip-prerelease-check",
    description = "Allow loading pre-release classes.",
  )
  public var skipPrereleaseCheck: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xallow-kotlin-package",
    description = "Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'.",
  )
  public var allowKotlinPackage: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xstdlib-compilation",
    description = "Enables special features which are relevant only for stdlib compilation.",
  )
  public var stdlibCompilation: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xreport-output-files",
    description = "Report the source-to-output file mapping.",
  )
  public var reportOutputFiles: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xplugin",
    valueDescription = "<path>",
    description = "Load plugins from the given classpath.",
  )
  public var pluginClasspaths: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-P",
    valueDescription = "plugin:<pluginId>:<optionName>=<value>",
    description = "Pass an option to a plugin.",
  )
  public var pluginOptions: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcompiler-plugin",
    valueDescription = "<path1>,<path2>[=<optionName>=<value>,<optionName>=<value>]",
    delimiter = Argument.Delimiters.none,
    description = "Register a compiler plugin.",
  )
  public var pluginConfigurations: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xmulti-platform",
    description = "Enable language support for multiplatform projects.",
  )
  @Enables(MultiPlatformProjects)
  public var multiPlatform: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xno-check-actual",
    description = "Do not check for the presence of the 'actual' modifier in multiplatform projects.",
  )
  public var noCheckActual: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xintellij-plugin-root",
    valueDescription = "<path>",
    description = "Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found.",
  )
  public var intellijPluginRoot: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xnew-inference",
    description = "Enable the new experimental generic type inference algorithm.",
  )
  @Enables(NewInference)
  @Enables(SamConversionPerArgument)
  @Enables(FunctionReferenceWithDefaultValueAsOtherType)
  @Enables(DisableCompatibilityModeForNewInference)
  public var newInference: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xinline-classes",
    description = "Enable experimental inline classes.",
  )
  @Enables(InlineClasses)
  public var inlineClasses: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xreport-perf",
    description = "Report detailed performance statistics.",
  )
  public var reportPerf: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdump-perf",
    valueDescription = "<path>",
    description = "Dump detailed performance statistics to the specified file.",
  )
  public var dumpPerf: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xmetadata-version",
    description = "Change the metadata version of the generated binary files.",
  )
  public var metadataVersion: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xcommon-sources",
    valueDescription = "<path>",
    description = "Sources of the common module that need to be compiled together with this module in multiplatform mode.\nThey should be a subset of sources passed as free arguments.",
  )
  public var commonSources: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xlist-phases",
    description = "List backend phases.",
  )
  public var listPhases: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdisable-phases",
    description = "Disable backend phases.",
  )
  public var disablePhases: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xverbose-phases",
    description = "Be verbose while performing the given backend phases.",
  )
  public var verbosePhases: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xphases-to-dump-before",
    description = "Dump the backend's state before these phases.",
  )
  public var phasesToDumpBefore: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xphases-to-dump-after",
    description = "Dump the backend's state after these phases.",
  )
  public var phasesToDumpAfter: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xphases-to-dump",
    description = "Dump the backend's state both before and after these phases.",
  )
  public var phasesToDump: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdump-directory",
    description = "Dump the backend state into this directory.",
  )
  public var dumpDirectory: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xdump-fqname",
    description = "Dump the declaration with the given FqName.",
  )
  public var dumpOnlyFqName: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xphases-to-validate-before",
    description = "Validate the backend's state before these phases.",
  )
  public var phasesToValidateBefore: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xphases-to-validate-after",
    description = "Validate the backend's state after these phases.",
  )
  public var phasesToValidateAfter: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xphases-to-validate",
    description = "Validate the backend's state both before and after these phases.",
  )
  public var phasesToValidate: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xverify-ir",
    valueDescription = "{none|warning|error}",
    description = "IR verification mode (no verification by default).",
  )
  public var verifyIr: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-Xverify-ir-visibility",
    description = "Check for visibility violations in IR when validating it before running any lowerings. Only has effect if '-Xverify-ir' is not 'none'.",
  )
  public var verifyIrVisibility: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xprofile-phases",
    description = "Profile backend phases.",
  )
  public var profilePhases: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcheck-phase-conditions",
    description = "Check pre- and postconditions of IR lowering phases.",
  )
  public var checkPhaseConditions: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @GradleOption(
    value = BOOLEAN_FALSE_DEFAULT,
    gradleInputType = INPUT,
  )
  @GradleDeprecatedOption(
    message = "Compiler flag -Xuse-k2 is deprecated; please use language version 2.0 instead",
    removeAfter = KOTLIN_2_2,
    level = HIDDEN,
  )
  @Argument(
    value = "-Xuse-k2",
    description = "Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet.",
  )
  public var useK2: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-fir-experimental-checkers",
    description = "Enable experimental frontend IR checkers that are not yet ready for production.",
  )
  public var useFirExperimentalCheckers: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-fir-ic",
    description = "Compile using frontend IR internal incremental compilation.\nWarning: This feature is not yet production-ready.",
  )
  public var useFirIC: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xuse-fir-lt",
    description = "Compile using the LightTree parser with the frontend IR.",
  )
  public var useFirLT: Boolean = true
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xmetadata-klib",
    deprecatedName = "-Xexpect-actual-linker",
    description = "Produce a klib that only contains the metadata of declarations.",
  )
  public var metadataKlib: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdisable-default-scripting-plugin",
    description = "Don't enable the scripting plugin by default.",
  )
  public var disableDefaultScriptingPlugin: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xexplicit-api",
    valueDescription = "{strict|warning|disable}",
    description = "Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.\nUse the 'warning' level to issue warnings instead of errors.",
  )
  public var explicitApi: String = "disable"
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-XXexplicit-return-types",
    valueDescription = "{strict|warning|disable}",
    description = "Force the compiler to report errors on all public API declarations without an explicit return type.\nUse the 'warning' level to issue warnings instead of errors.\nThis flag partially enables functionality of `-Xexplicit-api` flag, so please don't use them altogether",
  )
  public var explicitReturnTypes: String = "disable"
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xreturn-value-checker",
    valueDescription = "{check|full|disable}",
    description = "Set improved unused return value checker mode. Use 'check' to run checker only and use 'full' to also enable automatic annotation insertion.",
  )
  public var returnValueChecker: String = "disable"
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xsuppress-version-warnings",
    description = "Suppress warnings about outdated, inconsistent, or experimental language or API versions.",
  )
  public var suppressVersionWarnings: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xsuppress-api-version-greater-than-language-version-error",
    description = "Suppress error about API version greater than language version.\nWarning: This is temporary solution (see KT-63712) intended to be used only for stdlib build.",
  )
  public var suppressApiVersionGreaterThanLanguageVersionError: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xexpect-actual-classes",
    description = "'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.\nKotlin reports a warning every time you use one of them. You can use this flag to mute the warning.",
  )
  public var expectActualClasses: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xconsistent-data-class-copy-visibility",
    description = "The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. See https://youtrack.jetbrains.com/issue/KT-11914",
  )
  @Enables(DataClassCopyRespectsConstructorVisibility)
  public var consistentDataClassCopyVisibility: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xunrestricted-builder-inference",
    description = "Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls.",
  )
  @Enables(UnrestrictedBuilderInference)
  public var unrestrictedBuilderInference: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcontext-receivers",
    description = "Enable experimental context receivers.",
  )
  @Enables(ContextReceivers)
  public var contextReceivers: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcontext-parameters",
    description = "Enable experimental context parameters.",
  )
  @Enables(ContextParameters)
  public var contextParameters: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xcontext-sensitive-resolution",
    description = "Enable experimental context-sensitive resolution.",
  )
  @Enables(ContextSensitiveResolutionUsingExpectedType)
  public var contextSensitiveResolution: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xnon-local-break-continue",
    description = "Enable experimental non-local break and continue.",
  )
  @Enables(BreakContinueInInlineLambdas)
  public var nonLocalBreakContinue: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdirect-java-actualization",
    description = "Enable experimental direct Java actualization support.",
  )
  @Enables(DirectJavaActualization)
  public var directJavaActualization: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xmulti-dollar-interpolation",
    description = "Enable experimental multi-dollar interpolation.",
  )
  @Enables(MultiDollarInterpolation)
  public var multiDollarInterpolation: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xenable-incremental-compilation",
    description = "Enable incremental compilation.",
  )
  public var incrementalCompilation: Boolean? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xrender-internal-diagnostic-names",
    description = "Render the internal names of warnings and errors.",
  )
  public var renderInternalDiagnosticNames: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xallow-any-scripts-in-source-roots",
    description = "Allow compiling scripts along with regular Kotlin sources.",
  )
  @Disables(SkipStandaloneScriptsInSourceRoots)
  public var allowAnyScriptsInSourceRoots: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xreport-all-warnings",
    description = "Report all warnings even if errors are found.",
  )
  public var reportAllWarnings: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xfragments",
    valueDescription = "<fragment name>",
    description = "Declare all known fragments of a multiplatform compilation.",
  )
  public var fragments: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xfragment-sources",
    valueDescription = "<fragment name>:<path>",
    description = "Add sources to a specific fragment of a multiplatform compilation.",
  )
  public var fragmentSources: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xfragment-refines",
    valueDescription = "<fromModuleName>:<onModuleName>",
    description = "Declare that <fromModuleName> refines <onModuleName> with the dependsOn/refines relation.",
  )
  public var fragmentRefines: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xfragment-dependency",
    valueDescription = "<fragment name>:<path>",
    description = "Declare common klib dependencies for the specific fragment.\nThis argument is required for any HMPP module except the platform leaf module: it takes dependencies from -cp/-libraries.\nThe argument should be used only if the new compilation scheme is enabled with -Xseparate-kmp-compilation\n",
  )
  public var fragmentDependencies: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xseparate-kmp-compilation",
    description = "Enables the separated compilation scheme, in which common source sets are analyzed against their own dependencies",
  )
  public var separateKmpCompilationScheme: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xignore-const-optimization-errors",
    description = "Ignore all compilation exceptions while optimizing some constant expressions.",
  )
  public var ignoreConstOptimizationErrors: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xdont-warn-on-error-suppression",
    description = "Don't report warnings when errors are suppressed. This only affects K2.",
  )
  public var dontWarnOnErrorSuppression: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xwhen-guards",
    description = "Enable experimental language support for when guards.",
  )
  @Enables(WhenGuards)
  public var whenGuards: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xnested-type-aliases",
    description = "Enable experimental language support for nested type aliases.",
  )
  @Enables(NestedTypeAliases)
  public var nestedTypeAliases: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xsuppress-warning",
    valueDescription = "<WARNING_NAME>",
    description = "Suppress specified warning module-wide. This option is deprecated in favor of \"-Xwarning-level\" flag",
  )
  public var suppressedDiagnostics: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xwarning-level",
    valueDescription = "<WARNING_NAME>:(error|warning|disabled)",
    description = "Set the severity of the given warning.\n- `error` level raises the severity of a warning to error level (similar to -Werror but more granular)\n- `disabled` level suppresses reporting of a warning (similar to -nowarn but more granular)\n- `warning` level overrides -nowarn and -Werror for this specific warning (the warning will be reported/won't be considered as an error)",
  )
  public var warningLevels: Array<String>? = null
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xannotation-default-target",
    valueDescription = "first-only|first-only-warn|param-property",
    description = "Change the default annotation targets for constructor properties:\n-Xannotation-default-target=first-only:      use the first of the following allowed targets: '@param:', '@property:', '@field:';\n-Xannotation-default-target=first-only-warn: same as first-only, and raise warnings when both '@param:' and either '@property:' or '@field:' are allowed;\n-Xannotation-default-target=param-property:  use '@param:' target if applicable, and also use the first of either '@property:' or '@field:';\ndefault: 'first-only-warn' in language version 2.2+, 'first-only' in version 2.1 and before.",
  )
  public var annotationDefaultTarget: String? = null
    set(`value`) {
      checkFrozen()
      field = if (value.isNullOrEmpty()) null else value
    }

  @Argument(
    value = "-XXdebug-level-compiler-checks",
    description = "Enable debug level compiler checks. ATTENTION: these checks can slow compiler down or even crash it.",
  )
  public var debugLevelCompilerChecks: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-Xannotation-target-all",
    description = "Enable experimental language support for @all: annotation use-site target.",
  )
  @Enables(AnnotationAllUseSiteTarget)
  public var annotationTargetAll: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @Argument(
    value = "-XXlenient-mode",
    description = "Lenient compiler mode. When actuals are missing, placeholder declarations are generated.",
  )
  public var lenientMode: Boolean = false
    set(`value`) {
      checkFrozen()
      field = value
    }

  @get:AnnotationsTransient
  public abstract val configurator: CommonCompilerArgumentsConfigurator

  /**
   * Used only for serialize and deserialize settings. Don't use in other places!
   */
  public class DummyImpl : CommonCompilerArguments() {
    @field:JvmTransient
    @get:AnnotationsTransient
    override val configurator: CommonCompilerArgumentsConfigurator =
        CommonCompilerArgumentsConfigurator()

    override fun copyOf(): Freezable = copyCommonCompilerArguments(this, DummyImpl())
  }
}
