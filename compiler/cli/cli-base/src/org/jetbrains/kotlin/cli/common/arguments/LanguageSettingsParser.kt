/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.config.LanguageFeature

// Arguments of form '-XXLanguage:+LanguageFeature' or '-XXLanguage:-LanguageFeature', which enable or disable corresponding LanguageFeature.
object LanguageSettingsParser {

    private val wholePrefix: String = "${INTERNAL_ARGUMENT_PREFIX}Language"

    fun parseInternalArgument(arg: String, diagnostics: MutableList<ArgumentParseDiagnostic>): ManualLanguageFeatureSetting? {
        if (!arg.startsWith(wholePrefix)) return null

        val tail = arg.removePrefix(wholePrefix)
        if (tail.getOrNull(0) != ':') {
            return diagnostics.addAndReturnNull("Incorrect internal argument syntax, missing colon: '$arg'.")
        }
        return parseLanguageFeature(tail.substring(1), arg, diagnostics)
    }

    // Expected tail form: ':(+|-)<language feature name>'
    fun parseLanguageFeature(tail: String, wholeArgument: String, diagnostics: MutableList<ArgumentParseDiagnostic>): ManualLanguageFeatureSetting? {
        val modificator = tail.getOrNull(0)
        val languageFeatureState = when (modificator) {
            '+' -> LanguageFeature.State.ENABLED

            '-' -> LanguageFeature.State.DISABLED

            else -> return diagnostics.addAndReturnNull("Incorrect internal argument syntax, missing '+' or '-' modifier: '$wholeArgument'.")
        }

        val languageFeatureName = tail.substring(1)
        if (languageFeatureName.isEmpty()) return diagnostics.addAndReturnNull("Empty language feature name for internal argument '$wholeArgument'.")

        val languageFeature = LanguageFeature.fromString(languageFeatureName)
            ?: return diagnostics.addAndReturnNull("Unknown language feature '$languageFeatureName' in internal argument '$wholeArgument'.")

        if (languageFeature.testOnly && !areTestOnlyLanguageFeaturesAllowed) {
            diagnostics.addAndReturnNull(
                "Language feature '$languageFeatureName' is test-only and cannot be enabled from the command line.",
                isError = true,
            )
        }

        return ManualLanguageFeatureSetting(languageFeature, languageFeatureState, wholeArgument)
    }

    private fun MutableList<ArgumentParseDiagnostic>.addAndReturnNull(
        message: String,
        isError: Boolean = false,
    ): Nothing? {
        this += if (isError)
            ArgumentParseDiagnostic.InternalArgumentError(message)
        else
            ArgumentParseDiagnostic.InternalArgumentWarning(message)
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
