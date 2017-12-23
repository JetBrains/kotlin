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

package org.jetbrains.kotlin.idea.i18n.inspections

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.i18n.JavaI18nUtil
import com.intellij.lang.properties.ResourceBundleReference
import com.intellij.lang.properties.psi.Property
import com.intellij.lang.properties.references.PropertyReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class KotlinInvalidBundleOrPropertyInspection : AbstractKotlinInspection() {
    override fun getDisplayName() = CodeInsightBundle.message("inspection.unresolved.property.key.reference.name")

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {
            private fun processResourceBundleReference(ref: ResourceBundleReference, template: KtStringTemplateExpression) {
                if (ref.resolve() == null) {
                    holder.registerProblem(
                            template,
                            CodeInsightBundle.message("inspection.invalid.resource.bundle.reference", ref.canonicalText),
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                            TextRange(0, template.textLength)
                    )
                }
            }

            private fun processPropertyReference(ref: PropertyReference, template: KtStringTemplateExpression) {
                val property = ref.resolve() as? Property
                if (property == null) {
                    holder.registerProblem(
                            template,
                            CodeInsightBundle.message("inspection.unresolved.property.key.reference.message", ref.canonicalText),
                            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL,
                            TextRange(0, template.textLength),
                            *ref.quickFixes
                    )
                    return
                }

                val argument = template.parents.firstIsInstanceOrNull<KtValueArgument>() ?: return
                if (argument.getArgumentExpression() != KtPsiUtil.deparenthesize(template) ) return

                val callExpression = argument.getStrictParentOfType<KtCallExpression>() ?: return
                val resolvedCall = callExpression.getResolvedCall(callExpression.analyze(BodyResolveMode.PARTIAL)) ?: return

                val resolvedArguments = resolvedCall.valueArgumentsByIndex ?: return
                val keyArgumentIndex = resolvedArguments.indexOfFirst { it is ExpressionValueArgument && it.valueArgument == argument }
                if (keyArgumentIndex < 0) return

                val callable = resolvedCall.resultingDescriptor
                if (callable.valueParameters.size != keyArgumentIndex + 2) return

                val messageArgument = resolvedArguments[keyArgumentIndex + 1] as? VarargValueArgument ?: return
                if (messageArgument.arguments.singleOrNull()?.getSpreadElement() != null) return

                val expectedArgumentCount = JavaI18nUtil.getPropertyValuePlaceholdersCount(property.value ?: "")
                val actualArgumentCount = messageArgument.arguments.size
                if (actualArgumentCount < expectedArgumentCount) {
                    val description = CodeInsightBundle.message(
                            "property.has.more.parameters.than.passed",
                            ref.canonicalText, expectedArgumentCount, actualArgumentCount
                    )
                    holder.registerProblem(template, description, ProblemHighlightType.GENERIC_ERROR)
                }
            }

            override fun visitStringTemplateExpression(expression: KtStringTemplateExpression) {
                for (ref in expression.references) {
                    when (ref) {
                        is ResourceBundleReference -> processResourceBundleReference(ref, expression)
                        is PropertyReference -> processPropertyReference(ref, expression)
                    }
                }
            }
        }
    }
}