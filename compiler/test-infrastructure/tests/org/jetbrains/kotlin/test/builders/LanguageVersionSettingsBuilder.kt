/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.jetbrains.kotlin.test.directives.model.singleOrZeroValue
import org.jetbrains.kotlin.test.services.DefaultsDsl
import org.jetbrains.kotlin.test.services.AbstractEnvironmentConfigurator
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

    fun configureUsingDirectives(
        directives: RegisteredDirectives,
        environmentConfigurators: List<AbstractEnvironmentConfigurator>,
        targetBackend: TargetBackend?,
        useK2: Boolean
    ) {
        val apiVersion = directives.singleOrZeroValue(LanguageSettingsDirectives.API_VERSION)
        if (apiVersion != null) {
            this.apiVersion = apiVersion
            val languageVersion = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.fromVersionString(apiVersion.versionString)!!)
            this.languageVersion = languageVersion
        }
        val languageVersionDirective = directives.singleOrZeroValue(LanguageSettingsDirectives.LANGUAGE_VERSION)
        val allowDangerousLanguageVersionTesting =
            directives.contains(LanguageSettingsDirectives.ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING)
        if (languageVersionDirective != null) {
            if (!allowDangerousLanguageVersionTesting) {
                error(
                    """
                        The LANGUAGE_VERSION directive is prone to limiting test to a specific language version,
                        which will become obsolete at some point and the test won't check things like feature
                        intersection with newer releases.

                        For language feature testing, use `// !LANGUAGE: [+-]FeatureName` directive instead,
                        where FeatureName is an entry of the enum `LanguageFeature`

                        If you are really sure you need to pin language versions, use the LANGUAGE_VERSION
                        directive in combination with the ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING directive.
                    """.trimIndent()
                )
            }
            languageVersion = languageVersionDirective
            if (languageVersion < LanguageVersion.fromVersionString(this.apiVersion.versionString)!!) {
                error(
                    """
                        Language version must be larger than or equal to the API version.
                        Language version: '$languageVersion'.
                        API version: '$apiVersion'.
                    """.trimIndent()
                )
            }
        }
        when {
            useK2 && this.languageVersion < LanguageVersion.KOTLIN_2_0 -> this.languageVersion = LanguageVersion.LATEST_STABLE
            !useK2 && this.languageVersion > LanguageVersion.KOTLIN_1_9 -> this.languageVersion = LanguageVersion.KOTLIN_1_9
        }

        val analysisFlags = listOfNotNull(
            analysisFlag(AnalysisFlags.optIn, directives[LanguageSettingsDirectives.OPT_IN].takeIf { it.isNotEmpty() }),
            analysisFlag(AnalysisFlags.ignoreDataFlowInAssert, trueOrNull(LanguageSettingsDirectives.IGNORE_DATA_FLOW_IN_ASSERT in directives)),
            analysisFlag(AnalysisFlags.allowResultReturnType, trueOrNull(LanguageSettingsDirectives.ALLOW_RESULT_RETURN_TYPE in directives)),
            analysisFlag(AnalysisFlags.explicitApiMode, directives.singleOrZeroValue(LanguageSettingsDirectives.EXPLICIT_API_MODE)),
            analysisFlag(AnalysisFlags.allowKotlinPackage, trueOrNull(LanguageSettingsDirectives.ALLOW_KOTLIN_PACKAGE in directives)),
            analysisFlag(AnalysisFlags.muteExpectActualClassesWarning, trueOrNull(LanguageSettingsDirectives.ENABLE_EXPECT_ACTUAL_CLASSES_WARNING in directives) != true),
            analysisFlag(AnalysisFlags.dontWarnOnErrorSuppression, trueOrNull(LanguageSettingsDirectives.DONT_WARN_ON_ERROR_SUPPRESSION in directives)),

            analysisFlag(JvmAnalysisFlags.jvmDefaultMode, directives.singleOrZeroValue(LanguageSettingsDirectives.JVM_DEFAULT_MODE)),
            analysisFlag(JvmAnalysisFlags.inheritMultifileParts, trueOrNull(LanguageSettingsDirectives.INHERIT_MULTIFILE_PARTS in directives)),
            analysisFlag(JvmAnalysisFlags.sanitizeParentheses, trueOrNull(LanguageSettingsDirectives.SANITIZE_PARENTHESES in directives)),
            analysisFlag(JvmAnalysisFlags.enableJvmPreview, trueOrNull(LanguageSettingsDirectives.ENABLE_JVM_PREVIEW in directives)),
            analysisFlag(JvmAnalysisFlags.useIR, targetBackend?.isIR != false),

            analysisFlag(AnalysisFlags.explicitApiVersion, trueOrNull(apiVersion != null)),

            analysisFlag(JvmAnalysisFlags.generatePropertyAnnotationsMethods, trueOrNull(LanguageSettingsDirectives.GENERATE_PROPERTY_ANNOTATIONS_METHODS in directives)),
        )

        analysisFlags.forEach { withFlag(it.first, it.second) }

        environmentConfigurators.forEach {
            it.provideAdditionalAnalysisFlags(directives, languageVersion).entries.forEach { (flag, value) ->
                withFlag(flag, value)
            }
        }

        if (targetBackend?.isIR == true) {
            specificFeatures[LanguageFeature.JsAllowValueClassesInExternals] = LanguageFeature.State.ENABLED
        }

        if (targetBackend == TargetBackend.WASM) {
            specificFeatures[LanguageFeature.JsAllowImplementingFunctionInterface] = LanguageFeature.State.ENABLED
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

    @Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "HIDDEN")
    private fun <T : Any> analysisFlag(flag: AnalysisFlag<T>, value: @kotlin.internal.NoInfer T?): Pair<AnalysisFlag<T>, T>? =
        value?.let(flag::to)

    private fun trueOrNull(condition: Boolean): Boolean? = runIf(condition) { true }

    fun build(): LanguageVersionSettings {
        return LanguageVersionSettingsImpl(languageVersion, apiVersion, analysisFlags, specificFeatures)
    }
}

