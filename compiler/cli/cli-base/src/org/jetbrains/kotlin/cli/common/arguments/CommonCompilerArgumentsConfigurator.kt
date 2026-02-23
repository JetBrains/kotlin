/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*

open class CommonCompilerArgumentsConfigurator {
    open fun configureAnalysisFlags(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
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
                ?: collector.report(
                    CompilerMessageSeverity.ERROR,
                    "Unknown value for parameter -Xexplicit-api: '$explicitApi'. Value should be one of ${ExplicitApiMode.availableValues()}"
                )
            ExplicitApiMode.fromString(explicitReturnTypes)?.also { putAnalysisFlag(AnalysisFlags.explicitReturnTypes, it) }
                ?: collector.report(
                    CompilerMessageSeverity.ERROR,
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
            fillWarningLevelMap(arguments, collector)
            ReturnValueCheckerMode.fromString(returnValueChecker)?.also { putAnalysisFlag(AnalysisFlags.returnValueCheckerMode, it) }
                ?: collector.report(
                    CompilerMessageSeverity.ERROR,
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

            configureExtraLanguageFeatures(arguments, this, collector)
        }
    }

    protected open fun configureExtraLanguageFeatures(
        arguments: CommonCompilerArguments,
        map: HashMap<LanguageFeature, LanguageFeature.State>,
        collector: MessageCollector,
    ) {
    }

    private fun HashMap<LanguageFeature, LanguageFeature.State>.configureLanguageFeaturesFromInternalArgs(
        arguments: CommonCompilerArguments,
        collector: MessageCollector,
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
            collector.report(
                CompilerMessageSeverity.ERROR,
                "-XXLanguage:+IrCrossModuleInlinerBeforeKlibSerialization requires -XXLanguage:+IrIntraModuleInlinerBeforeKlibSerialization. " +
                        "Enable the intra-module inliner as well to avoid inconsistent configuration."
            )
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
        putAnalysisFlag(AnalysisFlags.warningLevels, result)
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

    val languageVersionSettings = LanguageVersionSettingsImpl(
        languageVersion,
        apiVersion,
        configureAnalysisFlags(collector, languageVersion) + additionalAnalysisFlags,
        configureLanguageFeatures(collector)
    )

    checkApiAndLanguageVersion(languageVersion, apiVersion, collector)

    checkExplicitApiAndExplicitReturnTypesAtTheSameTime(collector)

    return languageVersionSettings
}

fun CommonCompilerArguments.checkApiAndLanguageVersion(language: LanguageVersion, api: ApiVersion, collector: MessageCollector) {
    checkApiVersionIsNotGreaterThenLanguageVersion(language, api, collector)
    checkLanguageVersionIsStable(language, collector)
    checkOutdatedVersions(language, api, collector)
    checkProgressiveMode(language, collector)
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
                "-api-version (${apiVersion.versionString}) cannot be greater than -language-version (${languageVersion.versionString})."
            )
        }
    } else if (suppressApiVersionGreaterThanLanguageVersionError) {
        collector.report(
            WARNING,
            "-Xsuppress-api-version-greater-than-language-version-error was passed, but the API version (${apiVersion.versionString}) is not greater than the language version (${languageVersion.versionString})."
        )
    }
}

private fun CommonCompilerArguments.checkLanguageVersionIsStable(languageVersion: LanguageVersion, collector: MessageCollector) {
    if (!languageVersion.isStable && !suppressVersionWarnings) {
        collector.report(
            CompilerMessageSeverity.STRONG_WARNING,
            "Language version ${languageVersion.versionString} is experimental, there are no backwards compatibility guarantees for " +
                    "new language and library features. " +
                    "Use the stable version ${LanguageVersion.LATEST_STABLE} instead."
        )
    }
}

private fun CommonCompilerArguments.checkOutdatedVersions(language: LanguageVersion, api: ApiVersion, collector: MessageCollector) {
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
                collector.report(
                    CompilerMessageSeverity.ERROR,
                    "${versionKind.text} version ${version.versionString} is no longer supported; " +
                            "use version ${supportedVersion!!.versionString} or greater instead."
                )
            } else if (!suppressVersionWarnings) {
                collector.report(
                    CompilerMessageSeverity.STRONG_WARNING,
                    "${versionKind.text} version ${version.versionString} is deprecated in JVM " +
                            "and its support will be removed in a future version of Kotlin. " +
                            "Update the version to $firstNonDeprecated."
                )
            }
        }
        version.isDeprecated && !suppressVersionWarnings -> {
            collector.report(
                CompilerMessageSeverity.STRONG_WARNING,
                "${versionKind.text} version ${version.versionString} is deprecated " +
                        "and its support will be removed in a future version of Kotlin. " +
                        "Update the version to $firstNonDeprecated."
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
                    "Compiler behavior in such mode is undefined; consider moving to the latest stable version " +
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
            val entries = LanguageVersion.entries
            val versionStrings = if (versionOf == "API") {
                // TODO: this branch can be dropped again after KT-80590
                entries.map { ApiVersion.createByLanguageVersion(it) }.filterNot { it.isUnsupported }.map(ApiVersion::description)
            } else {
                entries.filterNot { it.isUnsupported && !it.isJvmOnly }.map(LanguageVersion::description)
            }
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
