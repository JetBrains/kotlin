/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

open class CommonCompilerArgumentsConfigurator {
    @OptIn(IDEAPluginsCompatibilityAPI::class)
    open fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
        languageVersion: LanguageVersion,
    ): MutableMap<AnalysisFlag<*>, Any> {
        return with(arguments) {
            HashMap<AnalysisFlag<*>, Any>().apply {
                put(AnalysisFlags.skipMetadataVersionCheck, skipMetadataVersionCheck)
                put(AnalysisFlags.skipPrereleaseCheck, skipPrereleaseCheck || skipMetadataVersionCheck)
                put(AnalysisFlags.multiPlatformDoNotCheckActual, noCheckActual)
                put(AnalysisFlags.optIn, optIn?.toList().orEmpty())
                put(AnalysisFlags.skipExpectedActualDeclarationChecker, metadataKlib)
                put(AnalysisFlags.explicitApiVersion, apiVersion != null)
                ExplicitApiMode.fromString(explicitApi)?.also { put(AnalysisFlags.explicitApiMode, it) } ?: collector.report(
                    CompilerMessageSeverity.ERROR,
                    "Unknown value for parameter -Xexplicit-api: '$explicitApi'. Value should be one of ${ExplicitApiMode.availableValues()}"
                )
                ExplicitApiMode.fromString(explicitReturnTypes)?.also { put(AnalysisFlags.explicitReturnTypes, it) } ?: collector.report(
                    CompilerMessageSeverity.ERROR,
                    "Unknown value for parameter -XXexplicit-return-types: '$explicitReturnTypes'. Value should be one of ${ExplicitApiMode.availableValues()}"
                )
                put(AnalysisFlags.allowKotlinPackage, allowKotlinPackage)
                put(AnalysisFlags.stdlibCompilation, stdlibCompilation)
                put(AnalysisFlags.muteExpectActualClassesWarning, expectActualClasses)
                put(AnalysisFlags.allowFullyQualifiedNameInKClass, true)
                put(AnalysisFlags.dontWarnOnErrorSuppression, dontWarnOnErrorSuppression)
                put(AnalysisFlags.lenientMode, lenientMode)
                put(AnalysisFlags.hierarchicalMultiplatformCompilation, separateKmpCompilationScheme)
                fillWarningLevelMap(arguments, collector)
                ReturnValueCheckerMode.fromString(returnValueChecker)?.also { put(AnalysisFlags.returnValueCheckerMode, it) }
                    ?: collector.report(
                        CompilerMessageSeverity.ERROR,
                        "Unknown value for parameter -Xreturn-value-checker: '$returnValueChecker'. Value should be one of ${ReturnValueCheckerMode.availableValues()}"
                    )
            }
        }
    }

    open fun configureLanguageFeatures(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
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
                configureLanguageFeaturesFromInternalArgs(arguments, collector)
            }

            configureExtraLanguageFeatures(arguments, this)
        }
    }

    protected open fun configureExtraLanguageFeatures(arguments: CommonCompilerArguments, map: HashMap<LanguageFeature, LanguageFeature.State>) {}

    private fun HashMap<LanguageFeature, LanguageFeature.State>.configureLanguageFeaturesFromInternalArgs(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
    ) {
        val featuresThatForcePreReleaseBinaries = mutableListOf<LanguageFeature>()
        val disabledFeaturesFromUnsupportedVersions = mutableListOf<LanguageFeature>()

        var standaloneSamConversionFeaturePassedExplicitly = false
        var functionReferenceWithDefaultValueFeaturePassedExplicitly = false
        for ((feature, state) in arguments.internalArguments.filterIsInstance<ManualLanguageFeatureSetting>()) {
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

    private fun HashMap<AnalysisFlag<*>, Any>.fillWarningLevelMap(arguments: CommonCompilerArguments, collector: MessageCollector) {
        val result = buildMap {
            val suppressedDiagnostics = arguments.suppressedDiagnostics.orEmpty()
            suppressedDiagnostics.associateWithTo(this) { WarningLevel.Disabled }
            if (suppressedDiagnostics.isNotEmpty()) {
                val replacement = "-Xwarning-level=${suppressedDiagnostics.first()}:disabled"
                val suffix = if (suppressedDiagnostics.size > 1) " (and the same for other warnings)" else ""
                collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    """Argument "-Xsuppress-warning" is deprecated. Use "$replacement" instead$suffix"""
                )
            }
            for (rawArgument in arguments.warningLevels.orEmpty()) {
                val split = rawArgument.split(":", limit = 2)
                if (split.size < 2) {
                    collector.report(
                        CompilerMessageSeverity.ERROR,
                        "Invalid argument for -Xwarning-level=$rawArgument"
                    )
                    continue
                }
                val (name, rawLevel) = split
                val level = WarningLevel.fromString(rawLevel) ?: run {
                    collector.report(
                        CompilerMessageSeverity.ERROR,
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
                    collector.report(CompilerMessageSeverity.ERROR, message)
                }
            }
        }
        put(AnalysisFlags.warningLevels, result)
    }
}

fun CommonCompilerArguments.toLanguageVersionSettings(collector: MessageCollector): LanguageVersionSettings {
    return toLanguageVersionSettings(collector, emptyMap())
}

fun CommonCompilerArguments.toLanguageVersionSettings(
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

    checkExplicitApiAndExplicitReturnTypesAtTheSameTime(collector)

    return languageVersionSettings
}

private fun CommonCompilerArguments.checkApiVersionIsNotGreaterThenLanguageVersion(
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

fun CommonCompilerArguments.checkLanguageVersionIsStable(languageVersion: LanguageVersion, collector: MessageCollector) {
    if (!languageVersion.isStable && !suppressVersionWarnings) {
        collector.report(
            CompilerMessageSeverity.STRONG_WARNING,
            "Language version ${languageVersion.versionString} is experimental, there are no backwards compatibility guarantees for " +
                    "new language and library features"
        )
    }
}

private fun CommonCompilerArguments.checkOutdatedVersions(language: LanguageVersion, api: ApiVersion, collector: MessageCollector) {
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

private fun CommonCompilerArguments.checkProgressiveMode(languageVersion: LanguageVersion, collector: MessageCollector) {
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

private fun CommonCompilerArguments.checkExplicitApiAndExplicitReturnTypesAtTheSameTime(collector: MessageCollector) {
    if (explicitApi == ExplicitApiMode.DISABLED.state || explicitReturnTypes == ExplicitApiMode.DISABLED.state) return
    if (explicitApi != explicitReturnTypes) {
        collector.report(
            CompilerMessageSeverity.ERROR,
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

private fun CommonCompilerArguments.parseOrConfigureLanguageVersion(collector: MessageCollector): LanguageVersion {
    if (useK2) {
        collector.report(
            CompilerMessageSeverity.ERROR,
            "Compiler flag -Xuse-k2 is no more supported. " +
                    "Compiler versions 2.0+ use K2 by default, unless the language version is set to 1.9 or earlier"
        )
    }

    // If only "-api-version" is specified, language version is assumed to be the latest stable
    return parseVersion(collector, languageVersion, "language") ?: LanguageVersion.LATEST_STABLE
}

private fun CommonCompilerArguments.parseVersion(collector: MessageCollector, value: String?, versionOf: String): LanguageVersion? =
    if (value == null) null
    else LanguageVersion.fromVersionString(value)
        ?: run {
            val versionStrings = LanguageVersion.entries.filterNot(LanguageVersion::isUnsupported).map(LanguageVersion::description)
            val message = "Unknown $versionOf version: $value\nSupported $versionOf versions: ${versionStrings.joinToString(", ")}"
            collector.report(CompilerMessageSeverity.ERROR, message, null)
            null
        }

fun CommonCompilerArguments.configureAnalysisFlags(collector: MessageCollector, languageVersion: LanguageVersion): MutableMap<AnalysisFlag<*>, Any> {
    return configurator.configureAnalysisFlags(this, collector, languageVersion)
}

fun CommonCompilerArguments.configureLanguageFeatures(collector: MessageCollector): MutableMap<LanguageFeature, LanguageFeature.State> {
    return configurator.configureLanguageFeatures(this, collector)
}
