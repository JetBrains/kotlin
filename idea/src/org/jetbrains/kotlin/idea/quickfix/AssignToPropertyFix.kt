/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.types.KotlinType

class AssignToPropertyFix(element: KtNameReferenceExpression) : KotlinQuickFixAction<KtNameReferenceExpression>(element) {

    override fun getText() = "Assign to property"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(element)
        if (element.getResolutionScope().getImplicitReceiversHierarchy().size == 1) {
            element.replace(psiFactory.createExpressionByPattern("this.$0", element))
        } else {
            element.containingClass()?.name?.let {
                element.replace(psiFactory.createExpressionByPattern("this@$0.$1", it, element))
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        private fun KtCallableDeclaration.hasNameAndTypeOf(name: Name, type: KotlinType) =
            nameAsName == name && (resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType == type

        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtNameReferenceExpression>? {
            val expression = diagnostic.psiElement as? KtNameReferenceExpression ?: return null

            val containingClass = expression.containingClass() ?: return null
            val right = (expression.parent as? KtBinaryExpression)?.right ?: return null
            val type = expression.analyze().getType(right) ?: return null
            val name = expression.getReferencedNameAsName()

            val inSecondaryConstructor = expression.getStrictParentOfType<KtSecondaryConstructor>() != null
            val hasAssignableProperty = containingClass.getProperties().any {
                (inSecondaryConstructor || it.isVar) && it.hasNameAndTypeOf(name, type)
            }
            val hasAssignablePropertyInPrimaryConstructor = containingClass.primaryConstructor?.valueParameters?.any {
                it.valOrVarKeyword?.node?.elementType == KtTokens.VAR_KEYWORD &&
                        it.hasNameAndTypeOf(name, type)
            } ?: false

            if (!hasAssignableProperty && !hasAssignablePropertyInPrimaryConstructor) return null

            return AssignToPropertyFix(expression)
        }
    }

}
