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
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MigrateDiagnosticSuppressionInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry) {
                super.visitAnnotationEntry(annotationEntry)

                if (annotationEntry.calleeExpression?.text != "Suppress") return
                val context = annotationEntry.analyze(BodyResolveMode.PARTIAL)
                val descriptor = context[BindingContext.ANNOTATION, annotationEntry] ?: return
                if (descriptor.fqName != KotlinBuiltIns.FQ_NAMES.suppress) return

                for (argument in annotationEntry.valueArguments) {
                    val expression = argument.getArgumentExpression() as? KtStringTemplateExpression ?: continue
                    val text = expression.text
                    if (text.firstOrNull() != '\"' || text.lastOrNull() != '\"') continue
                    val newDiagnosticFactory = MIGRATION_MAP[StringUtil.unquoteString(text)] ?: continue

                    holder.registerProblem(
                            expression,
                            "Diagnostic name should be replaced by the new one",
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                            ReplaceDiagnosticNameFix(newDiagnosticFactory)
                    )
                }
            }
        }
    }

    class ReplaceDiagnosticNameFix(private val diagnosticFactory: DiagnosticFactory<*>) : LocalQuickFix {
        override fun getName() = "$familyName with ${diagnosticFactory.name}"

        override fun getFamilyName() = "Replace diagnostic name"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement as? KtStringTemplateExpression ?: return
            if (!FileModificationService.getInstance().preparePsiElementForWrite(expression)) return

            val psiFactory = KtPsiFactory(expression)
            expression.replace(psiFactory.createExpression("\"${diagnosticFactory.name}\""))
        }

    }

    companion object {

        private val MIGRATION_MAP = mapOf(
                "HEADER_DECLARATION_WITH_BODY" to EXPECTED_DECLARATION_WITH_BODY,
                "HEADER_DECLARATION_WITH_DEFAULT_PARAMETER" to EXPECTED_DECLARATION_WITH_DEFAULT_PARAMETER,
                "HEADER_CLASS_CONSTRUCTOR_DELEGATION_CALL" to EXPECTED_CLASS_CONSTRUCTOR_DELEGATION_CALL,
                "HEADER_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER" to EXPECTED_CLASS_CONSTRUCTOR_PROPERTY_PARAMETER,
                "HEADER_ENUM_CONSTRUCTOR" to EXPECTED_ENUM_CONSTRUCTOR,
                "HEADER_ENUM_ENTRY_WITH_BODY" to EXPECTED_ENUM_ENTRY_WITH_BODY,
                "HEADER_PROPERTY_INITIALIZER" to EXPECTED_PROPERTY_INITIALIZER,

                "IMPL_TYPE_ALIAS_NOT_TO_CLASS" to ACTUAL_TYPE_ALIAS_NOT_TO_CLASS,
                "IMPL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE" to ACTUAL_TYPE_ALIAS_TO_CLASS_WITH_DECLARATION_SITE_VARIANCE,
                "IMPL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE" to ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE,
                "IMPL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION" to ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION,

                "HEADER_WITHOUT_IMPLEMENTATION" to NO_ACTUAL_FOR_EXPECT,
                "IMPLEMENTATION_WITHOUT_HEADER" to ACTUAL_WITHOUT_EXPECT,

                "HEADER_CLASS_MEMBERS_ARE_NOT_IMPLEMENTED" to NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS,
                "IMPL_MISSING" to ACTUAL_MISSING
        )
    }
}
