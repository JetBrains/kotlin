/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.KotlinTarget
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens.LATEINIT_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.VARARG_KEYWORD
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.AnnotationChecker
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.types.KotlinType


class MovePropertyToConstructorIntention :
        SelfTargetingIntention<KtProperty>(KtProperty::class.java, "Move to constructor"),
        LocalQuickFix {

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val property = descriptor.psiElement as? KtProperty ?: return
        applyTo(property, null)
    }

    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        fun KtProperty.isDeclaredInClass() : Boolean {
            val parent = getStrictParentOfType<KtClassOrObject>()
            return parent is KtClass && !parent.isInterface()
        }

        return !element.isLocal
               && !element.hasDelegate()
               && element.getter == null
               && element.setter == null
               && !element.hasModifier(LATEINIT_KEYWORD)
               && (element.isDeclaredInClass())
               && (element.initializer?.isValidInConstructor() ?: true)
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val parentClass = PsiTreeUtil.getParentOfType(element, KtClass::class.java) ?: return
        val factory = KtPsiFactory(element)
        val primaryConstructor = parentClass.createPrimaryConstructorIfAbsent()
        val constructorParameter = element.findConstructorParameter()

        val commentSaver = CommentSaver(element)

        val propertyAnnotationsText = element.modifierList?.annotationEntries?.joinToString(separator = " ") {
            if (it.isApplicableToConstructorParameter()) {
                it.getTextWithUseSiteIfMissing("property")
            }
            else {
                it.text
            }
        }

        if (constructorParameter != null) {
            val parameterAnnotationsText =
                    constructorParameter.modifierList?.annotationEntries?.joinToString(separator = " ") { it.text }

            val parameterText = buildString {
                element.modifierList?.getModifiersText()?.let(this::append)
                propertyAnnotationsText?.takeIf(String::isNotBlank)?.let { appendWithSpaceBefore(it) }
                parameterAnnotationsText?.let { appendWithSpaceBefore(it) }
                if (constructorParameter.isVarArg) appendWithSpaceBefore(VARARG_KEYWORD.value)
                appendWithSpaceBefore(element.valOrVarKeyword.text)
                element.name?.let { appendWithSpaceBefore(it) }
                constructorParameter.typeReference?.text?.let { append(": $it") }
                constructorParameter.defaultValue?.text?.let { append(" = $it") }
            }

            constructorParameter.replace(factory.createParameter(parameterText)).apply {
                commentSaver.restore(this)
            }
        }
        else {
            val typeText = element.typeReference?.text ?:
                           (element.resolveToDescriptor(BodyResolveMode.PARTIAL) as? PropertyDescriptor)?.type?.render() ?: return
            val parameterText = buildString {
                element.modifierList?.getModifiersText()?.let(this::append)
                propertyAnnotationsText?.takeIf(String::isNotBlank)?.let { appendWithSpaceBefore(it) }
                appendWithSpaceBefore(element.valOrVarKeyword.text)
                element.name?.let { appendWithSpaceBefore(it) }
                appendWithSpaceBefore(": $typeText")
                element.initializer?.text?.let { append(" = $it") }
            }

            primaryConstructor.valueParameterList?.addParameter(factory.createParameter(parameterText))?.apply {
                ShortenReferences.DEFAULT.process(this)
                commentSaver.restore(this)
            }
        }

        element.delete()
    }

    private fun KtProperty.findConstructorParameter(): KtParameter? {
        val reference = initializer as? KtReferenceExpression ?: return null
        val parameterDescriptor = reference.analyze(BodyResolveMode.PARTIAL)[BindingContext.REFERENCE_TARGET, reference]
                                          as? ParameterDescriptor ?: return null
        return parameterDescriptor.source.getPsi() as? KtParameter
    }

    private fun KtAnnotationEntry.isApplicableToConstructorParameter(): Boolean {
        val context = analyze(BodyResolveMode.PARTIAL)
        val descriptor = context[BindingContext.ANNOTATION, this] ?: return false
        val applicableTargets = AnnotationChecker.applicableTargetSet(descriptor)
        return applicableTargets.contains(KotlinTarget.VALUE_PARAMETER)
    }

    private fun KtAnnotationEntry.getTextWithUseSiteIfMissing(useSite: String) =
            if (useSiteTarget == null)
                "@$useSite:${typeReference?.text.orEmpty()}${valueArgumentList?.text.orEmpty()}"
            else
                text

    private fun KotlinType.render() = IdeDescriptorRenderers.SOURCE_CODE.renderType(this)

    private fun KtModifierList.getModifiersText() = getModifiers().joinToString(separator = " ") { it.text }

    private fun KtModifierList.getModifiers(): List<PsiElement> =
            node.getChildren(null).filter { it.elementType is KtModifierKeywordToken }.map { it.psi }

    private fun StringBuilder.appendWithSpaceBefore(str: String) = append(" " + str)

    private fun KtExpression.isValidInConstructor(): Boolean {
        val containingClass = getStrictParentOfType<KtClass>() ?: return false
        var isValid = true
        this.accept(object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                element.acceptChildren(this)
            }

            override fun visitReferenceExpression(expression: KtReferenceExpression) {
                val context = expression.analyze(BodyResolveMode.PARTIAL)
                val declarationDescriptor = expression.getResolvedCall(context)?.resultingDescriptor ?: return
                if (declarationDescriptor.containingDeclaration == containingClass.descriptor) {
                    isValid = false
                }
            }
        })

        return isValid
    }
}