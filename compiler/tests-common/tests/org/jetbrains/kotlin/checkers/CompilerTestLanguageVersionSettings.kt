/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File
import java.util.regex.Pattern

const val LANGUAGE_DIRECTIVE = "LANGUAGE"
const val API_VERSION_DIRECTIVE = "API_VERSION"

const val EXPERIMENTAL_DIRECTIVE = "EXPERIMENTAL"
const val USE_EXPERIMENTAL_DIRECTIVE = "USE_EXPERIMENTAL"
const val IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE = "IGNORE_DATA_FLOW_IN_ASSERT"
const val JVM_DEFAULT_MODE = "JVM_DEFAULT_MODE"
const val SKIP_METADATA_VERSION_CHECK = "SKIP_METADATA_VERSION_CHECK"
const val ALLOW_RESULT_RETURN_TYPE = "ALLOW_RESULT_RETURN_TYPE"

data class CompilerTestLanguageVersionSettings(
        private val initialLanguageFeatures: Map<LanguageFeature, LanguageFeature.State>,
        override val apiVersion: ApiVersion,
        override val languageVersion: LanguageVersion,
        private val analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap()
) : LanguageVersionSettings {
    private val languageFeatures = specificFeaturesForTests() + initialLanguageFeatures
    private val delegate = LanguageVersionSettingsImpl(languageVersion, apiVersion, emptyMap(), languageFeatures)

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
            languageFeatures[feature] ?: delegate.getFeatureSupport(feature)

    override fun isPreRelease(): Boolean = KotlinCompilerVersion.isPreRelease()

    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue
}

private fun specificFeaturesForTests(): Map<LanguageFeature, LanguageFeature.State> {
    return if (System.getProperty("kotlin.ni") == "true")
        mapOf(LanguageFeature.NewInference to LanguageFeature.State.ENABLED)
    else
        emptyMap()
}

fun parseLanguageVersionSettingsOrDefault(directiveMap: Map<String, String>): CompilerTestLanguageVersionSettings =
    parseLanguageVersionSettings(directiveMap) ?: defaultLanguageVersionSettings()

fun parseLanguageVersionSettings(directiveMap: Map<String, String>): CompilerTestLanguageVersionSettings? {
    val apiVersionString = directiveMap[API_VERSION_DIRECTIVE]
    val languageFeaturesString = directiveMap[LANGUAGE_DIRECTIVE]
    val experimental = directiveMap[EXPERIMENTAL_DIRECTIVE]?.split(' ')?.let { AnalysisFlags.experimental to it }
    val useExperimental = directiveMap[USE_EXPERIMENTAL_DIRECTIVE]?.split(' ')?.let { AnalysisFlags.useExperimental to it }
    val ignoreDataFlowInAssert = AnalysisFlags.ignoreDataFlowInAssert to directiveMap.containsKey(IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE)
    val enableJvmDefault = directiveMap[JVM_DEFAULT_MODE]?.let { JvmAnalysisFlags.jvmDefaultMode to JvmDefaultMode.fromStringOrNull(it)!! }
    val skipMetadataVersionCheck = AnalysisFlags.skipMetadataVersionCheck to directiveMap.containsKey(SKIP_METADATA_VERSION_CHECK)
    val allowResultReturnType = AnalysisFlags.allowResultReturnType to directiveMap.containsKey(ALLOW_RESULT_RETURN_TYPE)

    if (apiVersionString == null && languageFeaturesString == null && experimental == null &&
        useExperimental == null && !ignoreDataFlowInAssert.second && !allowResultReturnType.second
    ) {
        return null
    }

    val apiVersion = (if (apiVersionString != null) ApiVersion.parse(apiVersionString) else ApiVersion.LATEST_STABLE)
                     ?: error("Unknown API version: $apiVersionString")

    val languageVersion = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.fromVersionString(apiVersion.versionString)!!)

    val languageFeatures = languageFeaturesString?.let(::collectLanguageFeatureMap).orEmpty()

    return CompilerTestLanguageVersionSettings(
        languageFeatures, apiVersion, languageVersion,
        mapOf(
            *listOfNotNull(
                experimental, useExperimental, enableJvmDefault, ignoreDataFlowInAssert, skipMetadataVersionCheck, allowResultReturnType
            ).toTypedArray()
        )
    )
}

fun defaultLanguageVersionSettings(): CompilerTestLanguageVersionSettings =
    CompilerTestLanguageVersionSettings(emptyMap(), ApiVersion.LATEST_STABLE, LanguageVersion.LATEST_STABLE)

fun setupLanguageVersionSettingsForMultifileCompilerTests(files: List<File>, environment: KotlinCoreEnvironment) {
    val allDirectives = HashMap<String, String>()
    for (file in files) {
        allDirectives.putAll(KotlinTestUtils.parseDirectives(file.readText()))
    }
    environment.configuration.languageVersionSettings = parseLanguageVersionSettingsOrDefault(allDirectives)
}

fun setupLanguageVersionSettingsForCompilerTests(originalFileText: String, environment: KotlinCoreEnvironment) {
    val directives = KotlinTestUtils.parseDirectives(originalFileText)
    val languageVersionSettings = parseLanguageVersionSettingsOrDefault(directives)
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
