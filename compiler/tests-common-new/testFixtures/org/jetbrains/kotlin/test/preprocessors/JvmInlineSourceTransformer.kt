/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.preprocessors

import org.jetbrains.kotlin.config.LanguageFeature
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
 * target backend of the test and the language features of the module:
 * - `@JvmInline` on JVM;
 * - `@kotlin.jvm.JvmInline` when the target backend is [TargetBackend.ANY];
 * - `inline` on other backends when [LanguageFeature.FullValueClasses] is enabled, because there a plain `value class`
 *   is a full value class, not an inline one;
 * - empty string on other backends otherwise, because there a plain `value class` is an inline class.
 *
 * Whatever the backend, the placeholder keeps the class inline, so it must not precede a declaration which already has
 * the `inline` modifier.
 */
class JvmInlineSourceTransformer(testServices: TestServices) : ReversibleSourceFilePreprocessor(testServices) {
    companion object {
        fun computeModifier(targetBackend: TargetBackend, fullValueClassesEnabled: Boolean): ReplacingSourceTransformer {
            return when {
                targetBackend.isTransitivelyCompatibleWith(TargetBackend.JVM) -> TransformersFunctions.replaceOptionalJvmInlineAnnotationWithReal
                targetBackend == TargetBackend.ANY -> TransformersFunctions.replaceOptionalJvmInlineAnnotationWithUniversal
                fullValueClassesEnabled -> TransformersFunctions.replaceOptionalJvmInlineAnnotationWithInlineModifier
                else -> TransformersFunctions.removeOptionalJvmInlineAnnotation
            }
        }
    }

    private val isEnabled: Boolean
        get() = ConfigurationDirectives.WORKS_WHEN_VALUE_CLASS in testServices.moduleStructure.allDirectives

    private fun transformerFor(file: TestFile): ReplacingSourceTransformer {
        val targetBackend = testServices.defaultsProvider.targetBackend ?: TargetBackend.ANY
        val module = testServices.moduleStructure.modules.single { file in it.files }
        return computeModifier(targetBackend, module.languageVersionSettings.supportsFeature(LanguageFeature.FullValueClasses))
    }

    override fun process(file: TestFile, content: String): String {
        if (!isEnabled) return content
        return transformerFor(file).invokeForTestFile(content)
    }

    override fun revert(file: TestFile, actualContent: String): String {
        if (!isEnabled) return actualContent
        return transformerFor(file).revertForFile(actualContent)
    }
}
