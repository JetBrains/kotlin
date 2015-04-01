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

import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.kotlin.idea.intentions.RemoveExplicitTypeArguments
import org.jetbrains.kotlin.idea.intentions.SimplifyNegatedBinaryExpressionIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToElvisIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfThenToSafeAccessIntention
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveRightPartOfBinaryExpressionFix
import org.jetbrains.kotlin.j2k.PostProcessor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.ArrayList

public class J2kPostProcessor(override val contextToAnalyzeIn: PsiElement) : PostProcessor {
    override fun analyzeFile(file: JetFile): BindingContext {
        return file.analyzeFullyAndGetResult().bindingContext
    }

    override fun fixForProblem(problem: Diagnostic): (() -> Unit)? {
        val psiElement = problem.getPsiElement()
        return when (problem.getFactory()) {
            Errors.USELESS_CAST, Errors.USELESS_CAST_STATIC_ASSERT_IS_FINE -> { ->
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

    override fun doAdditionalProcessing(file: JetFile) {
        val redundantTypeArgs = ArrayList<JetTypeArgumentList>()
        file.accept(object : JetTreeVisitorVoid(){
            override fun visitTypeArgumentList(typeArgumentList: JetTypeArgumentList) {
                if (RemoveExplicitTypeArguments().isApplicableTo(typeArgumentList)) {
                    redundantTypeArgs.add(typeArgumentList)
                    return
                }

                super.visitTypeArgumentList(typeArgumentList)
            }

            override fun visitPrefixExpression(expression: JetPrefixExpression) {
                super.visitPrefixExpression(expression)

                val intention = SimplifyNegatedBinaryExpressionIntention()
                if (intention.isApplicableTo(expression)) {
                    intention.applyTo(expression)
                }
            }

            override fun visitIfExpression(expression: JetIfExpression) {
                super.visitIfExpression(expression)

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
            }
        })

        for (typeArgs in redundantTypeArgs) {
            typeArgs.delete()
        }
    }
}
