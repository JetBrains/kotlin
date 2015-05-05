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
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import java.util.ArrayList

public class SpecifyTypeExplicitlyAction : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String {
        return JetBundle.message("specify.type.explicitly.action.family.name")
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        var element = element
        val typeRefParent = PsiTreeUtil.getTopmostParentOfType<JetTypeReference>(element, javaClass<JetTypeReference>())
        if (typeRefParent != null) {
            element = typeRefParent
        }
        val declaration = element.getParent() as JetCallableDeclaration
        val type = getTypeForDeclaration(declaration)
        if (declaration.getTypeReference() == null) {
            addTypeAnnotation(project, editor, declaration, type)
        }
        else {
            declaration.setTypeReference(null)
        }
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        var element = element
        if (element.getContainingFile() is JetCodeFragment) {
            return false
        }

        val typeRefParent = PsiTreeUtil.getTopmostParentOfType<JetTypeReference>(element, javaClass<JetTypeReference>())
        if (typeRefParent != null) {
            element = typeRefParent
        }
        val parent = element.getParent()
        if (parent !is JetCallableDeclaration) return false

        if (parent is JetProperty && !PsiTreeUtil.isAncestor(parent.getInitializer(), element, false)) {
            if (parent.getTypeReference() != null) {
                setText(JetBundle.message("specify.type.explicitly.remove.action.name"))
                return true
            }
            else {
                setText(JetBundle.message("specify.type.explicitly.add.action.name"))
            }
        }
        else if (parent is JetNamedFunction && parent.getTypeReference() == null && !parent.hasBlockBody()) {
            setText(JetBundle.message("specify.type.explicitly.add.return.type.action.name"))
        }
        else if (parent is JetParameter && parent.isLoopParameter()) {
            if (parent.getTypeReference() != null) {
                setText(JetBundle.message("specify.type.explicitly.remove.action.name"))
                return true
            }
            else {
                setText(JetBundle.message("specify.type.explicitly.add.action.name"))
            }
        }
        else {
            return false
        }

        if (getTypeForDeclaration(parent).isError()) {
            return false
        }
        return !hasPublicMemberDiagnostic(parent)
    }

    companion object {


        private fun hasPublicMemberDiagnostic(declaration: JetNamedDeclaration): Boolean {
            val bindingContext = declaration.getContainingJetFile().analyzeFully()
            for (diagnostic in bindingContext.getDiagnostics()) {
                //noinspection ConstantConditions
                if (Errors.PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE == diagnostic.getFactory() && declaration == diagnostic.getPsiElement()) {
                    return true
                }
            }
            return false
        }

        public fun getTypeForDeclaration(declaration: JetCallableDeclaration): JetType {
            val bindingContext = declaration.getContainingJetFile().analyzeFully()

            val type = if (bindingContext.get<PsiElement, DeclarationDescriptor>(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration) != null) bindingContext.get<PsiElement, DeclarationDescriptor>(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration).getReturnType() else null
            return type ?: ErrorUtils.createErrorType("null type")
        }

        public fun createTypeExpressionForTemplate(exprType: JetType): Expression {
            val descriptor = exprType.getConstructor().getDeclarationDescriptor()
            val isAnonymous = descriptor != null && DescriptorUtils.isAnonymousObject(descriptor)

            val allSupertypes = TypeUtils.getAllSupertypes(exprType)
            val types = if (isAnonymous) ArrayList<JetType>() else Lists.newArrayList<JetType>(exprType)
            types.addAll(allSupertypes)

            return object : JetTypeLookupExpression<JetType>(types, types.iterator().next(), JetBundle.message("specify.type.explicitly.add.action.name")) {
                override fun getLookupString(element: JetType): String {
                    return IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(element)
                }

                override fun getResult(element: JetType): String {
                    return IdeDescriptorRenderers.SOURCE_CODE.renderType(element)
                }
            }
        }

        public fun addTypeAnnotation(project: Project, editor: Editor?, declaration: JetCallableDeclaration, exprType: JetType) {
            if (editor != null) {
                addTypeAnnotationWithTemplate(project, editor, declaration, exprType)
            }
            else {
                declaration.setTypeReference(anyTypeRef(project))
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

        private fun addTypeAnnotationWithTemplate(project: Project, editor: Editor, declaration: JetCallableDeclaration, exprType: JetType) {
            assert(!exprType.isError()) { "Unexpected error type, should have been checked before: " + declaration.getElementTextWithContext() + ", type = " + exprType }

            val expression = createTypeExpressionForTemplate(exprType)

            declaration.setTypeReference(anyTypeRef(project))

            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument())

            val newTypeRef = declaration.getTypeReference()
            assert(newTypeRef != null)
            val builder = TemplateBuilderImpl(newTypeRef)
            builder.replaceElement(newTypeRef, expression)

            editor.getCaretModel().moveToOffset(newTypeRef!!.getNode().getStartOffset())

            TemplateManager.getInstance(project).startTemplate(editor, builder.buildInlineTemplate(), createTypeReferencePostprocessor(declaration))
        }

        private fun anyTypeRef(project: Project): JetTypeReference {
            return JetPsiFactory(project).createType("Any")
        }
    }
}
