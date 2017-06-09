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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.isBooleanOrNullableBoolean

class NullableBooleanElvisInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
            object : KtVisitorVoid() {
                override fun visitBinaryExpression(expression: KtBinaryExpression) {
                    if (expression.operationToken != KtTokens.ELVIS) return
                    val lhs = expression.left ?: return
                    val rhs = expression.right ?: return
                    if (rhs !is KtConstantExpression || rhs.node.elementType != KtNodeTypes.BOOLEAN_CONSTANT) return
                    val type = lhs.analyze(BodyResolveMode.PARTIAL).getType(lhs) ?: return
                    if (type.isMarkedNullable && type.isBooleanOrNullableBoolean()) {
                        val parentIf = expression.getParentOfType<KtIfExpression>(false)
                        val partOfCondition = parentIf?.condition?.let { it in expression.parentsWithSelf } ?: false
                        val severity = if (partOfCondition) ProblemHighlightType.WEAK_WARNING else ProblemHighlightType.INFORMATION

                        holder.registerProblem(expression,
                                               "Equality check can be used instead of elvis",
                                               severity,
                                               ReplaceNullableBooleanElvisWithEqualityCheckFix())
                    }
                }
            }
}

private class ReplaceNullableBooleanElvisWithEqualityCheckFix : LocalQuickFix {
    override fun getName() = "Replace elvis with equality check"
    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as? KtBinaryExpression ?: return
        if (!FileModificationService.getInstance().preparePsiElementForWrite(element)) return
        if (element.operationToken != KtTokens.ELVIS) return
        val constPart = element.right as? KtConstantExpression ?: return
        val exprPart = element.left ?: return
        val facade = element.getResolutionFacade()

        val builtIns = facade.moduleDescriptor.builtIns
        val evaluator = ConstantExpressionEvaluator(builtIns, facade.getFrontendService(LanguageVersionSettings::class.java))

        val trace = DelegatingBindingTrace(facade.analyze(constPart, BodyResolveMode.PARTIAL), "Evaluate bool val")

        val constValue = evaluator.evaluateToConstantValue(constPart, trace, builtIns.booleanType)?.value as? Boolean ?: return

        element.replaced(KtPsiFactory(constPart).buildExpression {
            appendExpression(exprPart)
            appendFixedText(if (constValue) " != false" else " == true")
        })
    }
}
