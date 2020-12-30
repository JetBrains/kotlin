/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.annotationEntryVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MigrateDiagnosticSuppressionInspection : AbstractKotlinInspection(), CleanupLocalInspectionTool {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return annotationEntryVisitor(fun(annotationEntry) {
            if (annotationEntry.calleeExpression?.text != "Suppress") return
            val context = annotationEntry.analyze(BodyResolveMode.PARTIAL)
            val descriptor = context[BindingContext.ANNOTATION, annotationEntry] ?: return
            if (descriptor.fqName != StandardNames.FqNames.suppress) return

            for (argument in annotationEntry.valueArguments) {
                val expression = argument.getArgumentExpression() as? KtStringTemplateExpression ?: continue
                val text = expression.text
                if (text.firstOrNull() != '\"' || text.lastOrNull() != '\"') continue
                val newDiagnosticFactory = MIGRATION_MAP[StringUtil.unquoteString(text)] ?: continue

                holder.registerProblem(
                    expression,
                    KotlinBundle.message("diagnostic.name.should.be.replaced.by.the.new.one"),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    ReplaceDiagnosticNameFix(newDiagnosticFactory)
                )
            }
        })
    }

    class ReplaceDiagnosticNameFix(private val diagnosticFactory: DiagnosticFactory<*>) : LocalQuickFix {
        override fun getName() = KotlinBundle.message("replace.diagnostic.name.fix.text", familyName, diagnosticFactory.name!!)

        override fun getFamilyName() = KotlinBundle.message("replace.diagnostic.name.fix.family.name")

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
