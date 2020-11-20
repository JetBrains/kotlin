/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class MovePropertyToClassBodyIntention : SelfTargetingIntention<KtParameter>(
    KtParameter::class.java,
    KotlinBundle.lazyMessage("move.to.class.body")
) {
    override fun isApplicableTo(element: KtParameter, caretOffset: Int): Boolean {
        if (!element.isPropertyParameter()) return false
        val containingClass = element.containingClass() ?: return false
        return !containingClass.isAnnotation() && !containingClass.isData()
    }

    override fun applyTo(element: KtParameter, editor: Editor?) {
        val parentClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java) ?: return

        val propertyDeclaration = KtPsiFactory(element).createProperty("${element.valOrVarKeyword?.text} ${element.name} = ${element.name}")

        val firstProperty = parentClass.getProperties().firstOrNull()
        parentClass.addDeclarationBefore(propertyDeclaration, firstProperty).apply {
            val propertyModifierList = element.modifierList?.copy() as? KtModifierList
            propertyModifierList?.getModifier(KtTokens.VARARG_KEYWORD)?.delete()
            propertyModifierList?.let { modifierList?.replace(it) ?: addBefore(it, firstChild) }
            modifierList?.annotationEntries?.forEach {
                if (!it.isAppliedToProperty()) {
                    it.delete()
                } else if (it.useSiteTarget?.getAnnotationUseSiteTarget() == AnnotationUseSiteTarget.PROPERTY) {
                    it.useSiteTarget?.removeWithColon()
                }
            }
        }

        element.valOrVarKeyword?.delete()
        val parameterAnnotationsText = element.modifierList?.annotationEntries
            ?.filter { it.isAppliedToConstructorParameter() }
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " ") { it.textWithoutUseSite() }

        val hasVararg = element.hasModifier(KtTokens.VARARG_KEYWORD)
        if (parameterAnnotationsText != null) {
            element.modifierList?.replace(KtPsiFactory(element).createModifierList(parameterAnnotationsText))
        } else {
            element.modifierList?.delete()
        }

        if (hasVararg) element.addModifier(KtTokens.VARARG_KEYWORD)
    }

    private fun KtAnnotationEntry.isAppliedToProperty(): Boolean {
        useSiteTarget?.getAnnotationUseSiteTarget()?.let {
            return it == AnnotationUseSiteTarget.FIELD
                    || it == AnnotationUseSiteTarget.PROPERTY
                    || it == AnnotationUseSiteTarget.PROPERTY_GETTER
                    || it == AnnotationUseSiteTarget.PROPERTY_SETTER
                    || it == AnnotationUseSiteTarget.SETTER_PARAMETER
        }

        return !isApplicableToConstructorParameter()
    }

    private fun KtAnnotationEntry.isAppliedToConstructorParameter(): Boolean {
        useSiteTarget?.getAnnotationUseSiteTarget()?.let {
            return it == AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER
        }

        return isApplicableToConstructorParameter()
    }

    private fun KtAnnotationEntry.isApplicableToConstructorParameter(): Boolean {
        val context = analyze(BodyResolveMode.PARTIAL)
        val descriptor = context[BindingContext.ANNOTATION, this] ?: return false
        val applicableTargets = AnnotationChecker.applicableTargetSet(descriptor)
        return applicableTargets.contains(KotlinTarget.VALUE_PARAMETER)
    }

    private fun KtAnnotationEntry.textWithoutUseSite() = "@" + typeReference?.text.orEmpty() + valueArgumentList?.text.orEmpty()

    private fun KtAnnotationUseSiteTarget.removeWithColon() {
        nextSibling?.delete() // ':' symbol after use site
        delete()
    }
}
