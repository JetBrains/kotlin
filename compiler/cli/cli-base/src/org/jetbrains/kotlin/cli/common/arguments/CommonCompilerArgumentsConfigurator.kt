/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.CliDiagnostics
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory

open class CommonCompilerArgumentsConfigurator {
    interface Reporter {
        fun reportWarning(message: String)
        fun reportError(message: String)

        fun report(factory: KtSourcelessDiagnosticFactory, message: String)

        fun info(message: String)

        fun withLanguageVersionSettings(languageVersionSettings: LanguageVersionSettings): Reporter

        object DoNothing : Reporter {
            override fun reportWarning(message: String) {}
            override fun reportError(message: String) {}
            override fun report(factory: KtSourcelessDiagnosticFactory, message: String) {}
            override fun info(message: String) {}
            override fun withLanguageVersionSettings(languageVersionSettings: LanguageVersionSettings): Reporter = this
        }

        companion object
    }

    open fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> = with(arguments) {
        HashMap<AnalysisFlag<*>, Any>().apply {
            putAnalysisFlag(AnalysisFlags.skipMetadataVersionCheck, skipMetadataVersionCheck)
            putAnalysisFlag(AnalysisFlags.skipPrereleaseCheck, skipPrereleaseCheck || skipMetadataVersionCheck)
            putAnalysisFlag(AnalysisFlags.multiPlatformDoNotCheckActual, noCheckActual)
            putAnalysisFlag(AnalysisFlags.optIn, optIn?.toList().orEmpty())
            putAnalysisFlag(AnalysisFlags.escapingFunctionsList, parseEscapingFunctions(arguments, reporter))
            putAnalysisFlag(AnalysisFlags.skipExpectedActualDeclarationChecker, metadataKlib)
            putAnalysisFlag(AnalysisFlags.explicitApiVersion, apiVersion != null)
            ExplicitApiMode.fromString(explicitApi)?.also { putAnalysisFlag(AnalysisFlags.explicitApiMode, it) }
                ?: reporter.reportError(
                    "Unknown value for parameter -Xexplicit-api: '$explicitApi'. Value should be one of ${ExplicitApiMode.availableValues()}"
                )
            ExplicitApiMode.fromString(explicitReturnTypes)?.also { putAnalysisFlag(AnalysisFlags.explicitReturnTypes, it) }
                ?: reporter.reportError(
                    "Unknown value for parameter -XXexplicit-return-types: '$explicitReturnTypes'. Value should be one of ${ExplicitApiMode.availableValues()}"
                )
            putAnalysisFlag(AnalysisFlags.allowKotlinPackage, allowKotlinPackage)
            putAnalysisFlag(AnalysisFlags.stdlibCompilation, stdlibCompilation)
            putAnalysisFlag(AnalysisFlags.muteExpectActualClassesWarning, expectActualClasses)
            putAnalysisFlag(AnalysisFlags.allowFullyQualifiedNameInKClass, true)
            putAnalysisFlag(AnalysisFlags.dontWarnOnErrorSuppression, dontWarnOnErrorSuppression)
            putAnalysisFlag(AnalysisFlags.lenientMode, lenientMode)
            putAnalysisFlag(AnalysisFlags.headerMode, headerMode)
            HeaderMode.fromString(headerModeType)?.also { putAnalysisFlag(AnalysisFlags.headerModeType, it) }
                ?: reporter.reportError(
                    "Unknown value for parameter -Xheader-mode-type: '$headerModeType'. Value should be one of ${HeaderMode.availableValues()}"
                )
            putAnalysisFlag(AnalysisFlags.firAggressivePruning, firAggressivePruning ?: headerMode)
            putAnalysisFlag(AnalysisFlags.hierarchicalMultiplatformCompilation, separateKmpCompilationScheme && multiPlatform)
            putAnalysisFlag(AnalysisFlags.kmpJvmIncrementalCompilationEnabled, fragmentIncrementalClasspath.isNotEmpty() && multiPlatform)
            fillWarningLevelMap(arguments, reporter)
            ReturnValueCheckerMode.fromString(returnValueChecker)?.also { putAnalysisFlag(AnalysisFlags.returnValueCheckerMode, it) }
                ?: reporter.reportError(
                    "Unknown value for parameter -Xreturn-value-checker: '$returnValueChecker'. Value should be one of ${ReturnValueCheckerMode.availableValues()}"
                )
        }
    }

    protected fun MutableMap<AnalysisFlag<*>, Any>.putAnalysisFlag(flag: AnalysisFlag<*>, value: Any) {
        if (value == flag.defaultValue) {
            remove(flag)
        } else {
            this[flag] = value
        }
    }

