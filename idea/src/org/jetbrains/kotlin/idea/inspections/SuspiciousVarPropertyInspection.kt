/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.ChangeVariableMutabilityFix
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext

class SuspiciousVarPropertyInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        propertyVisitor(fun(property: KtProperty) {
            if (property.isLocal || !property.isVar || property.initializer == null || property.setter != null) return
            val getter = property.getter ?: return
            val context = property.analyze()
            val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? PropertyDescriptor ?: return
            if (context[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == false) return
            if (getter.hasBackingFieldReference()) return

            holder.registerProblem(
                property.valOrVarKeyword,
                "Suspicious 'var' property: its setter does not influence its getter result",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                IntentionWrapper(
                    ChangeVariableMutabilityFix(property, makeVar = false, deleteInitializer = true),
                    property.containingFile
                )
            )
        })

    companion object {
        private fun KtPropertyAccessor.hasBackingFieldReference(): Boolean {
            val bodyExpression = this.bodyExpression ?: return false
            return bodyExpression.isBackingFieldReference(property) || bodyExpression.anyDescendantOfType<KtNameReferenceExpression> {
                it.isBackingFieldReference(property)
            }
        }

        fun KtExpression.isBackingFieldReference(property: KtProperty): Boolean =
            this is KtNameReferenceExpression && isBackingFieldReference(property)

        private fun KtNameReferenceExpression.isBackingFieldReference(property: KtProperty): Boolean {
            return text == KtTokens.FIELD_KEYWORD.value && mainReference.resolve() == property
        }
    }
}