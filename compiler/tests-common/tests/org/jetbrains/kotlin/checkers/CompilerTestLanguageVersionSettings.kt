/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.util.*
import java.util.regex.Pattern

const val LANGUAGE_DIRECTIVE = "LANGUAGE"
const val API_VERSION_DIRECTIVE = "API_VERSION"

const val EXPERIMENTAL_DIRECTIVE = "EXPERIMENTAL"
const val USE_EXPERIMENTAL_DIRECTIVE = "USE_EXPERIMENTAL"
const val IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE = "IGNORE_DATA_FLOW_IN_ASSERT"
const val JVM_DEFAULT_MODE = "JVM_DEFAULT_MODE"

data class CompilerTestLanguageVersionSettings(
        private val initialLanguageFeatures: Map<LanguageFeature, LanguageFeature.State>,
        override val apiVersion: ApiVersion,
        override val languageVersion: LanguageVersion,
        private val analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap()
) : LanguageVersionSettings {
    private val languageFeatures = initialLanguageFeatures + specificFeaturesForTests()
    private val delegate = LanguageVersionSettingsImpl(languageVersion, apiVersion)

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
            languageFeatures[feature] ?: delegate.getFeatureSupport(feature)

    override fun isPreRelease(): Boolean = languageVersion.isPreRelease()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue
}

private fun specificFeaturesForTests(): Map<LanguageFeature, LanguageFeature.State> {
    return if (System.getProperty("kotlin.ni") == "true")
        mapOf(LanguageFeature.NewInference to LanguageFeature.State.ENABLED)
    else
        emptyMap()
}

fun parseLanguageVersionSettings(directiveMap: Map<String, String>): LanguageVersionSettings? {
    val apiVersionString = directiveMap[API_VERSION_DIRECTIVE]
    val languageFeaturesString = directiveMap[LANGUAGE_DIRECTIVE]
    val experimental = directiveMap[EXPERIMENTAL_DIRECTIVE]?.split(' ')?.let { AnalysisFlag.experimental to it }
    val useExperimental = directiveMap[USE_EXPERIMENTAL_DIRECTIVE]?.split(' ')?.let { AnalysisFlag.useExperimental to it }
    val ignoreDataFlowInAssert = AnalysisFlag.ignoreDataFlowInAssert to directiveMap.containsKey(IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE)
    val enableJvmDefault = directiveMap[JVM_DEFAULT_MODE]?.let { AnalysisFlag.jvmDefaultMode to JvmDefaultMode.fromStringOrNull(it)!! }

    if (apiVersionString == null && languageFeaturesString == null && experimental == null && useExperimental == null && !ignoreDataFlowInAssert.second) return null

    val apiVersion = (if (apiVersionString != null) ApiVersion.parse(apiVersionString) else ApiVersion.LATEST_STABLE)
                     ?: error("Unknown API version: $apiVersionString")

    val languageVersion = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.fromVersionString(apiVersion.versionString)!!)

    val languageFeatures = languageFeaturesString?.let(::collectLanguageFeatureMap).orEmpty()

    return CompilerTestLanguageVersionSettings(
        languageFeatures, apiVersion, languageVersion,
        mapOf(*listOfNotNull(experimental, useExperimental, enableJvmDefault, ignoreDataFlowInAssert).toTypedArray())
    )
}

fun setupLanguageVersionSettingsForCompilerTests(originalFileText: String, environment: KotlinCoreEnvironment) {
    val directives = KotlinTestUtils.parseDirectives(originalFileText)
    val languageVersionSettings = parseLanguageVersionSettings(directives) ?:
                                  CompilerTestLanguageVersionSettings(emptyMap(), ApiVersion.LATEST_STABLE, LanguageVersion.LATEST_STABLE)
    environment.configuration.languageVersionSettings = languageVersionSettings
}

private val languagePattern = Pattern.compile("(\\+|\\-|warn:)(\\w+)\\s*")

private fun collectLanguageFeatureMap(directives: String): Map<LanguageFeature, LanguageFeature.State> {
    val matcher = languagePattern.matcher(directives)
    if (!matcher.find()) {
        Assert.fail(
                "Wrong syntax in the '// !$LANGUAGE_DIRECTIVE: ...' directive:\n" +
                "found: '$directives'\n" +
                "Must be '((+|-|warn:)LanguageFeatureName)+'\n" +
                "where '+' means 'enable', '-' means 'disable', 'warn:' means 'enable with warning'\n" +
                "and language feature names are names of enum entries in LanguageFeature enum class"
        )
    }

    val values = HashMap<LanguageFeature, LanguageFeature.State>()
    do {
        val mode = when (matcher.group(1)) {
            "+" -> LanguageFeature.State.ENABLED
            "-" -> LanguageFeature.State.DISABLED
            "warn:" -> LanguageFeature.State.ENABLED_WITH_WARNING
            else -> error("Unknown mode for language feature: ${matcher.group(1)}")
        }
        val name = matcher.group(2)
        val feature = LanguageFeature.fromString(name) ?: throw AssertionError(
                "Language feature not found, please check spelling: $name\n" +
                "Known features:\n    ${LanguageFeature.values().joinToString("\n    ")}"
        )
        if (values.put(feature, mode) != null) {
            Assert.fail("Duplicate entry for the language feature: $name")
        }
    }
    while (matcher.find())

    return values
}
