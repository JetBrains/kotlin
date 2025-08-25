/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.LanguageFeature

// Arguments of form '-XXLanguage:+LanguageFeature' or '-XXLanguage:-LanguageFeature', which enable or disable corresponding LanguageFeature.
object LanguageSettingsParser {

    private val wholePrefix: String = "${INTERNAL_ARGUMENT_PREFIX}Language"

    fun canParse(arg: String): Boolean = arg.startsWith(wholePrefix)

    fun parseInternalArgument(arg: String, errors: ArgumentParseErrors): ManualLanguageFeatureSetting? {
        if (!arg.startsWith(wholePrefix)) return null

        return parseTail(arg.removePrefix(wholePrefix), arg, errors)
    }

    // Expected tail form: ':(+|-)<language feature name>'
    fun parseTail(tail: String, wholeArgument: String, errors: ArgumentParseErrors): ManualLanguageFeatureSetting? {
        fun reportAndReturnNull(message: String, severity: CompilerMessageSeverity = CompilerMessageSeverity.STRONG_WARNING): Nothing? {
            errors.internalArgumentsParsingProblems += severity to message
            return null
        }

        if (tail.getOrNull(0) != ':') return reportAndReturnNull("Incorrect internal argument syntax, missing colon: $wholeArgument")

        val modificator = tail.getOrNull(1)
        val languageFeatureState = when (modificator) {
            '+' -> LanguageFeature.State.ENABLED

            '-' -> LanguageFeature.State.DISABLED

            else -> return reportAndReturnNull("Incorrect internal argument syntax, missing modificator: $wholeArgument")
        }

        val languageFeatureName = tail.substring(2)
        if (languageFeatureName.isEmpty()) return reportAndReturnNull("Empty language feature name for internal argument '$wholeArgument'")

        val languageFeature = LanguageFeature.fromString(languageFeatureName)
            ?: return reportAndReturnNull("Unknown language feature '$languageFeatureName' in passed internal argument '$wholeArgument'")

        if (languageFeature.testOnly && !areTestOnlyLanguageFeaturesAllowed) {
            reportAndReturnNull(
                "Language feature '$languageFeatureName' is test-only and cannot be enabled from command line",
                severity = CompilerMessageSeverity.ERROR
            )
        }

        return ManualLanguageFeatureSetting(languageFeature, languageFeatureState, wholeArgument)
    }
}

fun allowTestsOnlyLanguageFeatures() {
    System.setProperty("kotlinc.test.allow.testonly.language.features", "true")
}

private val areTestOnlyLanguageFeaturesAllowed: Boolean by lazy {
    // Use system property because test infra in K/N uses an "isolated" classloader
    System.getProperty("kotlinc.test.allow.testonly.language.features")?.toBoolean() == true
}

data class ManualLanguageFeatureSetting(
    val languageFeature: LanguageFeature,
    val state: LanguageFeature.State,
    val stringRepresentation: String
)
