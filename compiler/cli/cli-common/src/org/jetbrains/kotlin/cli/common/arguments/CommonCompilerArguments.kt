/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.util.xmlb.annotations.Transient
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import java.util.*

@SuppressWarnings("WeakerAccess")
abstract class CommonCompilerArguments : CommonToolArguments() {
    companion object {
        @JvmStatic
        private val serialVersionUID = 0L

        const val PLUGIN_OPTION_FORMAT = "plugin:<pluginId>:<optionName>=<value>"

        const val WARN = "warn"
        const val ERROR = "error"
        const val ENABLE = "enable"
        const val DEFAULT = "default"
    }

    @get:Transient
    var autoAdvanceLanguageVersion: Boolean by FreezableVar(true)

    @GradleOption(DefaultValues.LanguageVersions::class)
    @Argument(
        value = "-language-version",
        valueDescription = "<version>",
        description = "Provide source compatibility with the specified version of Kotlin"
    )
    var languageVersion: String? by NullableStringFreezableVar(null)

    @get:Transient
    var autoAdvanceApiVersion: Boolean by FreezableVar(true)

    @GradleOption(DefaultValues.LanguageVersions::class)
    @Argument(
        value = "-api-version",
        valueDescription = "<version>",
        description = "Allow using declarations only from the specified version of bundled libraries"
    )
    var apiVersion: String? by NullableStringFreezableVar(null)

    @Argument(
        value = "-kotlin-home",
        valueDescription = "<path>",
        description = "Path to the home directory of Kotlin compiler used for discovery of runtime libraries"
    )
    var kotlinHome: String? by NullableStringFreezableVar(null)

    @Argument(
        value = "-progressive",
        deprecatedName = "-Xprogressive",
        description = "Enable progressive compiler mode.\n" +
                "In this mode, deprecations and bug fixes for unstable code take effect immediately,\n" +
                "instead of going through a graceful migration cycle.\n" +
                "Code written in the progressive mode is backward compatible; however, code written in\n" +
                "non-progressive mode may cause compilation errors in the progressive mode."
    )
    var progressiveMode by FreezableVar(false)

    @Argument(value = "-script", description = "Evaluate the given Kotlin script (*.kts) file")
    var script: Boolean by FreezableVar(false)

    @Argument(value = "-P", valueDescription = PLUGIN_OPTION_FORMAT, description = "Pass an option to a plugin")
    var pluginOptions: Array<String>? by FreezableVar(null)

    // Advanced options

