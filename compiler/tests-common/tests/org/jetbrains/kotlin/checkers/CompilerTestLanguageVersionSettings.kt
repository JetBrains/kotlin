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
const val INHERIT_MULTIFILE_PARTS = "INHERIT_MULTIFILE_PARTS"
const val SANITIZE_PARENTHESES = "SANITIZE_PARENTHESES"

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

fun parseLanguageVersionSettings(directives: Map<String, String>): CompilerTestLanguageVersionSettings? {
    val apiVersionString = directives[API_VERSION_DIRECTIVE]
    val languageFeaturesString = directives[LANGUAGE_DIRECTIVE]

    val analysisFlags = listOfNotNull(
        analysisFlag(AnalysisFlags.experimental, directives[EXPERIMENTAL_DIRECTIVE]?.split(' ')),
        analysisFlag(AnalysisFlags.useExperimental, directives[USE_EXPERIMENTAL_DIRECTIVE]?.split(' ')),
        analysisFlag(JvmAnalysisFlags.jvmDefaultMode, directives[JVM_DEFAULT_MODE]?.let { JvmDefaultMode.fromStringOrNull(it) }),
        analysisFlag(AnalysisFlags.ignoreDataFlowInAssert, if (IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE in directives) true else null),
        analysisFlag(AnalysisFlags.skipMetadataVersionCheck, if (SKIP_METADATA_VERSION_CHECK in directives) true else null),
        analysisFlag(AnalysisFlags.allowResultReturnType, if (ALLOW_RESULT_RETURN_TYPE in directives) true else null),
        analysisFlag(JvmAnalysisFlags.inheritMultifileParts, if (INHERIT_MULTIFILE_PARTS in directives) true else null),
        analysisFlag(JvmAnalysisFlags.sanitizeParentheses, if (SANITIZE_PARENTHESES in directives) true else null)
    )

    if (apiVersionString == null && languageFeaturesString == null && analysisFlags.isEmpty()) {
        return null
    }

    val apiVersion = (if (apiVersionString != null) ApiVersion.parse(apiVersionString) else ApiVersion.LATEST_STABLE)
        ?: error("Unknown API version: $apiVersionString")

    val languageVersion = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.fromVersionString(apiVersion.versionString)!!)

    val languageFeatures = languageFeaturesString?.let(::collectLanguageFeatureMap).orEmpty()

    return CompilerTestLanguageVersionSettings(languageFeatures, apiVersion, languageVersion, mapOf(*analysisFlags.toTypedArray()))
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

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
private fun <T : Any> analysisFlag(flag: AnalysisFlag<T>, value: @kotlin.internal.NoInfer T?): Pair<AnalysisFlag<T>, T>? =
    value?.let(flag::to)

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
