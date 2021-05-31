/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import org.jetbrains.kotlin.idea.completion.stringTemplates.StringTemplateCompletion

internal object KotlinFirCompletionParametersProvider {
    fun provide(parameters: CompletionParameters): KotlinFirCompletionParameters {
        val (corrected, type) = correctParameters(parameters) ?: return KotlinFirCompletionParameters.Original(parameters)
        return KotlinFirCompletionParameters.Corrected(corrected, parameters, type)
    }

    private fun correctParameters(parameters: CompletionParameters): Pair<CompletionParameters, KotlinFirCompletionParameters.CorrectionType>? {
        val correctParametersForInStringTemplateCompletion =
            StringTemplateCompletion.correctParametersForInStringTemplateCompletion(parameters)
                ?: return null
        return correctParametersForInStringTemplateCompletion to KotlinFirCompletionParameters.CorrectionType.BRACES_FOR_STRING_TEMPLATE
    }
}

internal sealed class KotlinFirCompletionParameters {
    abstract val ijParameters: CompletionParameters
    abstract val type: CorrectionType?

    internal class Original(
        override val ijParameters: CompletionParameters,
    ) : KotlinFirCompletionParameters() {
        override val type: CorrectionType? get() = null
    }

    internal class Corrected(
        override val ijParameters: CompletionParameters,
        val original: CompletionParameters,
        override val type: CorrectionType,
    ) : KotlinFirCompletionParameters()

    enum class CorrectionType {
        BRACES_FOR_STRING_TEMPLATE
    }
}