    open fun configureLanguageFeatures(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ): MutableMap<LanguageFeature, LanguageFeature.State> = with(arguments) {
        HashMap<LanguageFeature, LanguageFeature.State>().apply {
            configureCommonLanguageFeatures(arguments)

            if (progressiveMode) {
                LanguageFeature.entries.filter { it.actuallyEnabledInProgressiveMode }.forEach {
                    // Don't overwrite other settings: users may want to turn off some particular
                    // breaking change manually instead of turning off whole progressive mode
                    if (!contains(it)) put(it, LanguageFeature.State.ENABLED)
                }
            }

            ReturnValueCheckerMode.fromString(returnValueChecker)?.also {
                if (it != ReturnValueCheckerMode.DISABLED)
                    put(LanguageFeature.UnnamedLocalVariables, LanguageFeature.State.ENABLED)
            }

            // Internal arguments should go last, because it may be useful to override
            // some feature state via -XX (even if some -X flags were passed)
            if (internalArguments.isNotEmpty()) {
                configureLanguageFeaturesFromInternalArgs(arguments, reporter, languageVersion)
            }

            configureExtraLanguageFeatures(arguments, this, reporter)
        }
    }

    protected open fun configureExtraLanguageFeatures(
        arguments: CommonCompilerArguments,
        map: HashMap<LanguageFeature, LanguageFeature.State>,
        reporter: Reporter
    ) {
    }

