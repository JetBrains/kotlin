/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class UnnecessaryJavaUsageInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {

    private val patterns = mapOf(
            "java.io.PrintStream.println" to Pair("println($0)", ConversionType.METHOD),
            "java.io.PrintStream.print" to Pair("print($0)", ConversionType.METHOD),
            "java.util.Collections.sort" to Pair("$0.sort()", ConversionType.METHOD),
            "java.util.HashMap.put" to Pair("$0[$1] = $2", ConversionType.ASSIGNMENT)
    )

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return object : KtVisitorVoid() {
            override fun visitQualifiedExpression(expression: KtQualifiedExpression) {
                super.visitQualifiedExpression(expression)

                val selectorExpression = expression.selectorExpression
                if(selectorExpression !is KtCallExpression) return
                val args = selectorExpression.valueArguments
                if(args.isEmpty()) return

                val calleeExpression = selectorExpression.calleeExpression as? KtSimpleNameExpression ?: return
                val bindingContext = calleeExpression.analyze(BodyResolveMode.PARTIAL)
                val target = calleeExpression.mainReference.resolveToDescriptors(bindingContext).singleOrNull() ?: return
                val pattern = patterns[target.fqNameSafe.asString()] ?: return
                val javaUsageFix = UnnecessaryJavaUsageFix(pattern.first, pattern.second, expression.receiverExpression, args.map{it.text})
                holder.registerProblem(expression, "Use of Java API that has a Kotlin equivalent",
                                       ProblemHighlightType.WEAK_WARNING, javaUsageFix)
            }
        }
    }

    private class UnnecessaryJavaUsageFix(val pattern: String,
                                          val type: ConversionType,
                                          val receiver: KtExpression,
                                          val arguments: List<String>) : LocalQuickFix {
        override fun getName() = "Unnecessary java usage"
        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            val factory = KtPsiFactory(element)

            val newExpression = when(type) {
                ConversionType.ASSIGNMENT -> factory.createExpressionByPattern(pattern, receiver.text, *arguments.toTypedArray())
                ConversionType.METHOD -> factory.createExpressionByPattern(pattern, *arguments.toTypedArray())
            }

            element.replace(newExpression)
        }
    }

    private enum class ConversionType {
        METHOD, ASSIGNMENT
    }

}
