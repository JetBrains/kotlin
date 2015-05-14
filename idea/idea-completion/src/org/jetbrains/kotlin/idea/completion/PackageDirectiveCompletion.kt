/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PlainPrefixMatcher
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.PlatformPatterns
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.codeInsight.ReferenceVariantsHelper
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPackageDirective

/**
 * Performs completion in package directive. Should suggest only packages and avoid showing fake package produced by
 * DUMMY_IDENTIFIER.
 */
object PackageDirectiveCompletion {
    val DUMMY_IDENTIFIER = "___package___"
    val ACTIVATION_PATTERN = PlatformPatterns.psiElement().inside(javaClass<JetPackageDirective>())

    fun perform(parameters: CompletionParameters, result: CompletionResultSet): Boolean {
        val position = parameters.getPosition()
        if (!ACTIVATION_PATTERN.accepts(position)) return false

        val file = position.getContainingFile() as JetFile

        val ref = file.findReferenceAt(parameters.getOffset()) as? JetSimpleNameReference ?: return false
        val name = ref.expression.getText()!!

        try {
            val prefixLength = parameters.getOffset() - ref.expression.getTextOffset()
            val prefixMatcher = PlainPrefixMatcher(name.substring(0, prefixLength))
            val result = result.withPrefixMatcher(prefixMatcher)

            val resolutionFacade = ref.expression.getResolutionFacade()
            val bindingContext = resolutionFacade.analyze(ref.expression)

            val variants = ReferenceVariantsHelper(bindingContext, { true }).getPackageReferenceVariants(ref.expression, prefixMatcher.asNameFilter())
            for (variant in variants) {
                val lookupElement = LookupElementFactory(resolutionFacade, listOf()).createLookupElement(variant, false)
                if (!lookupElement.getLookupString().contains(DUMMY_IDENTIFIER)) {
                    result.addElement(lookupElement)
                }
            }

            return true
        }
        catch (e: ProcessCanceledException) {
            throw rethrowWithCancelIndicator(e)
        }
    }
}
