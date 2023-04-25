/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.arguments

import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.CALL
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.NO_CALL
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.gradle.dsl.JsMainFunctionExecutionMode
import org.jetbrains.kotlin.gradle.dsl.JsModuleKind
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapNamesPolicy
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion as KotlinVersionDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget as JvmTargetDsl
import kotlin.reflect.KType
import kotlin.reflect.typeOf

open class DefaultValues(
    val defaultValue: String,
    val type: KType,
    val kotlinOptionsType: KType,
    val possibleValues: List<String>? = null,
    val fromKotlinOptionConverterProp: String? = null,
    val toKotlinOptionConverterProp: String? = null,
    val toArgumentConverter: String? = toKotlinOptionConverterProp
) {
    open class DefaultBoolean(defaultValue: Boolean) : DefaultValues(defaultValue.toString(), typeOf<Boolean>(), typeOf<Boolean>())

    object BooleanFalseDefault : DefaultBoolean(false)

    object BooleanTrueDefault : DefaultBoolean(true)

    object StringNullDefault : DefaultValues("null", typeOf<String?>(), typeOf<String?>())

    object EmptyStringListDefault : DefaultValues("emptyList<String>()", typeOf<List<String>>(), typeOf<List<String>>())

    object EmptyStringArrayDefault : DefaultValues(
        "emptyList<String>()",
        typeOf<List<String>>(),
        typeOf<List<String>>(),
        toArgumentConverter = ".toTypedArray()"
    )

    object LanguageVersions : DefaultValues(
        "null",
        typeOf<KotlinVersionDsl?>(),
        typeOf<String?>(),
        possibleValues = LanguageVersion.values()
            .filterNot { it.isUnsupported }
            .map { "\"${it.description}\"" },
        fromKotlinOptionConverterProp = """
        if (this != null) ${typeOf<KotlinVersionDsl>()}.fromVersion(this) else null
        """.trimIndent(),
        toKotlinOptionConverterProp = """
        this?.version
        """.trimIndent()
    )

    object ApiVersions : DefaultValues(
        "null",
        typeOf<KotlinVersionDsl?>(),
        typeOf<String?>(),
        possibleValues = LanguageVersion.values()
            .map(ApiVersion.Companion::createByLanguageVersion)
            .filterNot { it.isUnsupported }
            .map { "\"${it.description}\"" },
        fromKotlinOptionConverterProp = """
        if (this != null) ${typeOf<KotlinVersionDsl>()}.fromVersion(this) else null
        """.trimIndent(),
        toKotlinOptionConverterProp = """
        this?.version
        """.trimIndent()
    )

    object JvmTargetVersions : DefaultValues(
        "org.jetbrains.kotlin.gradle.dsl.JvmTarget.DEFAULT",
        typeOf<JvmTargetDsl>(),
        typeOf<String?>(),
        possibleValues = JvmTarget.supportedValues().map { "\"${it.description}\"" },
        fromKotlinOptionConverterProp = """
        if (this != null) ${typeOf<JvmTargetDsl>()}.fromTarget(this) else null
        """.trimIndent(),
        toKotlinOptionConverterProp = """
        this.target
        """.trimIndent(),
    )

    object JsEcmaVersions : DefaultValues(
        "\"v5\"",
        typeOf<String>(),
        typeOf<String>(),
        possibleValues = listOf("\"v5\"")
    )

    object JsModuleKinds : DefaultValues(
        "${typeOf<JsModuleKind>()}.${JsModuleKind.MODULE_PLAIN.name}",
        typeOf<JsModuleKind>(),
        typeOf<String>(),
        possibleValues = listOf("\"plain\"", "\"amd\"", "\"commonjs\"", "\"umd\""),
        fromKotlinOptionConverterProp = """
        ${typeOf<JsModuleKind>()}.fromKind(this)
        """.trimIndent(),
        toKotlinOptionConverterProp = """
        this.kind
        """.trimIndent()
    )

    object JsSourceMapContentModes : DefaultValues(
        "null",
        typeOf<JsSourceMapEmbedMode?>(),
        typeOf<String?>(),
        possibleValues = listOf(
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING
        ).map { "\"$it\"" },
        fromKotlinOptionConverterProp = """
        this?.let { ${typeOf<JsSourceMapEmbedMode>()}.fromMode(it) }
        """.trimIndent(),
        toKotlinOptionConverterProp = """
        this?.mode
        """.trimIndent()
    )

    object JsSourceMapNamesPolicies : DefaultValues(
        "null",
        typeOf<JsSourceMapNamesPolicy?>(),
        typeOf<String?>(),
        possibleValues = listOf(
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_NO,
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES,
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_FQ_NAMES,
        ).map { "\"$it\"" },
        fromKotlinOptionConverterProp = """
        this?.let { ${typeOf<JsSourceMapNamesPolicy>()}.fromPolicy(it) }
        """.trimIndent(),
        toKotlinOptionConverterProp = """
        this?.policy
        """.trimIndent()
    )

    object JsMain : DefaultValues(
        "${typeOf<JsMainFunctionExecutionMode>()}.${JsMainFunctionExecutionMode.CALL.name}",
        typeOf<JsMainFunctionExecutionMode>(),
        typeOf<String>(),
        possibleValues = listOf("\"" + CALL + "\"", "\"" + NO_CALL + "\""),
        fromKotlinOptionConverterProp = """
        ${typeOf<JsMainFunctionExecutionMode>()}.fromMode(this)
        """.trimIndent(),
        toKotlinOptionConverterProp = """
        this.mode
        """.trimIndent()
    )
}
