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

package org.jetbrains.kotlin.idea.j2k

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.intentions.IfNullToElvisIntention
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeArgumentsIntention
import org.jetbrains.kotlin.idea.intentions.SimplifyNegatedBinaryExpressionIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToSafeAccessIntention
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveRightPartOfBinaryExpressionFix
import org.jetbrains.kotlin.j2k.PostProcessor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList

public class J2kPostProcessor(private val formatCode: Boolean) : PostProcessor {
    override fun analyzeFile(file: JetFile, range: TextRange?): BindingContext {
        val elements = if (range == null) {
            listOf(file)
        }
        else {
            file.elementsInRange(range).filterIsInstance<JetElement>()
        }
        return file.getResolutionFacade().analyzeFullyAndGetResult(elements).bindingContext
    }

    override fun fixForProblem(problem: Diagnostic): (() -> Unit)? {
        val psiElement = problem.getPsiElement()
        return when (problem.getFactory()) {
            Errors.USELESS_CAST -> { ->
                val expression = RemoveRightPartOfBinaryExpressionFix(psiElement as JetBinaryExpressionWithTypeRHS, "").invoke()

                val variable = expression.getParent() as? JetProperty
                if (variable != null && expression == variable.getInitializer() && variable.isLocal()) {
                    val refs = ReferencesSearch.search(variable, LocalSearchScope(variable.getContainingFile())).findAll()
                    for (ref in refs) {
                        val usage = ref.getElement() as? JetSimpleNameExpression ?: continue
                        usage.replace(expression)
                    }
                    variable.delete()
                }
            }

            Errors.REDUNDANT_PROJECTION -> { ->
                val fix = RemoveModifierFix.createRemoveProjectionFactory(true).createActions(problem).single() as RemoveModifierFix
                fix.invoke()
            }

            else -> super.fixForProblem(problem)
        }
    }

    private enum class RangeFilterResult {
        SKIP
        GO_INSIDE
        PROCESS
    }

    override fun doAdditionalProcessing(file: JetFile, rangeMarker: RangeMarker?) {
        fun rangeFilter(element: PsiElement): RangeFilterResult {
            if (rangeMarker == null) return RangeFilterResult.PROCESS
            if (!rangeMarker.isValid()) return RangeFilterResult.SKIP
            val range = TextRange(rangeMarker.getStartOffset(), rangeMarker.getEndOffset())
            val elementRange = element.getTextRange()
            return when {
                range.contains(elementRange) -> RangeFilterResult.PROCESS
                range.intersects(elementRange) -> RangeFilterResult.GO_INSIDE
                else -> RangeFilterResult.SKIP
            }
        }

        val redundantTypeArgs = ArrayList<JetTypeArgumentList>()
        file.accept(object : JetTreeVisitorVoid(){
            override fun visitElement(element: PsiElement) {
                if (rangeFilter(element) != RangeFilterResult.SKIP) {
                    super.visitElement(element)
                }
            }

            override fun visitTypeArgumentList(typeArgumentList: JetTypeArgumentList) {
                if (rangeFilter(typeArgumentList) == RangeFilterResult.PROCESS && RemoveExplicitTypeArgumentsIntention().isApplicableTo(typeArgumentList)) {
                    redundantTypeArgs.add(typeArgumentList)
                    return
                }

                super.visitTypeArgumentList(typeArgumentList)
            }

            override fun visitPrefixExpression(expression: JetPrefixExpression) {
                super.visitPrefixExpression(expression)

                val intention = SimplifyNegatedBinaryExpressionIntention()
                if (rangeFilter(expression) == RangeFilterResult.PROCESS && intention.isApplicableTo(expression)) {
                    intention.applyTo(expression)
                }
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                super.visitIfExpression(expression)

                if (rangeFilter(expression) == RangeFilterResult.PROCESS) {
                    run {
                        val intention = IfThenToSafeAccessIntention()
                        if (intention.isApplicableTo(expression)) {
                            intention.applyTo(expression)
                            return
                        }
                    }

                    run {
                        val intention = IfThenToElvisIntention()
                        if (intention.isApplicableTo(expression)) {
                            intention.applyTo(expression)
                            return
                        }
                    }

                    run {
                        val intention = IfNullToElvisIntention()
                        if (intention.applicabilityRange(expression) != null) {
                            intention.applyTo(expression)
                            return
                        }
                    }

                }
            }
        })

        for (typeArgs in redundantTypeArgs) {
            typeArgs.delete()
        }

        if (formatCode) {
            val codeStyleManager = CodeStyleManager.getInstance(file.getProject())
            if (rangeMarker != null) {
                if (rangeMarker.isValid()) {
                    codeStyleManager.reformatRange(file, rangeMarker.getStartOffset(), rangeMarker.getEndOffset())
                }
            }
            else {
                codeStyleManager.reformat(file)
            }
        }
    }
}
