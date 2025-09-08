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

    fun parseInternalArgument(arg: String, errors: ArgumentParseErrors): ManualLanguageFeatureSetting? {
        if (!arg.startsWith(wholePrefix)) return null

        val tail = arg.removePrefix(wholePrefix)
        if (tail.getOrNull(0) != ':') {
            return errors.reportAndReturnNull("Incorrect internal argument syntax, missing colon: $arg")
        }
        return parseLanguageFeature(tail.substring(1), arg, errors)
    }

    // Expected tail form: ':(+|-)<language feature name>'
    fun parseLanguageFeature(tail: String, wholeArgument: String, errors: ArgumentParseErrors): ManualLanguageFeatureSetting? {
        val modificator = tail.getOrNull(0)
        val languageFeatureState = when (modificator) {
            '+' -> LanguageFeature.State.ENABLED

            '-' -> LanguageFeature.State.DISABLED

            else -> return errors.reportAndReturnNull("Incorrect internal argument syntax, missing modificator: $wholeArgument")
        }

        val languageFeatureName = tail.substring(1)
        if (languageFeatureName.isEmpty()) return errors.reportAndReturnNull("Empty language feature name for internal argument '$wholeArgument'")

        val languageFeature = LanguageFeature.fromString(languageFeatureName)
            ?: return errors.reportAndReturnNull("Unknown language feature '$languageFeatureName' in passed internal argument '$wholeArgument'")

        if (languageFeature.testOnly && !areTestOnlyLanguageFeaturesAllowed) {
            errors.reportAndReturnNull(
                "Language feature '$languageFeatureName' is test-only and cannot be enabled from command line",
                severity = CompilerMessageSeverity.ERROR
            )
        }

        return ManualLanguageFeatureSetting(languageFeature, languageFeatureState, wholeArgument)
    }

    private fun ArgumentParseErrors.reportAndReturnNull(
        message: String,
        severity: CompilerMessageSeverity = CompilerMessageSeverity.STRONG_WARNING
    ): Nothing? {
        internalArgumentsParsingProblems += severity to message
        return null
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
