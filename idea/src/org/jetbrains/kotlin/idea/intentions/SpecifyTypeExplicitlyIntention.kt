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

package org.jetbrains.kotlin.idea.intentions

import com.google.common.collect.Lists
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.template.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.j2k.isConstructor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import java.util.ArrayList

public class SpecifyTypeExplicitlyIntention : JetSelfTargetingIntention<JetCallableDeclaration>(javaClass(), "Specify type explicitly") {
    override fun isApplicableTo(element: JetCallableDeclaration, caretOffset: Int): Boolean {
        if (element.getContainingFile() is JetCodeFragment) return false
        if (element is JetFunctionLiteral) return false // TODO: should JetFunctionLiteral be JetCallableDeclaration at all?
        if (element is JetPrimaryConstructor || element is JetSecondaryConstructor) return false // TODO: base class?
        if (element.getTypeReference() != null) return false

        if (getTypeForDeclaration(element).isError() || hasPublicMemberDiagnostic(element)) return false

        val initializer = (element as? JetWithExpressionInitializer)?.getInitializer()
        if (initializer != null && initializer.getTextRange().containsOffset(caretOffset)) return false

        if (element is JetNamedFunction && element.hasBlockBody()) return false

        setText(if (element is JetFunction) "Specify return type explicitly" else "Specify type explicitly")

        return true
    }

    private fun hasPublicMemberDiagnostic(declaration: JetNamedDeclaration): Boolean {
        return declaration.getContainingJetFile().analyzeFully().getDiagnostics()
                .any { Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE == it.getFactory() && declaration == it.getPsiElement() }
    }

    override fun applyTo(element: JetCallableDeclaration, editor: Editor) {
        val type = getTypeForDeclaration(element)
        addTypeAnnotation(editor, element, type)
    }

    companion object {
        public fun getTypeForDeclaration(declaration: JetCallableDeclaration): JetType {
            val descriptor = declaration.analyze()[BindingContext.DECLARATION_TO_DESCRIPTOR, declaration]
            val type = (descriptor as? CallableDescriptor)?.getReturnType()
            return type ?: ErrorUtils.createErrorType("null type")
        }

        public fun createTypeExpressionForTemplate(exprType: JetType): Expression {
            val descriptor = exprType.getConstructor().getDeclarationDescriptor()
            val isAnonymous = descriptor != null && DescriptorUtils.isAnonymousObject(descriptor)

            val allSupertypes = TypeUtils.getAllSupertypes(exprType)
            val types = if (isAnonymous) ArrayList<JetType>() else arrayListOf(exprType)
            types.addAll(allSupertypes)

            return object : JetTypeLookupExpression<JetType>(types, types.first()) {
                override fun getLookupString(element: JetType) = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(element)
                override fun getResult(element: JetType) = IdeDescriptorRenderers.SOURCE_CODE.renderType(element)
            }
        }

        public fun addTypeAnnotation(editor: Editor?, declaration: JetCallableDeclaration, exprType: JetType) {
            if (editor != null) {
                addTypeAnnotationWithTemplate(editor, declaration, exprType)
            }
            else {
                declaration.setType(KotlinBuiltIns.getInstance().getAnyType())
            }
        }

        public fun createTypeReferencePostprocessor(declaration: JetCallableDeclaration): TemplateEditingAdapter {
            return object : TemplateEditingAdapter() {
                override fun templateFinished(template: Template?, brokenOff: Boolean) {
                    val typeRef = declaration.getTypeReference()
                    if (typeRef != null && typeRef.isValid()) {
                        ShortenReferences.DEFAULT.process(typeRef)
                    }
                }
            }
        }

        private fun addTypeAnnotationWithTemplate(editor: Editor, declaration: JetCallableDeclaration, exprType: JetType) {
            assert(!exprType.isError()) { "Unexpected error type, should have been checked before: " + declaration.getElementTextWithContext() + ", type = " + exprType }

            val project = declaration.getProject()
            val expression = createTypeExpressionForTemplate(exprType)

            declaration.setType(KotlinBuiltIns.getInstance().getAnyType())

            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())

            val newTypeRef = declaration.getTypeReference()!!
            val builder = TemplateBuilderImpl(newTypeRef)
            builder.replaceElement(newTypeRef, expression)

            editor.getCaretModel().moveToOffset(newTypeRef.getNode().getStartOffset())

            TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), createTypeReferencePostprocessor(declaration))
        }
    }
}

