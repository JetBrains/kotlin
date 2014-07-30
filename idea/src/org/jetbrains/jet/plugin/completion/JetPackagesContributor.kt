/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.completion

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.plugin.caches.resolve.*
import org.jetbrains.jet.plugin.codeInsight.TipsManager
import org.jetbrains.jet.plugin.references.JetSimpleNameReference

/**
 * Performs completion in package directive. Should suggest only packages and avoid showing fake package produced by
 * DUMMY_IDENTIFIER.
 */
public class JetPackagesContributor : CompletionContributor() {
    class object {
        val DUMMY_IDENTIFIER = "___package___"

        val ACTIVATION_PATTERN: ElementPattern<out PsiElement> = PlatformPatterns.psiElement().inside(javaClass<JetPackageDirective>())
    }

    {
        extend(CompletionType.BASIC, ACTIVATION_PATTERN, object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                val file = parameters.getPosition().getContainingFile()
                if (file !is JetFile) return

                val ref = file.findReferenceAt(parameters.getOffset())

                if (ref is JetSimpleNameReference) {
                    val name = ref.expression.getText() ?: return

                    try {
                        val prefixLength = parameters.getOffset() - ref.expression.getTextOffset()
                        var result = result.withPrefixMatcher(PlainPrefixMatcher(name.substring(0, prefixLength)))

                        val resolveSession = ref.expression.getLazyResolveSession()
                        val bindingContext = resolveSession.resolveToElement(ref.expression)

                        val variants = TipsManager.getPackageReferenceVariants(ref.expression, bindingContext)
                        for (variant in variants) {
                            val lookupElement = DescriptorLookupConverter.createLookupElement(resolveSession, variant)
                            if (!lookupElement.getLookupString().contains(DUMMY_IDENTIFIER)) {
                                result.addElement(lookupElement)
                            }
                        }

                        result.stopHere()
                    }
                    catch (e: ProcessCanceledException) {
                        throw rethrowWithCancelIndicator(e)
                    }
                }
            }
        })
    }
}