    private fun HashMap<LanguageFeature, LanguageFeature.State>.configureLanguageFeaturesFromInternalArgs(
        arguments: CommonCompilerArguments,
        reporter: Reporter,
        languageVersion: LanguageVersion,
    ) {
        val featuresThatForcePreReleaseBinaries = mutableListOf<LanguageFeature>()
        val disabledFeaturesFromUnsupportedVersions = mutableListOf<LanguageFeature>()

        var standaloneSamConversionFeaturePassedExplicitly = false
        var functionReferenceWithDefaultValueFeaturePassedExplicitly = false
        for ((val feature = languageFeature, val state) in arguments.internalArguments) {
            put(feature, state)
            if (state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled(languageVersion)) {
                featuresThatForcePreReleaseBinaries += feature
            }

            if (
                state == LanguageFeature.State.DISABLED &&
                feature.sinceVersion?.isUnsupported == true &&
                feature.behaviorAfterSinceVersion == LanguageFeatureBehaviorAfterSinceVersion.CannotBeDisabled
            ) {
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

        val isCrossModuleInlinerEnabled = this[LanguageFeature.IrCrossModuleInlinerBeforeKlibSerialization] == LanguageFeature.State.ENABLED
        val isIntraModuleInlinerEnabled = this[LanguageFeature.IrIntraModuleInlinerBeforeKlibSerialization] == LanguageFeature.State.ENABLED
        if (isCrossModuleInlinerEnabled && !isIntraModuleInlinerEnabled) {
            reporter.reportError(
                "-XXLanguage:+IrCrossModuleInlinerBeforeKlibSerialization requires -XXLanguage:+IrIntraModuleInlinerBeforeKlibSerialization. " +
                        "Enable the intra-module inliner as well to avoid inconsistent configuration."
            )
        }

        if (featuresThatForcePreReleaseBinaries.isNotEmpty()) {
            reporter.reportWarning(
                "Following manually enabled features will force generation of pre-release binaries: ${featuresThatForcePreReleaseBinaries.joinToString()}"
            )
        }

        if (disabledFeaturesFromUnsupportedVersions.isNotEmpty()) {
            reporter.reportError(
                "The following features cannot be disabled manually, because the version they first appeared in is no longer " +
                        "supported:\n${disabledFeaturesFromUnsupportedVersions.joinToString()}"
            )
        }
    }

    // Expected entry form: '(+|-)<fq.name>'.
    // Entries that don't start with '+' or '-', or have an empty name, are reported and skipped.
    private fun parseEscapingFunctions(arguments: CommonCompilerArguments, reporter: Reporter): List<String> {
        return arguments.escapingFunctions?.toList().orEmpty().filter { entry ->
            when (entry.firstOrNull()) {
                '+', '-' -> {
                    if (entry.length == 1) {
                        reporter.reportWarning("Empty callable name in -Xescaping-functions entry '$entry'.")
                        return@filter false
                    }
                    true
                }
                else -> {
                    reporter.reportWarning(
                        "Incorrect -Xescaping-functions entry '$entry', each entry must start with '+' (to add) or '-' (to remove)."
                    )
                    false
                }
            }
        }
    }

    private fun HashMap<AnalysisFlag<*>, Any>.fillWarningLevelMap(arguments: CommonCompilerArguments, reporter: Reporter) {
        val result = buildMap {
            @Suppress("DEPRECATION")
            val suppressedDiagnostics = arguments.suppressedDiagnostics
            suppressedDiagnostics.associateWithTo(this) { WarningLevel.Disabled }
            for (rawArgument in arguments.warningLevels) {
                val split = rawArgument.split(":", limit = 2)
                if (split.size < 2) {
                    reporter.reportError(
                        "Invalid argument for -Xwarning-level=$rawArgument"
                    )
                    continue
                }
                val [name, rawLevel] = split
                val level = WarningLevel.fromString(rawLevel) ?: run {
                    reporter.reportError(
                        "Incorrect value for warning level: $rawLevel. Available values are: ${WarningLevel.entries.joinToString { it.cliOption }}"
                    )
                    continue
                }
                val existing = put(name, level)
                if (existing != null) {
                    val message = if (name in suppressedDiagnostics) {
                        "Severity of $name is configured both with -Xwarning-level and -Xsuppress-warning flags"
                    } else {
                        "-Xwarning-level is duplicated for warning $name"
                    }
                    reporter.reportError(message)
                }
            }
        }
        putAnalysisFlag(AnalysisFlags.warningLevels, result)
    }
}

fun CommonCompilerArguments.toLanguageVersionSettings(reporter: CommonCompilerArgumentsConfigurator.Reporter): LanguageVersionSettings {
    return toLanguageVersionSettings(reporter, emptyMap())
}

fun CommonCompilerArguments.toLanguageVersionSettings(
    reporter: CommonCompilerArgumentsConfigurator.Reporter,
    additionalAnalysisFlags: Map<AnalysisFlag<*>, Any>,
): LanguageVersionSettings {
    val languageVersion = parseOrConfigureLanguageVersion(reporter)
    // If only "-language-version" is specified, API version is assumed to be equal to the language version
    // (API version cannot be greater than the language version)
    val apiVersion = ApiVersion.createByLanguageVersion(parseVersion(reporter, apiVersion, "API") ?: languageVersion)

    val languageVersionSettings = LanguageVersionSettingsImpl(
        languageVersion,
        apiVersion,
        configureAnalysisFlags(reporter, languageVersion) + additionalAnalysisFlags,
        configureLanguageFeatures(reporter, languageVersion)
    )

    val reporterWithProperWarningLevels = reporter.withLanguageVersionSettings(languageVersionSettings)
    checkApiAndLanguageVersion(languageVersion, apiVersion, reporterWithProperWarningLevels)
    checkExplicitApiAndExplicitReturnTypesAtTheSameTime(reporterWithProperWarningLevels)

    return languageVersionSettings
}

fun CommonCompilerArguments.checkApiAndLanguageVersion(
    language: LanguageVersion,
    api: ApiVersion,
    reporter: CommonCompilerArgumentsConfigurator.Reporter,
) {
    checkApiVersionIsNotGreaterThenLanguageVersion(language, api, reporter)
    checkLanguageVersionIsStable(language, reporter)
    checkOutdatedVersions(language, api, reporter)
    checkProgressiveMode(language, reporter)
}

private fun checkApiVersionIsNotGreaterThenLanguageVersion(
    languageVersion: LanguageVersion,
    apiVersion: ApiVersion,
    reporter: CommonCompilerArgumentsConfigurator.Reporter,
) {
    if (apiVersion > ApiVersion.createByLanguageVersion(languageVersion)) {
        reporter.reportError(
            "-api-version (${apiVersion.versionString}) cannot be greater than -language-version (${languageVersion.versionString})."
        )
    }
}

private fun CommonCompilerArguments.checkLanguageVersionIsStable(languageVersion: LanguageVersion, reporter: CommonCompilerArgumentsConfigurator.Reporter) {
    if (!languageVersion.isStable && !suppressVersionWarnings) {
        reporter.report(
            CliDiagnostics.EXPERIMENTAL_LANGUAGE_VERSION,
            "Language version ${languageVersion.versionString} is experimental, there are no backwards compatibility guarantees for " +
                    "new language and library features. " +
                    "Use the stable version ${LanguageVersion.LATEST_STABLE} instead."
        )
    }
}

private fun CommonCompilerArguments.checkOutdatedVersions(
    language: LanguageVersion,
    api: ApiVersion,
    reporter: CommonCompilerArgumentsConfigurator.Reporter,
) {
    val [version, supportedVersion, versionKind] = findOutdatedVersion(language, api) ?: return
    val firstNonDeprecated by lazy {
        when (versionKind) {
            VersionKind.LANGUAGE -> LanguageVersion.FIRST_NON_DEPRECATED
            VersionKind.API -> ApiVersion.FIRST_NON_DEPRECATED
        }
    }
    when {
        version.isUnsupported -> {
            reporter.report(
                CliDiagnostics.UNSUPPORTED_LANGUAGE_VERSION,
                "${versionKind.text} version ${version.versionString} is no longer supported; " +
                        "use version ${supportedVersion!!.versionString} or greater instead."
            )
        }
        version.isDeprecated && !suppressVersionWarnings -> {
            reporter.report(
                CliDiagnostics.DEPRECATED_LANGUAGE_VERSION,
                "${versionKind.text} version ${version.versionString} is deprecated " +
                        "and its support will be removed in a future version of Kotlin. " +
                        "Update the version to $firstNonDeprecated."
            )
        }
    }
}

private fun findOutdatedVersion(
    language: LanguageVersion,
    api: ApiVersion,
): Triple<LanguageOrApiVersion, LanguageOrApiVersion?, VersionKind>? {
    return when {
        language.isUnsupported -> Triple(language, LanguageVersion.FIRST_SUPPORTED, VersionKind.LANGUAGE)
        api.isUnsupported -> Triple(api, ApiVersion.FIRST_SUPPORTED, VersionKind.API)
        language.isDeprecated -> Triple(language, null, VersionKind.LANGUAGE)
        api.isDeprecated -> Triple(api, null, VersionKind.API)
        else -> null
    }
}

private fun CommonCompilerArguments.checkProgressiveMode(languageVersion: LanguageVersion, reporter: CommonCompilerArgumentsConfigurator.Reporter) {
    if (progressiveMode && languageVersion < LanguageVersion.LATEST_STABLE && !suppressVersionWarnings) {
        reporter.reportWarning(
            "'-progressive' is meaningful only for the latest language version (${LanguageVersion.LATEST_STABLE}), " +
                    "while this build uses $languageVersion\n" +
                    "Compiler behavior in such mode is undefined; consider moving to the latest stable version " +
                    "or turning off progressive mode."
        )
    }
}

private fun CommonCompilerArguments.checkExplicitApiAndExplicitReturnTypesAtTheSameTime(reporter: CommonCompilerArgumentsConfigurator.Reporter) {
    if (explicitApi == ExplicitApiMode.DISABLED.state || explicitReturnTypes == ExplicitApiMode.DISABLED.state) return
    if (explicitApi != explicitReturnTypes) {
        reporter.reportError(
            """
                    '-Xexplicit-api' and '-XXexplicit-return-types' flags cannot have different values at the same time.
                    Consider use only one of those flags
                    Passed:
                      '-Xexplicit-api=${explicitApi}'
                      '-XXexplicit-return-types=${explicitReturnTypes}'
                    """.trimIndent()
        )
    }
}

private enum class VersionKind(val text: String) {
    LANGUAGE("Language"), API("API")
}

private fun CommonCompilerArguments.parseOrConfigureLanguageVersion(reporter: CommonCompilerArgumentsConfigurator.Reporter): LanguageVersion {
    // If only "-api-version" is specified, language version is assumed to be the latest stable
    return parseVersion(reporter, languageVersion, "language") ?: LanguageVersion.LATEST_STABLE
}

private fun CommonCompilerArguments.parseVersion(reporter: CommonCompilerArgumentsConfigurator.Reporter, value: String?, versionOf: String): LanguageVersion? =
    if (value == null) null
    else LanguageVersion.fromVersionString(value)
        ?: run {
            val versionStrings = LanguageVersion.entries.filterNot(LanguageVersion::isUnsupported).map(LanguageVersion::description)
            val message = "Unknown $versionOf version: $value\nSupported $versionOf versions: ${versionStrings.joinToString(", ")}"
            reporter.reportError(message)
            null
        }

fun CommonCompilerArguments.configureAnalysisFlags(
    reporter: CommonCompilerArgumentsConfigurator.Reporter,
    languageVersion: LanguageVersion,
): MutableMap<AnalysisFlag<*>, Any> {
    return configurator.configureAnalysisFlags(this, reporter, languageVersion)
}

fun CommonCompilerArguments.configureLanguageFeatures(
    reporter: CommonCompilerArgumentsConfigurator.Reporter,
    languageVersion: LanguageVersion,
): MutableMap<LanguageFeature, LanguageFeature.State> {
    return configurator.configureLanguageFeatures(this, reporter, languageVersion)
}
