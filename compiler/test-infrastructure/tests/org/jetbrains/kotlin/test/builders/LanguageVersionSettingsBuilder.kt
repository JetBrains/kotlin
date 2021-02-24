/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.DefaultsDsl
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.util.LANGUAGE_FEATURE_PATTERN
import org.jetbrains.kotlin.utils.addToStdlib.runIf

@DefaultsDsl
class LanguageVersionSettingsBuilder {
    companion object {
        fun fromExistingSettings(builder: LanguageVersionSettingsBuilder): LanguageVersionSettingsBuilder {
            return LanguageVersionSettingsBuilder().apply {
                languageVersion = builder.languageVersion
                apiVersion = builder.apiVersion
                specificFeatures += builder.specificFeatures
                analysisFlags += builder.analysisFlags
            }
        }
    }

    var languageVersion: LanguageVersion = LanguageVersion.LATEST_STABLE
    var apiVersion: ApiVersion = ApiVersion.LATEST_STABLE

    private val specificFeatures: MutableMap<LanguageFeature, LanguageFeature.State> = mutableMapOf()
    private val analysisFlags: MutableMap<AnalysisFlag<*>, Any?> = mutableMapOf()

    fun enable(feature: LanguageFeature) {
        specificFeatures[feature] = LanguageFeature.State.ENABLED
    }

    fun enableWithWarning(feature: LanguageFeature) {
        specificFeatures[feature] = LanguageFeature.State.ENABLED_WITH_WARNING
    }

    fun disable(feature: LanguageFeature) {
        specificFeatures[feature] = LanguageFeature.State.DISABLED
    }

    fun <T> withFlag(flag: AnalysisFlag<T>, value: T) {
        analysisFlags[flag] = value
    }

    fun configureUsingDirectives(directives: RegisteredDirectives, environmentConfigurators: List<EnvironmentConfigurator>) {
        val apiVersion = directives.singleOrZeroValue(LanguageSettingsDirectives.API_VERSION)
        if (apiVersion != null) {
            this.apiVersion = apiVersion
            val languageVersion = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.fromVersionString(apiVersion.versionString)!!)
            this.languageVersion = languageVersion
        }

        val analysisFlags = listOfNotNull(
            analysisFlag(AnalysisFlags.experimental, directives[LanguageSettingsDirectives.EXPERIMENTAL].takeIf { it.isNotEmpty() }),
            analysisFlag(AnalysisFlags.useExperimental, directives[LanguageSettingsDirectives.USE_EXPERIMENTAL].takeIf { it.isNotEmpty() }),
            analysisFlag(AnalysisFlags.ignoreDataFlowInAssert, trueOrNull(LanguageSettingsDirectives.IGNORE_DATA_FLOW_IN_ASSERT in directives)),
            analysisFlag(AnalysisFlags.constraintSystemForOverloadResolution, directives.singleOrZeroValue(LanguageSettingsDirectives.CONSTRAINT_SYSTEM_FOR_OVERLOAD_RESOLUTION)),
            analysisFlag(AnalysisFlags.allowResultReturnType, trueOrNull(LanguageSettingsDirectives.ALLOW_RESULT_RETURN_TYPE in directives)),
            analysisFlag(AnalysisFlags.explicitApiMode, directives.singleOrZeroValue(LanguageSettingsDirectives.EXPLICIT_API_MODE)),

            analysisFlag(JvmAnalysisFlags.jvmDefaultMode, directives.singleOrZeroValue(LanguageSettingsDirectives.JVM_DEFAULT_MODE)),
            analysisFlag(JvmAnalysisFlags.inheritMultifileParts, trueOrNull(LanguageSettingsDirectives.INHERIT_MULTIFILE_PARTS in directives)),
            analysisFlag(JvmAnalysisFlags.sanitizeParentheses, trueOrNull(LanguageSettingsDirectives.SANITIZE_PARENTHESES in directives)),
            analysisFlag(JvmAnalysisFlags.enableJvmPreview, trueOrNull(LanguageSettingsDirectives.ENABLE_JVM_PREVIEW in directives)),

            analysisFlag(AnalysisFlags.explicitApiVersion, trueOrNull(apiVersion != null)),
        )

        analysisFlags.forEach { withFlag(it.first, it.second) }

        environmentConfigurators.forEach {
            it.provideAdditionalAnalysisFlags(directives).entries.forEach { (flag, value) ->
                withFlag(flag, value)
            }
        }

        directives[LanguageSettingsDirectives.LANGUAGE].forEach { parseLanguageFeature(it) }
    }

    private fun parseLanguageFeature(featureString: String) {
        val matcher = LANGUAGE_FEATURE_PATTERN.matcher(featureString)
        if (!matcher.find()) {
            error(
                """Wrong syntax in the '// !${LanguageSettingsDirectives.LANGUAGE.name}: ...' directive:
                   found: '$featureString'
                   Must be '((+|-|warn:)LanguageFeatureName)+'
                   where '+' means 'enable', '-' means 'disable', 'warn:' means 'enable with warning'
                   and language feature names are names of enum entries in LanguageFeature enum class"""
            )
        }
        val mode = when (val mode = matcher.group(1)) {
            "+" -> LanguageFeature.State.ENABLED
            "-" -> LanguageFeature.State.DISABLED
            "warn:" -> LanguageFeature.State.ENABLED_WITH_WARNING
            else -> error("Unknown mode for language feature: $mode")
        }
        val name = matcher.group(2)
        val feature = LanguageFeature.fromString(name) ?: error("Language feature with name \"$name\" not found")
        specificFeatures[feature] = mode
    }

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
    private fun <T : Any> analysisFlag(flag: AnalysisFlag<T>, value: @kotlin.internal.NoInfer T?): Pair<AnalysisFlag<T>, T>? =
        value?.let(flag::to)

    private fun trueOrNull(condition: Boolean): Boolean? = runIf(condition) { true }

    fun build(): LanguageVersionSettings {
        return LanguageVersionSettingsImpl(languageVersion, apiVersion, analysisFlags, specificFeatures)
    }
}

