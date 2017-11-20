/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError

open class RemovePartsFromPropertyFix(
        element: KtProperty,
        private val removeInitializer: Boolean,
        private val removeGetter: Boolean,
        private val removeSetter: Boolean
) : KotlinQuickFixAction<KtProperty>(element) {

    private constructor(element: KtProperty) : this(
            element,
            element.hasInitializer(),
            element.getter?.bodyExpression != null,
            element.setter?.bodyExpression != null
    )

    override fun getText(): String =
            "Remove ${partsToRemove(removeGetter, removeSetter, removeInitializer)} from property"

    override fun getFamilyName(): String = "Remove parts from property"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        val type = QuickFixUtil.getDeclarationReturnType(element) ?: return false
        return !type.isError
    }

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val newElement = element?.copy() as? KtProperty ?: return

        val getter = newElement.getter
        if (removeGetter && getter != null) {
            newElement.deleteChildInternal(getter.node)
        }

        val setter = newElement.setter
        if (removeSetter && setter != null) {
            newElement.deleteChildInternal(setter.node)
        }

        val initializer = newElement.initializer
        var typeToAdd: KotlinType? = null
        if (removeInitializer && initializer != null) {
            val nextSibling = newElement.nameIdentifier?.nextSibling
            if (nextSibling != null) {
                newElement.deleteChildRange(nextSibling, initializer)
                val type = QuickFixUtil.getDeclarationReturnType(element)
                if (newElement.typeReference == null && type != null) {
                    typeToAdd = type
                }
            }
        }
        val replaceElement = element?.replace(newElement) as? KtProperty
        if (replaceElement != null && typeToAdd != null) {
            SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, replaceElement, typeToAdd)
        }
    }

    private fun partsToRemove(getter: Boolean, setter: Boolean, initializer: Boolean): String = buildString {
        if (getter) {
            append("getter")
            if (setter && initializer)
                append(", ")
            else if (setter || initializer)
                append(" and ")
        }
        if (setter) {
            append("setter")
            if (initializer) append(" and ")
        }
        if (initializer) append("initializer")
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtProperty>? {
            val element = diagnostic.psiElement
            val property = PsiTreeUtil.getParentOfType(element, KtProperty::class.java) ?: return null
            return RemovePartsFromPropertyFix(property)
        }
    }

    object LateInitFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtProperty>? {
            val element = Errors.INAPPLICABLE_LATEINIT_MODIFIER.cast(diagnostic).psiElement
            val property = PsiTreeUtil.getParentOfType(element, KtProperty::class.java) ?: return null
            val hasInitializer = property.hasInitializer()
            val hasGetter = property.getter?.bodyExpression != null
            val hasSetter = property.setter?.bodyExpression != null
            if (!hasInitializer && !hasGetter && !hasSetter) return null
            return RemovePartsFromPropertyFix(property, hasInitializer, hasGetter, hasSetter)
        }
    }

}
