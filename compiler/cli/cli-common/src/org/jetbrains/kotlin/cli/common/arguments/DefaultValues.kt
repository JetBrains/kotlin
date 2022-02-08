/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.CALL
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.NO_CALL
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion

open class DefaultValues(val defaultValue: String, val possibleValues: List<String>? = null) {
    object BooleanFalseDefault : DefaultValues("false")

    object BooleanTrueDefault : DefaultValues("true")

    object StringNullDefault : DefaultValues("null")

    object ListEmptyDefault : DefaultValues("<empty list>")

    object LanguageVersions : DefaultValues(
        "null",
        LanguageVersion.values()
            .filterNot { it.isUnsupported }
            .map { "\"${it.description}\"" }
    )

    object ApiVersions : DefaultValues(
        "null",
        LanguageVersion.values()
            .map(ApiVersion.Companion::createByLanguageVersion)
            .filterNot { it.isUnsupported }
            .map { "\"${it.description}\"" }
    )

    object JvmTargetVersions : DefaultValues(
        "null",
        JvmTarget.values().map { "\"${it.description}\"" }
    )

    object JsEcmaVersions : DefaultValues(
        "\"v5\"",
        listOf("\"v5\"")
    )

    object JsModuleKinds : DefaultValues(
        "\"plain\"",
        listOf("\"plain\"", "\"amd\"", "\"commonjs\"", "\"umd\"")
    )

    object JsSourceMapContentModes : DefaultValues(
        "null",
        listOf(
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING
        ).map { "\"$it\"" }
    )

    object JsMain : DefaultValues(
        "\"" + CALL + "\"",
        listOf("\"" + CALL + "\"", "\"" + NO_CALL + "\"")
    )
}
