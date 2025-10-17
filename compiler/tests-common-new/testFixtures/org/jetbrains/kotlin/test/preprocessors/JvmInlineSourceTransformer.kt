/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.services.ReversibleSourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.defaultsProvider
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.ReplacingSourceTransformer
import org.jetbrains.kotlin.test.utils.TransformersFunctions

/**
 * Replaces the `OPTIONAL_JVM_INLINE_ANNOTATION` placeholder with one of the following options, depending on the
 * target backend of the test:
 * - `@JvmInline`
 * - `@kotlin.jvm.JvmInline`
 * - empty string
 */
class JvmInlineSourceTransformer(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    private var contentModifier: ReplacingSourceTransformer? = null

    companion object {
        fun computeModifier(targetBackend: TargetBackend): ReplacingSourceTransformer {
            return when {
                targetBackend.isTransitivelyCompatibleWith(TargetBackend.JVM) -> TransformersFunctions.replaceOptionalJvmInlineAnnotationWithReal
                targetBackend == TargetBackend.ANY -> TransformersFunctions.replaceOptionalJvmInlineAnnotationWithUniversal
                else -> TransformersFunctions.removeOptionalJvmInlineAnnotation
            }
        }
    }

    override fun process(file: TestFile, content: String): String {
        if (ConfigurationDirectives.WORKS_WHEN_VALUE_CLASS !in testServices.moduleStructure.allDirectives) return content
        val targetBackend = testServices.defaultsProvider.targetBackend ?: TargetBackend.ANY
        val contentModifier = computeModifier(targetBackend).also { this.contentModifier = it }
        return contentModifier.invokeForTestFile(content)
    }

    override fun revert(file: TestFile, actualContent: String): String {
        return contentModifier?.revertForFile(actualContent) ?: actualContent
    }
}
