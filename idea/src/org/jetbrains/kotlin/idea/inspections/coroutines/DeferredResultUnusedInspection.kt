/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.coroutines

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.inspections.AbstractResultUnusedChecker
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import javax.swing.JComponent

class DeferredResultUnusedInspection(@JvmField var standardOnly: Boolean = false) : AbstractResultUnusedChecker(
    expressionChecker = fun(expression, inspection): Boolean =
        inspection is DeferredResultUnusedInspection && expression is KtCallExpression &&
                (!inspection.standardOnly || expression.calleeExpression?.text in shortNames),
    callChecker = fun(resolvedCall, inspection): Boolean {
        if (inspection !is DeferredResultUnusedInspection) return false
        return if (inspection.standardOnly) {
            resolvedCall.resultingDescriptor.fqNameOrNull() in fqNamesAll
        } else {
            val returnTypeClassifier = resolvedCall.resultingDescriptor.returnType?.constructor?.declarationDescriptor
            val importableFqName = returnTypeClassifier?.importableFqName
            importableFqName == deferred || importableFqName == deferredExperimental
        }
    }
) {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        callExpressionVisitor(fun(expression) {
            if (!check(expression)) return
            holder.registerProblem(expression.calleeExpression ?: expression, "Deferred result is never used")
        })

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Reports only function calls from kotlinx.coroutines", "standardOnly")
        return panel
    }

    companion object {
        private val shortNames = setOf("async")

        private val fqNames: Set<FqName> = shortNames.mapTo(mutableSetOf()) { FqName("$COROUTINE_PACKAGE.$it") }

        private val fqNamesExperimental: Set<FqName> = shortNames.mapTo(mutableSetOf()) { FqName("$COROUTINE_EXPERIMENTAL_PACKAGE.$it") }

        private val fqNamesAll = fqNames + fqNamesExperimental

        private val deferred = FqName("$COROUTINE_PACKAGE.Deferred")

        private val deferredExperimental = FqName("$COROUTINE_EXPERIMENTAL_PACKAGE.Deferred")
    }
}