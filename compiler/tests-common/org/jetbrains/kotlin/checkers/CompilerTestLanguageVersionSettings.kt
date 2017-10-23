/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.config.*
import org.junit.Assert
import java.util.*
import java.util.regex.Pattern

const val LANGUAGE_DIRECTIVE = "LANGUAGE"
const val API_VERSION_DIRECTIVE = "API_VERSION"

data class CompilerTestLanguageVersionSettings(
        private val languageFeatures: Map<LanguageFeature, LanguageFeature.State>,
        override val apiVersion: ApiVersion,
        override val languageVersion: LanguageVersion
) : LanguageVersionSettings {
    private val delegate = LanguageVersionSettingsImpl(languageVersion, apiVersion)

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State =
            languageFeatures[feature] ?: delegate.getFeatureSupport(feature)

    override fun <T> getFlag(flag: AnalysisFlag<T>): T = flag.defaultValue
}

fun parseLanguageVersionSettings(directiveMap: Map<String, String>): LanguageVersionSettings? {
    val apiVersionString = directiveMap[API_VERSION_DIRECTIVE]
    val directives = directiveMap[LANGUAGE_DIRECTIVE]
    if (apiVersionString == null && directives == null) return null

    val apiVersion = (if (apiVersionString != null) ApiVersion.parse(apiVersionString) else ApiVersion.LATEST_STABLE)
                     ?: error("Unknown API version: $apiVersionString")

    val languageFeatures = directives?.let(::collectLanguageFeatureMap).orEmpty()

    return CompilerTestLanguageVersionSettings(languageFeatures, apiVersion, LanguageVersion.LATEST_STABLE)
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
