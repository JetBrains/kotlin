/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.arguments

import com.intellij.openapi.util.text.StringUtil.compareVersionNumbers
import org.jetbrains.kotlin.config.LanguageFeature

object CliArgumentStringBuilder {
    private const val LANGUAGE_FEATURE_FLAG_PREFIX = "-XXLanguage:"
    private const val LANGUAGE_FEATURE_DEDICATED_FLAG_PREFIX = "-X"

    private val versionRegex = Regex("""^(\d+)\.(\d+)\.(\d+)""")

    private val LanguageFeature.dedicatedFlagInfo
        get() = when (this) {
            LanguageFeature.InlineClasses -> Pair("inline-classes", KotlinVersion(1, 3, 50))
            else -> null
        }

    private val LanguageFeature.State.sign: String
        get() = when (this) {
            LanguageFeature.State.ENABLED -> "+"
            LanguageFeature.State.DISABLED -> "-"
            LanguageFeature.State.ENABLED_WITH_WARNING -> "+" // not supported normally
            LanguageFeature.State.ENABLED_WITH_ERROR -> "-" // not supported normally
        }

    private fun LanguageFeature.getFeatureMentionInCompilerArgsRegex(): Regex {
        val basePattern = "$LANGUAGE_FEATURE_FLAG_PREFIX(?:-|\\+)$name"
        val fullPattern =
            if (dedicatedFlagInfo != null) "(?:$basePattern)|$LANGUAGE_FEATURE_DEDICATED_FLAG_PREFIX${dedicatedFlagInfo!!.first}" else basePattern

        return Regex(fullPattern)
    }

    fun LanguageFeature.buildArgumentString(state: LanguageFeature.State, kotlinVersion: String?): String {
        val shouldBeFeatureEnabled = state == LanguageFeature.State.ENABLED || state == LanguageFeature.State.ENABLED_WITH_WARNING
        val dedicatedFlag = dedicatedFlagInfo?.run {
            val (xFlag, xFlagSinceVersion) = this

            if (kotlinVersion == null) return@run xFlag

            val parsedVersion = versionRegex.find(kotlinVersion) ?: return@run xFlag
            val isAtLeastSpecifiedVersion = parsedVersion.destructured.let { (major, minor, patch) ->
                KotlinVersion(major.toInt(), minor.toInt(), patch.toInt()) >= xFlagSinceVersion
            }
            if (isAtLeastSpecifiedVersion) xFlag else null
        }

        return if (shouldBeFeatureEnabled && dedicatedFlag != null) {
            LANGUAGE_FEATURE_DEDICATED_FLAG_PREFIX + dedicatedFlag
        } else {
            "$LANGUAGE_FEATURE_FLAG_PREFIX${state.sign}$name"
        }
    }

    fun String.replaceLanguageFeature(
        feature: LanguageFeature,
        state: LanguageFeature.State,
        kotlinVersion: String?,
        prefix: String = "",
        postfix: String = "",
        separator: String = ", ",
        quoted: Boolean = true
    ): String {
        val quote = if (quoted) "\"" else ""
        val featureArgumentString = feature.buildArgumentString(state, kotlinVersion)
        val existingFeatureMatchResult = feature.getFeatureMentionInCompilerArgsRegex().find(this)

        return if (existingFeatureMatchResult != null) {
            replace(existingFeatureMatchResult.value, featureArgumentString)
        } else {
            val splitText = if (postfix.isNotEmpty()) split(postfix) else listOf(this, "")
            if (splitText.size != 2) {
                "$prefix$quote$featureArgumentString$quote$postfix"
            } else {
                val (mainPart, commentPart) = splitText
                // In Groovy / Kotlin DSL, we can have comment after [...] or listOf(...)
                mainPart + "$separator$quote$featureArgumentString$quote$postfix" + commentPart
            }
        }
    }
}