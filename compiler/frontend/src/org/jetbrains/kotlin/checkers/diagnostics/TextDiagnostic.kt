/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers.diagnostics

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory1
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.rendering.AbstractDiagnosticWithParametersRenderer
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticRenderer
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticWithParameters1Renderer
import org.jetbrains.kotlin.diagnostics.rendering.Renderers.TO_STRING
import java.util.regex.Pattern

class TextDiagnostic(
    override val name: String,
    override val platform: String?,
    val parameters: List<String>?,
    inference: InferenceCompatibility?
) : AbstractTestDiagnostic {
    override var inferenceCompatibility = inference ?: InferenceCompatibility.ALL

    val description: String
        get() = (if (platform != null) "$platform:" else "") + name

    enum class InferenceCompatibility constructor(internal var abbreviation: String?) {
        NEW(CheckerTestUtil.NEW_INFERENCE_PREFIX), OLD(CheckerTestUtil.OLD_INFERENCE_PREFIX), ALL(null);

        fun isCompatible(other: InferenceCompatibility): Boolean {
            return this == other || this == ALL || other == ALL
        }
    }

    override fun compareTo(other: AbstractTestDiagnostic): Int {
        return name.compareTo(other.name)
    }

    override fun enhanceInferenceCompatibility(inferenceCompatibility: InferenceCompatibility) {
        this.inferenceCompatibility = inferenceCompatibility
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val that = other as TextDiagnostic?

        if (name != that!!.name) return false
        if (if (platform != null) platform != that.platform else that.platform != null) return false
        if (if (parameters != null) parameters != that.parameters else that.parameters != null) return false
        return inferenceCompatibility == that.inferenceCompatibility

    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (platform?.hashCode() ?: 0)
        result = 31 * result + (parameters?.hashCode() ?: 0)
        result = 31 * result + inferenceCompatibility.hashCode()
        return result
    }

    fun asString(withNewInference: Boolean = true, renderParameters: Boolean = true): String {
        val result = StringBuilder()
        if (withNewInference && inferenceCompatibility.abbreviation != null) {
            result.append(inferenceCompatibility.abbreviation)
            result.append(";")
        }
        if (platform != null) {
            result.append(platform)
            result.append(":")
        }
        result.append(name)

        if (renderParameters && parameters != null) {
            result.append("(")
            result.append(StringUtil.join(parameters, { "\"" + crossPlatformLineBreak.matcher(it).replaceAll(" ") + "\"" }, ", "))
            result.append(")")
        }
        return result.toString()
    }

    override fun toString(): String {
        return asString()
    }

    companion object {
        private val crossPlatformLineBreak = Pattern.compile("""\r?\n""")

        fun parseDiagnostic(text: String): TextDiagnostic {
            val matcher = CheckerTestUtil.individualDiagnosticPattern.matcher(text)
            if (!matcher.find())
                throw IllegalArgumentException("Could not parse diagnostic: $text")

            val inference = computeInferenceCompatibility(
                extractDataBefore(
                    matcher.group(1),
                    ";"
                )
            )
            val platform =
                extractDataBefore(matcher.group(2), ":")

            val name = matcher.group(3)
            val parameters = matcher.group(4) ?: return TextDiagnostic(
                name,
                platform,
                null,
                inference
            )

            return TextDiagnostic(
                name,
                platform,
                parameters.trim('"').split(Regex("""",\s*"""")),
                inference
            )
        }

        private fun computeInferenceCompatibility(abbreviation: String?): InferenceCompatibility {
            return if (abbreviation == null) InferenceCompatibility.ALL else InferenceCompatibility.values().single { inference -> abbreviation == inference.abbreviation }
        }

        private fun extractDataBefore(prefix: String?, anchor: String): String? {
            assert(prefix == null || prefix.endsWith(anchor)) { prefix ?: "" }
            return prefix?.substringBeforeLast(anchor, prefix)
        }

        fun asTextDiagnostic(abstractTestDiagnostic: AbstractTestDiagnostic): TextDiagnostic {
            return if (abstractTestDiagnostic is ActualDiagnostic) {
                asTextDiagnostic(abstractTestDiagnostic)
            } else abstractTestDiagnostic as TextDiagnostic

        }

        private fun asTextDiagnostic(actualDiagnostic: ActualDiagnostic): TextDiagnostic {
            val diagnostic = actualDiagnostic.diagnostic
            val renderer = when (diagnostic.factory) {
                is DebugInfoDiagnosticFactory1 -> {
                    @Suppress("UNCHECKED_CAST")
                    DiagnosticWithParameters1Renderer("{0}", TO_STRING) as DiagnosticRenderer<Diagnostic>
                }
                else -> DefaultErrorMessages.getRendererForDiagnostic(diagnostic)
            }
            val diagnosticName = actualDiagnostic.name

            if (renderer is AbstractDiagnosticWithParametersRenderer) {
                val renderParameters = renderer.renderParameters(diagnostic)
                val parameters = ContainerUtil.map(renderParameters, { it.toString() })
                return TextDiagnostic(
                    diagnosticName,
                    actualDiagnostic.platform,
                    parameters,
                    actualDiagnostic.inferenceCompatibility
                )
            }
            return TextDiagnostic(
                diagnosticName,
                actualDiagnostic.platform,
                null,
                actualDiagnostic.inferenceCompatibility
            )
        }
    }
}