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
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext

class PropertyVarInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        propertyVisitor(fun(property: KtProperty) {
            if (property.isLocal || !property.isVar || property.initializer == null || property.setter != null) return
            val getter = property.getter ?: return
            val descriptor = property.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? PropertyDescriptor ?: return
            if (property.analyze()[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == false) return
            if (getter.hasBackingFieldReference()) return

            holder.registerProblem(
                property.valOrVarKeyword,
                "Property setter does not influence its getter result",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                IntentionWrapper(
                    ChangeVariableMutabilityFix(property, makeVar = false, deleteInitializer = true),
                    property.containingFile
                )
            )
        })

    private fun KtPropertyAccessor.hasBackingFieldReference(): Boolean {
        val bodyExpression = this.bodyExpression ?: return false
        val p = this.property
        return bodyExpression.isBackingField(p) || bodyExpression.anyDescendantOfType<KtNameReferenceExpression> { it.isBackingField(p) }
    }

    private fun KtExpression.isBackingField(property: KtProperty): Boolean {
        val ref = this as? KtNameReferenceExpression ?: return false
        return ref.text == "field" && ref.mainReference.resolve() == property
    }
}