/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.refinement.TypeRefinement

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

    override fun getText(): String = KotlinBundle.message(
        "remove.0.from.property",
        partsToRemove(removeGetter, removeSetter, removeInitializer)
    )

    override fun getFamilyName(): String = KotlinBundle.message("remove.parts.from.property")

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
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
            // `refineType` call here is needed to avoid InvalidModuleException exception
            //
            // It happens because after refinement KotlinTypes may access ModuleDescriptor content
            // and that module becomes invalid after replacement two lines above
            //
            // The actual problem is that we use a type obtained from the obsolete analysis session
            // The ideal fix would be using a String that needs to be rendered instead of actual type
            //
            // But calling another type refinement also helps because it makes KotlinType instance using new module descriptor
            @OptIn(TypeRefinement::class)
            typeToAdd = replaceElement.getResolutionFacade().frontendService<KotlinTypeRefiner>().refineType(typeToAdd)

            SpecifyTypeExplicitlyIntention.addTypeAnnotation(editor, replaceElement, typeToAdd)
        }
    }

    private fun partsToRemove(getter: Boolean, setter: Boolean, initializer: Boolean): String = buildString {
        if (getter) {
            append(KotlinBundle.message("text.getter"))
            if (setter && initializer)
                append(", ")
            else if (setter || initializer)
                append(" ${KotlinBundle.message("configuration.text.and")} ")
        }
        if (setter) {
            append(KotlinBundle.message("text.setter"))
            if (initializer) append(" ${KotlinBundle.message("configuration.text.and")} ")
        }
        if (initializer) append(KotlinBundle.message("text.initializer"))
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
