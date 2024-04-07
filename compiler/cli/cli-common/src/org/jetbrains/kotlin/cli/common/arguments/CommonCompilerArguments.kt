/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.utils.IDEAPlatforms
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

@SuppressWarnings("WeakerAccess")
abstract class CommonCompilerArguments : CommonToolArguments() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 0L

        const val PLUGIN_OPTION_FORMAT = "plugin:<pluginId>:<optionName>=<value>"
        const val PLUGIN_DECLARATION_FORMAT = "<path>[=<optionName>=<value>]"

        const val WARN = "warn"
        const val ERROR = "error"
        const val ENABLE = "enable"
        const val DEFAULT = "default"
    }

    @get:Transient
    var autoAdvanceLanguageVersion = true
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.LANGUAGE_VERSIONS,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-language-version",
        valueDescription = "<version>",
        description = "Provide source compatibility with the specified version of Kotlin."
    )
    var languageVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @get:Transient
    var autoAdvanceApiVersion = true
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.API_VERSIONS,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-api-version",
        valueDescription = "<version>",
        description = "Allow using declarations from only the specified version of bundled libraries."
    )
    var apiVersion: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-kotlin-home",
        valueDescription = "<path>",
        description = "Path to the Kotlin compiler home directory used for the discovery of runtime libraries."
    )
    var kotlinHome: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @GradleOption(
        value = DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT
    )
    @Argument(
        value = "-progressive",
        deprecatedName = "-Xprogressive",
        description = """Enable progressive compiler mode.
In this mode, deprecations and bug fixes for unstable code take effect immediately
instead of going through a graceful migration cycle.
Code written in progressive mode is backward compatible; however, code written without
progressive mode enabled may cause compilation errors in progressive mode."""
    )
    var progressiveMode = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-script", description = "Evaluate the given Kotlin script (*.kts) file.")
    var script = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleOption(
        value = DefaultValue.EMPTY_STRING_ARRAY_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT
    )
    @Argument(
        value = "-opt-in",
        deprecatedName = "-Xopt-in",
        valueDescription = "<fq.name>",
        description = "Enable API usages that require opt-in with an opt-in requirement marker with the given fully qualified name."
    )
    var optIn: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    // Advanced options

    @Argument(value = "-Xno-inline", description = "Disable method inlining.")
    var noInline = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xskip-metadata-version-check",
        description = "Allow loading classes with bad metadata versions and pre-release classes."
    )
    var skipMetadataVersionCheck = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xskip-prerelease-check", description = "Allow loading pre-release classes.")
    var skipPrereleaseCheck = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xallow-kotlin-package",
        description = "Allow compiling code in the 'kotlin' package, and allow not requiring 'kotlin.stdlib' in 'module-info'."
    )
    var allowKotlinPackage = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xreport-output-files", description = "Report the source-to-output file mapping.")
    var reportOutputFiles = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xplugin", valueDescription = "<path>", description = "Load plugins from the given classpath.")
    var pluginClasspaths: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-P", valueDescription = PLUGIN_OPTION_FORMAT, description = "Pass an option to a plugin.")
    var pluginOptions: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcompiler-plugin",
        valueDescription = "<path1>,<path2>:<optionName>=<value>,<optionName>=<value>",
        description = "Register a compiler plugin.",
        delimiter = Argument.Delimiters.none
    )
    var pluginConfigurations: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xmulti-platform", description = "Enable language support for multiplatform projects.")
    var multiPlatform = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xno-check-actual", description = "Do not check for the presence of the 'actual' modifier in multiplatform projects.")
    var noCheckActual = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xintellij-plugin-root",
        valueDescription = "<path>",
        description = "Path to 'kotlin-compiler.jar' or the directory where the IntelliJ IDEA configuration files can be found."
    )
    var intellijPluginRoot: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xnew-inference",
        description = "Enable the new experimental generic type inference algorithm."
    )
    var newInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xinline-classes",
        description = "Enable experimental inline classes."
    )
    var inlineClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xlegacy-smart-cast-after-try",
        description = "Allow 'var' smart casts even in the presence of assignments in 'try' blocks."
    )
    var legacySmartCastAfterTry = false
        set(value) {
            checkFrozen()
            field = value
        }

    @IDEAPluginsCompatibilityAPI(
        IDEAPlatforms._212, // maybe 211 AS used it too
        IDEAPlatforms._213,
        message = "Please migrate to -opt-in",
        plugins = "Android"
    )
    var experimental: Array<String>? = null

    @IDEAPluginsCompatibilityAPI(
        IDEAPlatforms._212, // maybe 211 AS used it too
        IDEAPlatforms._213,
        message = "Please migrate to -opt-in",
        plugins = "Android"
    )
    var useExperimental: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xreport-perf", description = "Report detailed performance statistics.")
    var reportPerf = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdump-perf",
        valueDescription = "<path>",
        description = "Dump detailed performance statistics to the specified file."
    )
    var dumpPerf: String? = null
        set(value) {
            checkFrozen()
            field = if (value.isNullOrEmpty()) null else value
        }

    @Argument(
        value = "-Xmetadata-version",
        description = "Change the metadata version of the generated binary files."
    )
    var metadataVersion: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcommon-sources",
        valueDescription = "<path>",
        description = """Sources of the common module that need to be compiled together with this module in multiplatform mode.
They should be a subset of sources passed as free arguments."""
    )
    var commonSources: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xlist-phases",
        description = "List backend phases."
    )
    var listPhases = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdisable-phases",
        description = "Disable backend phases."
    )
    var disablePhases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xverbose-phases",
        description = "Be verbose while performing the given backend phases."
    )
    var verbosePhases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-dump-before",
        description = "Dump the backend's state before these phases."
    )
    var phasesToDumpBefore: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-dump-after",
        description = "Dump the backend's state after these phases."
    )
    var phasesToDumpAfter: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-dump",
        description = "Dump the backend's state both before and after these phases."
    )
    var phasesToDump: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdump-directory",
        description = "Dump the backend state into this directory."
    )
    var dumpDirectory: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdump-fqname",
        description = "Dump the declaration with the given FqName."
    )
    var dumpOnlyFqName: String? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-validate-before",
        description = "Validate the backend's state before these phases."
    )
    var phasesToValidateBefore: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-validate-after",
        description = "Validate the backend's state after these phases."
    )
    var phasesToValidateAfter: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xphases-to-validate",
        description = "Validate the backend's state both before and after these phases."
    )
    var phasesToValidate: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xprofile-phases",
        description = "Profile backend phases."
    )
    var profilePhases = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcheck-phase-conditions",
        description = "Check pre- and postconditions of IR lowering phases."
    )
    var checkPhaseConditions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcheck-sticky-phase-conditions",
        description = "Run sticky condition checks on subsequent phases. Implicitly enables '-Xcheck-phase-conditions'."
    )
    var checkStickyPhaseConditions = false
        set(value) {
            checkFrozen()
            field = value
        }

    @GradleDeprecatedOption(
        message = "Compiler flag -Xuse-k2 is deprecated; please use language version 2.0 instead",
        level = DeprecationLevel.WARNING, // TODO: KT-65990 switch to ERROR in 2.1
        removeAfter = LanguageVersion.KOTLIN_2_1,
    )
    @GradleOption(
        DefaultValue.BOOLEAN_FALSE_DEFAULT,
        gradleInputType = GradleInputTypes.INPUT,
        shouldGenerateDeprecatedKotlinOptions = true,
    )
    @Argument(
        value = "-Xuse-k2",
        deprecatedName = "-Xuse-fir",
        description = "Compile using the experimental K2 compiler pipeline. No compatibility guarantees are provided yet."
    )
    var useK2 = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fir-extended-checkers",
        description = "Use extended analysis mode based on the frontend IR.\nWarning: This feature is not yet production-ready."
    )
    var useFirExtendedCheckers = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fir-ic",
        description = "Compile using frontend IR internal incremental compilation.\nWarning: This feature is not yet production-ready."
    )
    var useFirIC = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fir-lt",
        description = "Compile using the LightTree parser with the frontend IR."
    )
    var useFirLT = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xuse-fir-fake-override-builder",
        description = "Generate all fake overrides via FIR2IR instead of IR, i.e. revert to behavior before KT-61514 was resolved."
    )
    var useFirFakeOverrideBuilder = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xmetadata-klib",
        description = "Produce a klib that only contains the metadata of declarations.",
        deprecatedName = "-Xexpect-actual-linker"
    )
    var metadataKlib: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xdisable-default-scripting-plugin", description = "Don't enable the scripting plugin by default.")
    var disableDefaultScriptingPlugin = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xexplicit-api",
        valueDescription = "{strict|warning|disable}",
        description = """Force the compiler to report errors on all public API declarations without an explicit visibility or a return type.
Use the 'warning' level to issue warnings instead of errors."""
    )
    var explicitApi: String = ExplicitApiMode.DISABLED.state
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xinference-compatibility",
        description = "Enable compatibility changes for the generic type inference algorithm."
    )
    var inferenceCompatibility = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xsuppress-version-warnings",
        description = "Suppress warnings about outdated, inconsistent, or experimental language or API versions."
    )
    var suppressVersionWarnings = false
        set(value) {
            checkFrozen()
            field = value
        }

    // TODO(KT-56076): remove this argument after stdlib started to be built with 2.0
    @Argument(
        value = "-Xsuppress-api-version-greater-than-language-version-error",
        description = "Suppress error about API version greater than language version.\n" +
                "Warning: This is temporary solution (see KT-63712) intended to be used only for stdlib build."
    )
    var suppressApiVersionGreaterThanLanguageVersionError: Boolean = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xextended-compiler-checks",
        description = """Enable additional compiler checks that might provide verbose diagnostic information for certain errors.
Warning: This mode is not backward compatible and might cause compilation errors in previously compiled code."""
    )
    var extendedCompilerChecks = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xexpect-actual-classes",
        description = """'expect'/'actual' classes (including interfaces, objects, annotations, enums, and 'actual' typealiases) are in Beta.
Kotlin reports a warning every time you use one of them. You can use this flag to mute the warning."""
    )
    var expectActualClasses = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xconsistent-data-class-copy-visibility",
        description = "The effect of this compiler flag is the same as applying @ConsistentCopyVisibility annotation to all data classes in the module. " +
                "See https://youtrack.jetbrains.com/issue/KT-11914"
    )
    var consistentDataClassCopyVisibility = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xunrestricted-builder-inference",
        description = "Eliminate builder inference restrictions, for example by allowing type variables to be returned from builder inference calls."
    )
    var unrestrictedBuilderInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xenable-builder-inference",
        description = """Use builder inference by default for all calls with lambdas that can't be resolved without it.
The corresponding calls' declarations may not be marked with @BuilderInference."""
    )
    var enableBuilderInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xself-upper-bound-inference",
        description = "Support inferring type arguments from the self-type upper bounds of the corresponding type parameters."
    )
    var selfUpperBoundInference = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xcontext-receivers",
        description = "Enable experimental context receivers."
    )
    var contextReceivers = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-relative-path-base",
        description = "Provide a base path to compute the source's relative paths in klib (default is empty)."
    )
    var relativePathBases: Array<String>? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-normalize-absolute-path",
        description = "Normalize absolute paths in klibs."
    )
    var normalizeAbsolutePath = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xklib-enable-signature-clash-checks",
        description = "Enable signature uniqueness checks."
    )
    var enableSignatureClashChecks = true
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xenable-incremental-compilation", description = "Enable incremental compilation.")
    var incrementalCompilation: Boolean? = null
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xrender-internal-diagnostic-names", description = "Render the internal names of warnings and errors.")
    var renderInternalDiagnosticNames = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xallow-any-scripts-in-source-roots", description = "Allow compiling scripts along with regular Kotlin sources.")
    var allowAnyScriptsInSourceRoots = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(value = "-Xreport-all-warnings", description = "Report all warnings even if errors are found.")
    var reportAllWarnings = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xfragments",
        valueDescription = "<fragment name>",
        description = "Declare all known fragments of a multiplatform compilation."
    )
    var fragments: Array<String>? = null

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
        value = "-Xignore-const-optimization-errors",
        description = "Ignore all compilation exceptions while optimizing some constant expressions."
    )
    var ignoreConstOptimizationErrors = false
        set(value) {
            checkFrozen()
            field = value
        }

    @Argument(
        value = "-Xdont-warn-on-error-suppression",
        description = "Don't report warnings when errors are suppressed. This only affects K2."
    )
    var dontWarnOnErrorSuppression = false
        set(value) {
            checkFrozen()
            field = value
        }

    @OptIn(IDEAPluginsCompatibilityAPI::class)
    open fun configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
        return HashMap<AnalysisFlag<*>, Any>().apply {
            put(AnalysisFlags.skipMetadataVersionCheck, skipMetadataVersionCheck)
            put(AnalysisFlags.skipPrereleaseCheck, skipPrereleaseCheck || skipMetadataVersionCheck)
            put(AnalysisFlags.multiPlatformDoNotCheckActual, noCheckActual)
            val useExperimentalFqNames = useExperimental?.toList().orEmpty()
            if (useExperimentalFqNames.isNotEmpty()) {
                collector.report(
                    WARNING, "'-Xuse-experimental' is deprecated and will be removed in a future release, please use -opt-in instead"
                )
            }
            put(AnalysisFlags.optIn, useExperimentalFqNames + optIn?.toList().orEmpty())
            put(AnalysisFlags.skipExpectedActualDeclarationChecker, metadataKlib)
            put(AnalysisFlags.explicitApiVersion, apiVersion != null)
            ExplicitApiMode.fromString(explicitApi)?.also { put(AnalysisFlags.explicitApiMode, it) } ?: collector.report(
                CompilerMessageSeverity.ERROR,
                "Unknown value for parameter -Xexplicit-api: '$explicitApi'. Value should be one of ${ExplicitApiMode.availableValues()}"
            )
            put(AnalysisFlags.extendedCompilerChecks, extendedCompilerChecks)
            put(AnalysisFlags.allowKotlinPackage, allowKotlinPackage)
            put(AnalysisFlags.muteExpectActualClassesWarning, expectActualClasses)
            put(AnalysisFlags.consistentDataClassCopyVisibility, consistentDataClassCopyVisibility)
            put(AnalysisFlags.allowFullyQualifiedNameInKClass, true)
            put(AnalysisFlags.dontWarnOnErrorSuppression, dontWarnOnErrorSuppression)
        }
    }

    open fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> =
        HashMap<LanguageFeature, LanguageFeature.State>().apply {
            if (multiPlatform) {
                put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED)
            }

            if (unrestrictedBuilderInference) {
                put(LanguageFeature.UnrestrictedBuilderInference, LanguageFeature.State.ENABLED)
            }

            if (enableBuilderInference) {
                put(LanguageFeature.UseBuilderInferenceWithoutAnnotation, LanguageFeature.State.ENABLED)
            }

            if (selfUpperBoundInference) {
                put(LanguageFeature.TypeInferenceOnCallsWithSelfTypes, LanguageFeature.State.ENABLED)
            }

            if (newInference) {
                put(LanguageFeature.NewInference, LanguageFeature.State.ENABLED)
                put(LanguageFeature.SamConversionPerArgument, LanguageFeature.State.ENABLED)
                put(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType, LanguageFeature.State.ENABLED)
                put(LanguageFeature.DisableCompatibilityModeForNewInference, LanguageFeature.State.ENABLED)
            }

            if (contextReceivers) {
                put(LanguageFeature.ContextReceivers, LanguageFeature.State.ENABLED)
            }

            if (inlineClasses) {
                put(LanguageFeature.InlineClasses, LanguageFeature.State.ENABLED)
            }

            if (legacySmartCastAfterTry) {
                put(LanguageFeature.SoundSmartCastsAfterTry, LanguageFeature.State.DISABLED)
            }

            if (inferenceCompatibility) {
                put(LanguageFeature.InferenceCompatibility, LanguageFeature.State.ENABLED)
            }

            if (progressiveMode) {
                LanguageFeature.entries.filter { it.enabledInProgressiveMode }.forEach {
                    // Don't overwrite other settings: users may want to turn off some particular
                    // breaking change manually instead of turning off whole progressive mode
                    if (!contains(it)) put(it, LanguageFeature.State.ENABLED)
                }
            }

            if (useK2) {
                // TODO: remove when K2 compilation will mean LV 2.0
                put(LanguageFeature.SkipStandaloneScriptsInSourceRoots, LanguageFeature.State.ENABLED)
            } else if (allowAnyScriptsInSourceRoots) {
                put(LanguageFeature.SkipStandaloneScriptsInSourceRoots, LanguageFeature.State.DISABLED)
            }

            // Internal arguments should go last, because it may be useful to override
            // some feature state via -XX (even if some -X flags were passed)
            if (internalArguments.isNotEmpty()) {
                configureLanguageFeaturesFromInternalArgs(collector)
            }

            configureExtraLanguageFeatures(this)
        }

    protected open fun configureExtraLanguageFeatures(map: HashMap<LanguageFeature, LanguageFeature.State>) {}

    private fun HashMap<LanguageFeature, LanguageFeature.State>.configureLanguageFeaturesFromInternalArgs(collector: MessageCollector) {
        val featuresThatForcePreReleaseBinaries = mutableListOf<LanguageFeature>()
        val disabledFeaturesFromUnsupportedVersions = mutableListOf<LanguageFeature>()

        var standaloneSamConversionFeaturePassedExplicitly = false
        var functionReferenceWithDefaultValueFeaturePassedExplicitly = false
        for ((feature, state) in internalArguments.filterIsInstance<ManualLanguageFeatureSetting>()) {
            put(feature, state)
            if (state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()) {
                featuresThatForcePreReleaseBinaries += feature
            }

            if (state == LanguageFeature.State.DISABLED && feature.sinceVersion?.isUnsupported == true) {
                disabledFeaturesFromUnsupportedVersions += feature
            }

            when (feature) {
                LanguageFeature.SamConversionPerArgument ->
                    standaloneSamConversionFeaturePassedExplicitly = true

                LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType ->
                    functionReferenceWithDefaultValueFeaturePassedExplicitly = true

                else -> {}
            }
        }

        if (this[LanguageFeature.NewInference] == LanguageFeature.State.ENABLED) {
            if (!standaloneSamConversionFeaturePassedExplicitly)
                put(LanguageFeature.SamConversionPerArgument, LanguageFeature.State.ENABLED)

            if (!functionReferenceWithDefaultValueFeaturePassedExplicitly)
                put(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType, LanguageFeature.State.ENABLED)

            put(LanguageFeature.DisableCompatibilityModeForNewInference, LanguageFeature.State.ENABLED)
        }

        if (featuresThatForcePreReleaseBinaries.isNotEmpty()) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Following manually enabled features will force generation of pre-release binaries: ${featuresThatForcePreReleaseBinaries.joinToString()}"
            )
        }

        if (disabledFeaturesFromUnsupportedVersions.isNotEmpty()) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "The following features cannot be disabled manually, because the version they first appeared in is no longer " +
                        "supported:\n${disabledFeaturesFromUnsupportedVersions.joinToString()}"
            )
        }
    }

    fun toLanguageVersionSettings(collector: MessageCollector): LanguageVersionSettings {
        return toLanguageVersionSettings(collector, emptyMap())
    }

    fun toLanguageVersionSettings(
        collector: MessageCollector,
        additionalAnalysisFlags: Map<AnalysisFlag<*>, Any>
    ): LanguageVersionSettings {
        val languageVersion = parseOrConfigureLanguageVersion(collector)
        // If only "-language-version" is specified, API version is assumed to be equal to the language version
        // (API version cannot be greater than the language version)
        val apiVersion = ApiVersion.createByLanguageVersion(parseVersion(collector, apiVersion, "API") ?: languageVersion)

        checkApiVersionIsNotGreaterThenLanguageVersion(languageVersion, apiVersion, collector)

        val languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion,
            apiVersion,
            configureAnalysisFlags(collector, languageVersion) + additionalAnalysisFlags,
            configureLanguageFeatures(collector)
        )

        checkLanguageVersionIsStable(languageVersion, collector)
        checkOutdatedVersions(languageVersion, apiVersion, collector)
        checkProgressiveMode(languageVersion, collector)

        checkIrSupport(languageVersionSettings, collector)

        checkPlatformSpecificSettings(languageVersionSettings, collector)

        return languageVersionSettings
    }

    private fun checkApiVersionIsNotGreaterThenLanguageVersion(
        languageVersion: LanguageVersion,
        apiVersion: ApiVersion,
        collector: MessageCollector
    ) {
        if (apiVersion > ApiVersion.createByLanguageVersion(languageVersion)) {
            if (!suppressApiVersionGreaterThanLanguageVersionError) {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "-api-version (${apiVersion.versionString}) cannot be greater than -language-version (${languageVersion.versionString})"
                )
            }
        } else if (suppressApiVersionGreaterThanLanguageVersionError) {
            collector.report(WARNING, "Useless suppress -Xsuppress-api-version-greater-than-language-version-error")
        }
    }

    fun checkLanguageVersionIsStable(languageVersion: LanguageVersion, collector: MessageCollector) {
        if (!languageVersion.isStable && !suppressVersionWarnings) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Language version ${languageVersion.versionString} is experimental, there are no backwards compatibility guarantees for " +
                        "new language and library features"
            )
        }
    }

    private fun checkOutdatedVersions(language: LanguageVersion, api: ApiVersion, collector: MessageCollector) {
        val (version, supportedVersion, versionKind) = findOutdatedVersion(language, api) ?: return
        when {
            version.isUnsupported -> {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "${versionKind.text} version ${version.versionString} is no longer supported; " +
                            "please, use version ${supportedVersion!!.versionString} or greater."
                )
            }
            version.isDeprecated && !suppressVersionWarnings -> {
                collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "${versionKind.text} version ${version.versionString} is deprecated " +
                            "and its support will be removed in a future version of Kotlin"
                )
            }
        }
    }

    private fun findOutdatedVersion(
        language: LanguageVersion,
        api: ApiVersion
    ): Triple<LanguageOrApiVersion, LanguageOrApiVersion?, VersionKind>? {
        return when {
            language.isUnsupported -> Triple(language, LanguageVersion.FIRST_SUPPORTED, VersionKind.LANGUAGE)
            api.isUnsupported -> Triple(api, ApiVersion.FIRST_SUPPORTED, VersionKind.API)
            language.isDeprecated -> Triple(language, null, VersionKind.LANGUAGE)
            api.isDeprecated -> Triple(api, null, VersionKind.API)
            else -> null
        }
    }

    private fun checkProgressiveMode(languageVersion: LanguageVersion, collector: MessageCollector) {
        if (progressiveMode && languageVersion < LanguageVersion.LATEST_STABLE && !suppressVersionWarnings) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "'-progressive' is meaningful only for the latest language version (${LanguageVersion.LATEST_STABLE}), " +
                        "while this build uses $languageVersion\n" +
                        "Compiler behavior in such mode is undefined; please, consider moving to the latest stable version " +
                        "or turning off progressive mode."
            )
        }
    }

    protected open fun defaultLanguageVersion(collector: MessageCollector): LanguageVersion =
        LanguageVersion.LATEST_STABLE

    protected open fun checkPlatformSpecificSettings(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
    }

    protected open fun checkIrSupport(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        // backend-specific
    }

    private enum class VersionKind(val text: String) {
        LANGUAGE("Language"), API("API")
    }

    private fun parseOrConfigureLanguageVersion(collector: MessageCollector): LanguageVersion {
        // If only "-api-version" is specified, language version is assumed to be the latest stable (or 2.0 with -Xuse-k2)
        val explicitVersion = parseVersion(collector, languageVersion, "language")
        val explicitOrDefaultVersion = explicitVersion ?: defaultLanguageVersion(collector)
        if (useK2) {
            val message = when (explicitVersion?.usesK2) {
                true ->
                    "Deprecated compiler flag -Xuse-k2 is redundant because of \"-language-version $explicitVersion\" and should be removed"
                false ->
                    "Deprecated compiler flag -Xuse-k2 overrides \"-language-version $explicitVersion\" to 2.0;" +
                            " please remove -Xuse-k2 and use -language-version to select either $explicitVersion or 2.0"
                null ->
                    "Compiler flag -Xuse-k2 is deprecated; please use \"-language-version 2.0\" instead"
            }
            collector.report(CompilerMessageSeverity.STRONG_WARNING, message)
        }
        return if (useK2 && !explicitOrDefaultVersion.usesK2) LanguageVersion.KOTLIN_2_0
        else explicitOrDefaultVersion
    }

    private fun parseVersion(collector: MessageCollector, value: String?, versionOf: String): LanguageVersion? =
        if (value == null) null
        else LanguageVersion.fromVersionString(value)
            ?: run {
                val versionStrings = LanguageVersion.entries.filterNot(LanguageVersion::isUnsupported).map(LanguageVersion::description)
                val message = "Unknown $versionOf version: $value\nSupported $versionOf versions: ${versionStrings.joinToString(", ")}"
                collector.report(CompilerMessageSeverity.ERROR, message, null)
                null
            }

    // Used only for serialize and deserialize settings. Don't use in other places!
    class DummyImpl : CommonCompilerArguments() {
        override fun copyOf(): Freezable = copyCommonCompilerArguments(this, DummyImpl())
    }
}
