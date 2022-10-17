/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings

class LanguageFeatureMessageRenderer @JvmOverloads constructor(
    private val type: Type,
    private val useHtml: Boolean = false
) : DiagnosticParameterRenderer<Pair<LanguageFeature, LanguageVersionSettings>> {
    private val additionalFeatureMessages = mapOf(
        LanguageFeature.UnitConversionsOnArbitraryExpressions to "You can also change the original type of this expression to (...) -> Unit"
    )

    enum class Type {
        UNSUPPORTED,
        WARNING
    }

    override fun render(obj: Pair<LanguageFeature, LanguageVersionSettings>, renderingContext: RenderingContext): String {
        val (feature, settings) = obj
        val since = feature.sinceVersion

        val sb = StringBuilder()
        sb.append("The feature \"").append(feature.presentableName).append("\" is ")

        when (type) {
            Type.UNSUPPORTED ->
                when {
                    since == null ->
                        sb.append("experimental and should be enabled explicitly")
                    since > settings.languageVersion ->
                        sb.append("only available since language version ").append(since.versionString)
                    feature.sinceApiVersion > settings.apiVersion ->
                        sb.append("only available since API version ").append(feature.sinceApiVersion.versionString)
                    else ->
                        sb.append("disabled")
                }

            Type.WARNING -> sb.append("experimental")
        }

        val hintUrl = feature.hintUrl
        if (hintUrl != null) {
            if (useHtml) {
                sb.append(" (").append("see more <a href=\"").append(hintUrl).append("\">here</a>)")
            } else {
                sb.append(" (see: ").append(hintUrl).append(")")
            }
        }

        if (feature in additionalFeatureMessages) {
            sb.append(". ${additionalFeatureMessages[feature]}")
        }

        return sb.toString()
    }
}
