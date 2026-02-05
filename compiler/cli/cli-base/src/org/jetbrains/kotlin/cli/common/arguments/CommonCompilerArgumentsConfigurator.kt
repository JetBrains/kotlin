/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.*

open class CommonCompilerArgumentsConfigurator {
    interface Reporter {
        fun reportWarning(message: String)
        fun reportError(message: String)
        fun info(message: String)

        object DoNothing : Reporter {
            override fun reportWarning(message: String) {}
            override fun reportError(message: String) {}
            override fun info(message: String) {}
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
            putAnalysisFlag(AnalysisFlags.headerModeType, headerModeType)
            putAnalysisFlag(AnalysisFlags.hierarchicalMultiplatformCompilation, separateKmpCompilationScheme && multiPlatform)
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
                configureLanguageFeaturesFromInternalArgs(arguments, reporter)
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
        reporter: Reporter
    ) {
        val featuresThatForcePreReleaseBinaries = mutableListOf<LanguageFeature>()
        val disabledFeaturesFromUnsupportedVersions = mutableListOf<LanguageFeature>()

        var standaloneSamConversionFeaturePassedExplicitly = false
        var functionReferenceWithDefaultValueFeaturePassedExplicitly = false
        for ((feature, state) in arguments.internalArguments) {
            put(feature, state)
            if (state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()) {
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

    private fun HashMap<AnalysisFlag<*>, Any>.fillWarningLevelMap(arguments: CommonCompilerArguments, reporter: Reporter) {
        val result = buildMap {
            val suppressedDiagnostics = arguments.suppressedDiagnostics.orEmpty()
            suppressedDiagnostics.associateWithTo(this) { WarningLevel.Disabled }
            if (suppressedDiagnostics.isNotEmpty()) {
                val replacement = "-Xwarning-level=${suppressedDiagnostics.first()}:disabled"
                val suffix = if (suppressedDiagnostics.size > 1) " (and the same for other warnings)" else ""
                reporter.reportWarning(
                    """Argument "-Xsuppress-warning" is deprecated. Use "$replacement" instead$suffix"""
                )
            }
            for (rawArgument in arguments.warningLevels.orEmpty()) {
                val split = rawArgument.split(":", limit = 2)
                if (split.size < 2) {
                    reporter.reportError(
                        "Invalid argument for -Xwarning-level=$rawArgument"
                    )
                    continue
                }
                val (name, rawLevel) = split
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
        configureLanguageFeatures(reporter)
    )

    checkApiAndLanguageVersion(languageVersion, apiVersion, reporter)

    checkExplicitApiAndExplicitReturnTypesAtTheSameTime(reporter)

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

private fun CommonCompilerArguments.checkApiVersionIsNotGreaterThenLanguageVersion(
    languageVersion: LanguageVersion,
    apiVersion: ApiVersion,
    reporter: CommonCompilerArgumentsConfigurator.Reporter,
) {
    if (apiVersion > ApiVersion.createByLanguageVersion(languageVersion)) {
        if (!suppressApiVersionGreaterThanLanguageVersionError) {
            reporter.reportError(
                "-api-version (${apiVersion.versionString}) cannot be greater than -language-version (${languageVersion.versionString})."
            )
        }
    } else if (suppressApiVersionGreaterThanLanguageVersionError) {
        reporter.reportWarning(
            "-Xsuppress-api-version-greater-than-language-version-error was passed, but the API version (${apiVersion.versionString}) is not greater than the language version (${languageVersion.versionString})."
        )
    }
}

private fun CommonCompilerArguments.checkLanguageVersionIsStable(languageVersion: LanguageVersion, reporter: CommonCompilerArgumentsConfigurator.Reporter) {
    if (!languageVersion.isStable && !suppressVersionWarnings) {
        reporter.reportWarning(
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
    val (version, supportedVersion, versionKind) = findOutdatedVersion(language, api) ?: return
    val firstNonDeprecated by lazy {
        when (versionKind) {
            VersionKind.LANGUAGE -> LanguageVersion.FIRST_NON_DEPRECATED
            VersionKind.API -> ApiVersion.FIRST_NON_DEPRECATED
        }
    }
    when {
        version.isUnsupported -> {
            if ((!language.isJvmOnly || this !is K2JVMCompilerArguments)) {
                reporter.reportError(
                    "${versionKind.text} version ${version.versionString} is no longer supported; " +
                            "use version ${supportedVersion!!.versionString} or greater instead."
                )
            } else if (!suppressVersionWarnings) {
                reporter.reportWarning(
                    "${versionKind.text} version ${version.versionString} is deprecated in JVM " +
                            "and its support will be removed in a future version of Kotlin. " +
                            "Update the version to $firstNonDeprecated."
                )
            }
        }
        version.isDeprecated && !suppressVersionWarnings -> {
            reporter.reportWarning(
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
    if (useK2) {
        reporter.reportError(
            "Compiler flag -Xuse-k2 is no more supported. " +
                    "Compiler versions 2.0+ use K2 by default, unless the language version is set to 1.9 or earlier"
        )
    }

    // If only "-api-version" is specified, language version is assumed to be the latest stable
    return parseVersion(reporter, languageVersion, "language") ?: LanguageVersion.LATEST_STABLE
}

private fun CommonCompilerArguments.parseVersion(reporter: CommonCompilerArgumentsConfigurator.Reporter, value: String?, versionOf: String): LanguageVersion? =
    if (value == null) null
    else LanguageVersion.fromVersionString(value)
        ?: run {
            val entries = LanguageVersion.entries
            val versionStrings = if (versionOf == "API") {
                // TODO: this branch can be dropped again after KT-80590
                entries.map { ApiVersion.createByLanguageVersion(it) }.filterNot { it.isUnsupported }.map(ApiVersion::description)
            } else {
                entries.filterNot { it.isUnsupported && !it.isJvmOnly }.map(LanguageVersion::description)
            }
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

fun CommonCompilerArguments.configureLanguageFeatures(reporter: CommonCompilerArgumentsConfigurator.Reporter): MutableMap<LanguageFeature, LanguageFeature.State> {
    return configurator.configureLanguageFeatures(this, reporter)
}
