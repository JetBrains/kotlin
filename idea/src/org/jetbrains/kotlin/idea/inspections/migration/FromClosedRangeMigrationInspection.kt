/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.configuration.MigrationInfo
import org.jetbrains.kotlin.idea.configuration.isLanguageVersionUpdate
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.quickfix.migration.MigrationFix
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.simpleNameExpressionVisitor
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.constants.TypedCompileTimeConstant
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class FromClosedRangeMigrationInspection : AbstractKotlinInspection(), MigrationFix, CleanupLocalInspectionTool {
    override fun isApplicable(migrationInfo: MigrationInfo): Boolean {
        return migrationInfo.isLanguageVersionUpdate(LanguageVersion.KOTLIN_1_2, LanguageVersion.KOTLIN_1_3)
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        simpleNameExpressionVisitor(fun(simpleNameExpression) {
            val callExpression = simpleNameExpression.parent as? KtCallExpression ?: return
            if (simpleNameExpression.text != SHORT_NAME) return

            run {
                val versionAtLeast13 = simpleNameExpression.languageVersionSettings.languageVersion >= LanguageVersion.KOTLIN_1_3
                if (!versionAtLeast13 && !ApplicationManager.getApplication().isUnitTestMode) {
                    return
                }
            }

            val descriptor = simpleNameExpression.resolveMainReferenceToDescriptors().firstOrNull() ?: return
            val callableDescriptor = descriptor as? CallableDescriptor ?: return

            val resolvedToFqName = callableDescriptor.fqNameOrNull()?.asString() ?: return
            if (resolvedToFqName != INT_FROM_CLOSE_RANGE_FQNAME && resolvedToFqName != LONG_FROM_CLOSE_RANGE_FQNAME) {
                return
            }

            val stepParameter = callableDescriptor.valueParameters.getOrNull(2) ?: return
            if (stepParameter.name.asString() != "step") return

            val resolvedCall = callExpression.resolveToCall() ?: return
            val resolvedValueArgument = resolvedCall.valueArguments[stepParameter] as? ExpressionValueArgument ?: return
            val argumentExpression = resolvedValueArgument.valueArgument?.getArgumentExpression() ?: return

            val constant = ConstantExpressionEvaluator.getConstant(argumentExpression, argumentExpression.analyze(BodyResolveMode.PARTIAL))
            if (constant != null) {
                val value = (constant as? TypedCompileTimeConstant<*>)?.constantValue?.value
                if (value != null) {
                    if ((resolvedToFqName == INT_FROM_CLOSE_RANGE_FQNAME && Int.MIN_VALUE == value) ||
                        (resolvedToFqName == LONG_FROM_CLOSE_RANGE_FQNAME && Long.MIN_VALUE == value)
                    ) {
                        report(holder, simpleNameExpression, resolvedToFqName, isOnTheFly, isError = true)
                        return
                    }
                }

                // Don't report for constants other than MIN_VALUE
                return
            }

            report(holder, simpleNameExpression, resolvedToFqName, isOnTheFly, isError = false)
        })

    private fun report(
        holder: ProblemsHolder,
        simpleNameExpression: KtSimpleNameExpression,
        resolvedToFqName: String,
        isOnTheFly: Boolean,
        isError: Boolean
    ) {
        val desc = when (resolvedToFqName) {
            INT_FROM_CLOSE_RANGE_FQNAME -> INT_FROM_CLOSE_RANGE_DESC
            LONG_FROM_CLOSE_RANGE_FQNAME -> LONG_FROM_CLOSE_RANGE_DESC
            else -> throw IllegalArgumentException("Can't process $resolvedToFqName")
        }

        val problemDescriptor = holder.manager.createProblemDescriptor(
            simpleNameExpression,
            simpleNameExpression,
            "It's prohibited to call $desc with MIN_VALUE step since 1.3",
            if (isError) ProblemHighlightType.ERROR else ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            isOnTheFly
        )

        holder.registerProblem(problemDescriptor)
    }

    companion object {
        private const val SHORT_NAME = "fromClosedRange"

        private const val INT_FROM_CLOSE_RANGE_FQNAME = "kotlin.ranges.IntProgression.Companion.fromClosedRange"
        private const val INT_FROM_CLOSE_RANGE_DESC = "IntProgression.fromClosedRange()"

        private const val LONG_FROM_CLOSE_RANGE_FQNAME = "kotlin.ranges.LongProgression.Companion.fromClosedRange"
        private const val LONG_FROM_CLOSE_RANGE_DESC = "LongProgression.fromClosedRange()"
    }
}
