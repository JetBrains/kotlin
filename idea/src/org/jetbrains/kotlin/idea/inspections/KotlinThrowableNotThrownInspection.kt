/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.search.searches.ReferencesSearch
import com.siyeh.ig.psiutils.TestUtils
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypes
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.descriptorUtil.isSubclassOf
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isNothing

class KotlinThrowableNotThrownInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = callExpressionVisitor(fun(callExpression) {
        val calleeExpression = callExpression.calleeExpression ?: return
        if (!calleeExpression.text.let { it.contains("Exception") || it.contains("Error") }) return
        if (TestUtils.isInTestSourceContent(callExpression)) return
        val resultingDescriptor = callExpression.resolveToCall()?.resultingDescriptor ?: return
        val type = resultingDescriptor.returnType ?: return
        if (type.isNothing() || type.isNullable()) return
        val classDescriptor = type.constructor.declarationDescriptor as? ClassDescriptor ?: return
        if (!classDescriptor.isSubclassOf(DefaultBuiltIns.Instance.throwable)) return
        if (callExpression.isUsed()) return

        val description = if (resultingDescriptor is ConstructorDescriptor) {
            "Throwable instance '${calleeExpression.text}' is not thrown"
        } else {
            "Result of '${calleeExpression.text}' call is not thrown"
        }
        holder.registerProblem(calleeExpression, description, ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
    })

    private fun KtExpression.isUsed(): Boolean {
        if (!isUsedAsExpression(analyze(BodyResolveMode.PARTIAL_WITH_CFA))) return false
        val property = getParentOfTypes(
            true,
            KtThrowExpression::class.java,
            KtReturnExpression::class.java,
            KtProperty::class.java
        ) as? KtProperty ?: return true
        return !property.isLocal || ReferencesSearch.search(property).any()
    }
}