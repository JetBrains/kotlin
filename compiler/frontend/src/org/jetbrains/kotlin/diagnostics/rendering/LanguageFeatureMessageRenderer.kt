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

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings

class LanguageFeatureMessageRenderer @JvmOverloads constructor(
        private val type: Type,
        private val useHtml: Boolean = false
): DiagnosticParameterRenderer<Pair<LanguageFeature, LanguageVersionSettings>> {

    enum class Type {
        UNSUPPORTED,
        WARNING,
        ERROR
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
            Type.ERROR -> sb.append("experimental and disabled")
        }

        val hintUrl = feature.hintUrl
        if (hintUrl != null) {
            if (useHtml) {
                sb.append(" (").append("see more <a href=\"").append(hintUrl).append("\">here</a>)")
            }
            else {
                sb.append(" (see: ").append(hintUrl).append(")")
            }
        }

        return sb.toString()
    }
}