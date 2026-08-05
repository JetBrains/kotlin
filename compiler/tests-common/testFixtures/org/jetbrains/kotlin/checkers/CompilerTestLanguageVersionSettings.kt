/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.test.checkTestInfrastructure
import org.jetbrains.kotlin.test.testInfraError
import org.jetbrains.kotlin.test.util.LANGUAGE_FEATURE_PATTERN

const val LANGUAGE_DIRECTIVE = "LANGUAGE"

fun collectLanguageFeatureMap(directives: String): Map<LanguageFeature, LanguageFeature.State> {
    val matcher = LANGUAGE_FEATURE_PATTERN.matcher(directives)
    checkTestInfrastructure(matcher.find()) {
        "Wrong syntax in the '// $LANGUAGE_DIRECTIVE: ...' directive:\n" +
                "found: '$directives'\n" +
                "Must be '((+|-)LanguageFeatureName)+'\n" +
                "where '+' means 'enable', '-' means 'disable', 'warn:' means 'enable with warning'\n" +
                "and language feature names are names of enum entries in LanguageFeature enum class"
    }

    val values = HashMap<LanguageFeature, LanguageFeature.State>()
    do {
        val mode = when (matcher.group(1)) {
            "+" -> LanguageFeature.State.ENABLED
            "-" -> LanguageFeature.State.DISABLED
            else -> testInfraError("Unknown mode for language feature: ${matcher.group(1)}")
        }
        val name = matcher.group(2)
        val feature = LanguageFeature.fromString(name) ?: testInfraError(
                "Language feature not found, please check spelling: $name\n" +
                "Known features:\n    ${LanguageFeature.entries.joinToString("\n    ")}"
        )
        if (values.put(feature, mode) != null) {
            testInfraError("Duplicate entry for the language feature: $name")
        }
    }
    while (matcher.find())

    return values
}