    @Argument(value = "-Xno-inline", description = "Disable method inlining")
    var noInline: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xskip-metadata-version-check",
        description = "Allow to load classes with bad metadata version and pre-release classes"
    )
    var skipMetadataVersionCheck: Boolean by FreezableVar(false)

    @Argument(value = "-Xskip-prerelease-check", description = "Allow to load pre-release classes")
    var skipPrereleaseCheck: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xallow-kotlin-package",
        description = "Allow compiling code in package 'kotlin' and allow not requiring kotlin.stdlib in module-info"
    )
    var allowKotlinPackage: Boolean by FreezableVar(false)

    @Argument(value = "-Xreport-output-files", description = "Report source to output files mapping")
    var reportOutputFiles: Boolean by FreezableVar(false)

    @Argument(value = "-Xplugin", valueDescription = "<path>", description = "Load plugins from the given classpath")
    var pluginClasspaths: Array<String>? by FreezableVar(null)

    @Argument(value = "-Xmulti-platform", description = "Enable experimental language support for multi-platform projects")
    var multiPlatform: Boolean by FreezableVar(false)

    @Argument(value = "-Xno-check-actual", description = "Do not check presence of 'actual' modifier in multi-platform projects")
    var noCheckActual: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xintellij-plugin-root",
        valueDescription = "<path>",
        description = "Path to the kotlin-compiler.jar or directory where IntelliJ configuration files can be found"
    )
    var intellijPluginRoot: String? by NullableStringFreezableVar(null)

    @Argument(
        value = "-Xnew-inference",
        description = "Enable new experimental generic type inference algorithm"
    )
    var newInference: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xinline-classes",
        description = "Enable experimental inline classes"
    )
    var inlineClasses: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xpolymorphic-signature",
        description = "Enable experimental support for @PolymorphicSignature (MethodHandle/VarHandle)"
    )
    var polymorphicSignature: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xlegacy-smart-cast-after-try",
        description = "Allow var smart casts despite assignment in try block"
    )
    var legacySmartCastAfterTry: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xeffect-system",
        description = "Enable experimental language feature: effect system"
    )
    var effectSystem: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xread-deserialized-contracts",
        description = "Enable reading of contracts from metadata"
    )
    var readDeserializedContracts: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xexperimental",
        valueDescription = "<fq.name>",
        description = "Enable and propagate usages of experimental API for marker annotation with the given fully qualified name"
    )
    var experimental: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xuse-experimental",
        valueDescription = "<fq.name>",
        description = "Enable, but don't propagate usages of experimental API for marker annotation with the given fully qualified name"
    )
    var useExperimental: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xopt-in",
        valueDescription = "<fq.name>",
        description = "Enable usages of API that requires opt-in with an opt-in requirement marker with the given fully qualified name"
    )
    var optIn: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xproper-ieee754-comparisons",
        description = "Generate proper IEEE 754 comparisons in all cases if values are statically known to be of primitive numeric types"
    )
    var properIeee754Comparisons by FreezableVar(false)

    @Argument(value = "-Xreport-perf", description = "Report detailed performance statistics")
    var reportPerf: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xdump-perf",
        valueDescription = "<path>",
        description = "Dump detailed performance statistics to the specified file"
    )
    var dumpPerf: String? by NullableStringFreezableVar(null)

    @Argument(
        value = "-Xmetadata-version",
        description = "Change metadata version of the generated binary files"
    )
    var metadataVersion: String? by FreezableVar(null)

    @Argument(
        value = "-Xcommon-sources",
        valueDescription = "<path>",
        description = "Sources of the common module that need to be compiled together with this module in the multi-platform mode.\n" +
                "Should be a subset of sources passed as free arguments"
    )
    var commonSources: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xallow-result-return-type",
        description = "Allow compiling code when `kotlin.Result` is used as a return type"
    )
    var allowResultReturnType: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xlist-phases",
        description = "List backend phases"
    )
    var listPhases: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xdisable-phases",
        description = "Disable backend phases"
    )
    var disablePhases: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xverbose-phases",
        description = "Be verbose while performing these backend phases"
    )
    var verbosePhases: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xphases-to-dump-before",
        description = "Dump backend state before these phases"
    )
    var phasesToDumpBefore: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xphases-to-dump-after",
        description = "Dump backend state after these phases"
    )
    var phasesToDumpAfter: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xphases-to-dump",
        description = "Dump backend state both before and after these phases"
    )
    var phasesToDump: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xexclude-from-dumping",
        description = "Names of elements that should not be dumped"
    )
    var namesExcludedFromDumping: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xdump-directory",
        description = "Dump backend state into directory"
    )
    var dumpDirectory: String? by FreezableVar(null)

    @Argument(
        value = "-Xdump-fqname",
        description = "FqName of declaration that should be dumped"
    )
    var dumpOnlyFqName: String? by FreezableVar(null)

    @Argument(
        value = "-Xphases-to-validate-before",
        description = "Validate backend state before these phases"
    )
    var phasesToValidateBefore: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xphases-to-validate-after",
        description = "Validate backend state after these phases"
    )
    var phasesToValidateAfter: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xphases-to-validate",
        description = "Validate backend state both before and after these phases"
    )
    var phasesToValidate: Array<String>? by FreezableVar(null)

    @Argument(
        value = "-Xprofile-phases",
        description = "Profile backend phases"
    )
    var profilePhases: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xcheck-phase-conditions",
        description = "Check pre- and postconditions on phases"
    )
    var checkPhaseConditions: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xcheck-sticky-phase-conditions",
        description = "Run sticky condition checks on subsequent phases as well. Implies -Xcheck-phase-conditions"
    )
    var checkStickyPhaseConditions: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xuse-fir",
        description = "Compile using Front-end IR. Warning: this feature is far from being production-ready"
    )
    var useFir: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xuse-fir-extended-checkers",
        description = "Use extended analysis mode based on Front-end IR. Warning: this feature is far from being production-ready"
    )
    var useFirExtendedCheckers: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xdisable-ultra-light-classes",
        description = "Do not use the ultra light classes implementation"
    )
    var disableUltraLightClasses: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xuse-mixed-named-arguments",
        description = "Enable Support named arguments in their own position even if the result appears as mixed"
    )
    var useMixedNamedArguments: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xexpect-actual-linker",
        description = "Enable experimental expect/actual linker"
    )
    var expectActualLinker: Boolean by FreezableVar(false)

    @Argument(value = "-Xdisable-default-scripting-plugin", description = "Do not enable scripting plugin by default")
    var disableDefaultScriptingPlugin: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xexplicit-api",
        valueDescription = "{strict|warning|disable}",
        description = "Force compiler to report errors on all public API declarations without explicit visibility or return type.\n" +
                "Use 'warning' level to issue warnings instead of errors."
    )
    var explicitApi: String by FreezableVar(ExplicitApiMode.DISABLED.state)

    @Argument(
        value = "-Xinference-compatibility",
        description = "Enable compatibility changes for generic type inference algorithm"
    )
    var inferenceCompatibility: Boolean by FreezableVar(false)

    @Argument(
        value = "-Xsuppress-version-warnings",
        description = "Suppress warnings about outdated, inconsistent or experimental language or API versions"
    )
    var suppressVersionWarnings: Boolean by FreezableVar(false)

    open fun configureAnalysisFlags(collector: MessageCollector): MutableMap<AnalysisFlag<*>, Any> {
        return HashMap<AnalysisFlag<*>, Any>().apply {
            put(AnalysisFlags.skipMetadataVersionCheck, skipMetadataVersionCheck)
            put(AnalysisFlags.skipPrereleaseCheck, skipPrereleaseCheck || skipMetadataVersionCheck)
            put(AnalysisFlags.multiPlatformDoNotCheckActual, noCheckActual)
            val experimentalFqNames = experimental?.toList().orEmpty()
            if (experimentalFqNames.isNotEmpty()) {
                put(AnalysisFlags.experimental, experimentalFqNames)
                collector.report(CompilerMessageSeverity.WARNING, "'-Xexperimental' is deprecated and will be removed in a future release")
            }
            put(AnalysisFlags.useExperimental, useExperimental?.toList().orEmpty() + optIn?.toList().orEmpty())
            put(AnalysisFlags.expectActualLinker, expectActualLinker)
            put(AnalysisFlags.explicitApiVersion, apiVersion != null)
            put(AnalysisFlags.allowResultReturnType, allowResultReturnType)
            ExplicitApiMode.fromString(explicitApi)?.also { put(AnalysisFlags.explicitApiMode, it) } ?: collector.report(
                CompilerMessageSeverity.ERROR,
                "Unknown value for parameter -Xexplicit-api: '$explicitApi'. Value should be one of ${ExplicitApiMode.availableValues()}"
            )
        }
    }

    open fun configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> =
        HashMap<LanguageFeature, LanguageFeature.State>().apply {
            if (multiPlatform) {
                put(LanguageFeature.MultiPlatformProjects, LanguageFeature.State.ENABLED)
            }

            if (newInference) {
                put(LanguageFeature.NewInference, LanguageFeature.State.ENABLED)
                put(LanguageFeature.SamConversionPerArgument, LanguageFeature.State.ENABLED)
                put(LanguageFeature.FunctionReferenceWithDefaultValueAsOtherType, LanguageFeature.State.ENABLED)
                put(LanguageFeature.DisableCompatibilityModeForNewInference, LanguageFeature.State.ENABLED)
            }

            if (inlineClasses) {
                put(LanguageFeature.InlineClasses, LanguageFeature.State.ENABLED)
            }

            if (polymorphicSignature) {
                put(LanguageFeature.PolymorphicSignature, LanguageFeature.State.ENABLED)
            }

            if (legacySmartCastAfterTry) {
                put(LanguageFeature.SoundSmartCastsAfterTry, LanguageFeature.State.DISABLED)
            }

            if (effectSystem) {
                put(LanguageFeature.UseCallsInPlaceEffect, LanguageFeature.State.ENABLED)
                put(LanguageFeature.UseReturnsEffect, LanguageFeature.State.ENABLED)
            }

            if (readDeserializedContracts) {
                put(LanguageFeature.ReadDeserializedContracts, LanguageFeature.State.ENABLED)
            }

            if (properIeee754Comparisons) {
                put(LanguageFeature.ProperIeee754Comparisons, LanguageFeature.State.ENABLED)
            }

            if (useMixedNamedArguments) {
                put(LanguageFeature.MixedNamedArgumentsInTheirOwnPosition, LanguageFeature.State.ENABLED)
            }

            if (inferenceCompatibility) {
                put(LanguageFeature.InferenceCompatibility, LanguageFeature.State.ENABLED)
            }

            if (progressiveMode) {
                LanguageFeature.values().filter { it.kind.enabledInProgressiveMode }.forEach {
                    // Don't overwrite other settings: users may want to turn off some particular
                    // breaking change manually instead of turning off whole progressive mode
                    if (!contains(it)) put(it, LanguageFeature.State.ENABLED)
                }
            }

            // Internal arguments should go last, because it may be useful to override
            // some feature state via -XX (even if some -X flags were passed)
            if (internalArguments.isNotEmpty()) {
                configureLanguageFeaturesFromInternalArgs(collector)
            }
        }

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

        // If only "-api-version" is specified, language version is assumed to be the latest stable
        val languageVersion = parseVersion(collector, languageVersion, "language") ?: LanguageVersion.LATEST_STABLE

        // If only "-language-version" is specified, API version is assumed to be equal to the language version
        // (API version cannot be greater than the language version)
        val apiVersion = parseVersion(collector, apiVersion, "API") ?: languageVersion

        checkApiVersionIsNotGreaterThenLanguageVersion(languageVersion, apiVersion, collector)

        val languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion,
            ApiVersion.createByLanguageVersion(apiVersion),
            configureAnalysisFlags(collector),
            configureLanguageFeatures(collector)
        )

        if (!suppressVersionWarnings) {
            checkLanguageVersionIsStable(languageVersion, collector)
            checkOutdatedVersions(languageVersion, apiVersion, collector)
            checkProgressiveMode(languageVersion, collector)

            checkIrSupport(languageVersionSettings, collector)
        }

        return languageVersionSettings
    }

    private fun checkApiVersionIsNotGreaterThenLanguageVersion(
        languageVersion: LanguageVersion,
        apiVersion: LanguageVersion,
        collector: MessageCollector
    ) {
        if (apiVersion > languageVersion) {
            collector.report(
                CompilerMessageSeverity.ERROR,
                "-api-version (${apiVersion.versionString}) cannot be greater than -language-version (${languageVersion.versionString})"
            )
        }
    }

    private fun checkLanguageVersionIsStable(languageVersion: LanguageVersion, collector: MessageCollector) {
        if (!languageVersion.isStable) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "Language version ${languageVersion.versionString} is experimental, there are no backwards compatibility guarantees for " +
                        "new language and library features"
            )
        }
    }

    private fun checkOutdatedVersions(language: LanguageVersion, api: LanguageVersion, collector: MessageCollector) {
        val (version, versionKind) = findOutdatedVersion(language, api) ?: return
        when {
            version.isUnsupported -> {
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "${versionKind.text} version ${version.versionString} is no longer supported; " +
                            "please, use version ${LanguageVersion.OLDEST_DEPRECATED.versionString} or greater."
                )
            }
            version.isDeprecated -> {
                collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "${versionKind.text} version ${version.versionString} is deprecated " +
                            "and its support will be removed in a future version of Kotlin"
                )
            }
        }
    }

    private fun findOutdatedVersion(language: LanguageVersion, api: LanguageVersion): Pair<LanguageVersion, VersionKind>? {
        return when {
            language.isUnsupported -> language to VersionKind.LANGUAGE
            api.isUnsupported -> api to VersionKind.API
            language.isDeprecated -> language to VersionKind.LANGUAGE
            api.isDeprecated -> api to VersionKind.API
            else -> null
        }
    }

    private fun checkProgressiveMode(languageVersion: LanguageVersion, collector: MessageCollector) {
        if (progressiveMode && languageVersion < LanguageVersion.LATEST_STABLE) {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "'-progressive' is meaningful only for the latest language version (${LanguageVersion.LATEST_STABLE}), " +
                        "while this build uses $languageVersion\n" +
                        "Compiler behavior in such mode is undefined; please, consider moving to the latest stable version " +
                        "or turning off progressive mode."
            )
        }
    }

    protected open fun checkIrSupport(languageVersionSettings: LanguageVersionSettings, collector: MessageCollector) {
        // backend-specific
    }

    private enum class VersionKind(val text: String) {
        LANGUAGE("Language"), API("API")
    }

    private fun parseVersion(collector: MessageCollector, value: String?, versionOf: String): LanguageVersion? =
        if (value == null) null
        else LanguageVersion.fromVersionString(value)
            ?: run {
                val versionStrings = LanguageVersion.values().filterNot(LanguageVersion::isUnsupported).map(LanguageVersion::description)
                val message = "Unknown $versionOf version: $value\nSupported $versionOf versions: ${versionStrings.joinToString(", ")}"
                collector.report(CompilerMessageSeverity.ERROR, message, null)
                null
            }

    // Used only for serialize and deserialize settings. Don't use in other places!
    class DummyImpl : CommonCompilerArguments()
}
