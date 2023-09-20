/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.test.Directives
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.util.LANGUAGE_FEATURE_PATTERN
import org.junit.Assert
import java.io.File

const val LANGUAGE_DIRECTIVE = "LANGUAGE"
const val API_VERSION_DIRECTIVE = "API_VERSION"

const val OPT_IN_DIRECTIVE = "OPT_IN"
const val IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE = "IGNORE_DATA_FLOW_IN_ASSERT"
const val JVM_DEFAULT_MODE = "JVM_DEFAULT_MODE"
const val SKIP_METADATA_VERSION_CHECK = "SKIP_METADATA_VERSION_CHECK"
const val ALLOW_RESULT_RETURN_TYPE = "ALLOW_RESULT_RETURN_TYPE"
const val INHERIT_MULTIFILE_PARTS = "INHERIT_MULTIFILE_PARTS"
const val SANITIZE_PARENTHESES = "SANITIZE_PARENTHESES"
const val ENABLE_JVM_PREVIEW = "ENABLE_JVM_PREVIEW"

data class CompilerTestLanguageVersionSettings(
    private val initialLanguageFeatures: Map<LanguageFeature, LanguageFeature.State>,
    override val apiVersion: ApiVersion,
    override val languageVersion: LanguageVersion,
    val analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap()
) : LanguageVersionSettings {
    val extraLanguageFeatures = specificFeaturesForTests() + initialLanguageFeatures
    private val delegate = LanguageVersionSettingsImpl(languageVersion, apiVersion, emptyMap(), extraLanguageFeatures)

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
        extraLanguageFeatures[feature] ?: delegate.getFeatureSupport(feature)

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

fun parseLanguageVersionSettingsOrDefault(directiveMap: Directives): CompilerTestLanguageVersionSettings =
    parseLanguageVersionSettings(directiveMap) ?: defaultLanguageVersionSettings()

@JvmOverloads
fun parseLanguageVersionSettings(
    directives: Directives,
    extraLanguageFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
): CompilerTestLanguageVersionSettings? {
    val apiVersionString = directives[API_VERSION_DIRECTIVE]
    val languageFeaturesString = directives[LANGUAGE_DIRECTIVE]

    val analysisFlags = listOfNotNull(
        analysisFlag(AnalysisFlags.optIn, directives[OPT_IN_DIRECTIVE]?.split(' ')),
        analysisFlag(JvmAnalysisFlags.jvmDefaultMode, directives[JVM_DEFAULT_MODE]?.let { JvmDefaultMode.fromStringOrNull(it) }),
        analysisFlag(AnalysisFlags.ignoreDataFlowInAssert, if (IGNORE_DATA_FLOW_IN_ASSERT_DIRECTIVE in directives) true else null),
        analysisFlag(AnalysisFlags.skipMetadataVersionCheck, if (SKIP_METADATA_VERSION_CHECK in directives) true else null),
        analysisFlag(AnalysisFlags.allowResultReturnType, if (ALLOW_RESULT_RETURN_TYPE in directives) true else null),
        analysisFlag(JvmAnalysisFlags.inheritMultifileParts, if (INHERIT_MULTIFILE_PARTS in directives) true else null),
        analysisFlag(JvmAnalysisFlags.sanitizeParentheses, if (SANITIZE_PARENTHESES in directives) true else null),
        analysisFlag(JvmAnalysisFlags.enableJvmPreview, if (ENABLE_JVM_PREVIEW in directives) true else null),
        analysisFlag(AnalysisFlags.explicitApiVersion, if (apiVersionString != null) true else null)
    )

    if (apiVersionString == null && languageFeaturesString == null && analysisFlags.isEmpty()) {
        return null
    }

    val apiVersion = when (apiVersionString) {
        null -> ApiVersion.LATEST_STABLE
        "LATEST" -> ApiVersion.LATEST
        else -> ApiVersion.parse(apiVersionString) ?: error("Unknown API version: $apiVersionString")
    }

    val languageVersion = maxOf(LanguageVersion.LATEST_STABLE, LanguageVersion.fromVersionString(apiVersion.versionString)!!)

    val languageFeatures = languageFeaturesString?.let(::collectLanguageFeatureMap).orEmpty() + extraLanguageFeatures

    return CompilerTestLanguageVersionSettings(languageFeatures, apiVersion, languageVersion, mapOf(*analysisFlags.toTypedArray()))
}

fun defaultLanguageVersionSettings(): CompilerTestLanguageVersionSettings =
    CompilerTestLanguageVersionSettings(emptyMap(), ApiVersion.LATEST_STABLE, LanguageVersion.LATEST_STABLE)

fun languageVersionSettingsFromText(fileTexts: List<String>): LanguageVersionSettings {
    val allDirectives = Directives()
    for (fileText in fileTexts) {
        KotlinTestUtils.parseDirectives(fileText, allDirectives)
    }
    return parseLanguageVersionSettingsOrDefault(allDirectives)
}

fun setupLanguageVersionSettingsForMultifileCompilerTests(files: List<File>, environment: KotlinCoreEnvironment) {
    environment.configuration.languageVersionSettings = languageVersionSettingsFromText(files.map { it.readText() })
}

fun setupLanguageVersionSettingsForCompilerTests(originalFileText: String, environment: KotlinCoreEnvironment) {
    val directives = KotlinTestUtils.parseDirectives(originalFileText)
    val languageVersionSettings = parseLanguageVersionSettingsOrDefault(directives)
    environment.configuration.languageVersionSettings = languageVersionSettings
}

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "HIDDEN")
private fun <T : Any> analysisFlag(flag: AnalysisFlag<T>, value: @kotlin.internal.NoInfer T?): Pair<AnalysisFlag<T>, T>? =
    value?.let(flag::to)

private fun collectLanguageFeatureMap(directives: String): Map<LanguageFeature, LanguageFeature.State> {
    val matcher = LANGUAGE_FEATURE_PATTERN.matcher(directives)
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
