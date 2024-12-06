/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class AnnotationDefaultTargetMode(val description: String) {
    FIRST_ONLY("first-only"),
    FIRST_ONLY_WARN("first-only-warn"),
    PARAM_PROPERTY("param-property");

    companion object {
        @JvmStatic
        fun fromStringOrNull(string: String?) = AnnotationDefaultTargetMode.entries.find { it.description == string }

        @JvmStatic
        fun fromString(string: String?, languageVersionSettings: LanguageVersionSettings) =
            fromStringOrNull(string) ?: when {
                languageVersionSettings.supportsFeature(LanguageFeature.PropertyParamAnnotationDefaultTargetMode) -> PARAM_PROPERTY
                languageVersionSettings.supportsFeature(LanguageFeature.AnnotationDefaultTargetMigrationWarning) -> FIRST_ONLY_WARN
                else -> FIRST_ONLY
            }
    }
}
