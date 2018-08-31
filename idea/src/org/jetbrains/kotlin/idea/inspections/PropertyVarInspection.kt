/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.unwrapBlockOrParenthesis
import org.jetbrains.kotlin.idea.quickfix.ChangeVariableMutabilityFix
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.propertyVisitor

class PropertyVarInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        propertyVisitor(fun(property: KtProperty) {
            if (property.isLocal || !property.isVar || property.setter != null) return
            val initializer = property.initializer ?: return
            val getter = property.getter ?: return
            val getterExpression = getter.initializer?.unwrapBlockOrParenthesis()
                ?: (getter.bodyExpression?.unwrapBlockOrParenthesis() as? KtReturnExpression)?.returnedExpression
                ?: return
            if (initializer.text != getterExpression.text) return
            holder.registerProblem(
                property.valOrVarKeyword,
                "This 'var' has no meaning",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                IntentionWrapper(
                    ChangeVariableMutabilityFix(property, makeVar = false, deleteInitializer = true),
                    property.containingFile
                )
            )
        })
}