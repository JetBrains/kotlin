/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.*
import org.jetbrains.kotlin.idea.inspections.RemoveRedundantSetterFix
import org.jetbrains.kotlin.idea.inspections.isRedundantSetter
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.search.usagesSearch.descriptor
import org.jetbrains.kotlin.idea.util.runOnExpectAndAllActuals
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.resolve.ExposedVisibilityChecker

open class ChangeVisibilityFix(
    element: KtModifierListOwner,
    protected val elementName: String,
    protected val visibilityModifier: KtModifierKeywordToken,
    private val addImplicitVisibilityModifier: Boolean = false
) : KotlinQuickFixAction<KtModifierListOwner>(element) {

    override fun getText() = KotlinBundle.message("make.0.1", elementName, visibilityModifier)
    override fun getFamilyName() = KotlinBundle.message("make.0", visibilityModifier)

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val pointer = element?.createSmartPointer()
        val originalElement = pointer?.element
        if (originalElement is KtDeclaration) {
            originalElement.runOnExpectAndAllActuals(useOnSelf = true) {
                it.setVisibility(visibilityModifier, addImplicitVisibilityModifier)
            }
        } else {
            originalElement?.setVisibility(visibilityModifier, addImplicitVisibilityModifier)
        }

        val propertyAccessor = pointer?.element as? KtPropertyAccessor
        if (propertyAccessor?.isRedundantSetter() == true) {
            RemoveRedundantSetterFix.removeRedundantSetter(propertyAccessor)
        }
    }

    protected class ChangeToPublicFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.PUBLIC_KEYWORD), HighPriorityAction {

        override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
            val element = element ?: return false
            return element.canBePublic()
        }
    }

    protected class ChangeToProtectedFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.PROTECTED_KEYWORD) {

        override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
            val element = element ?: return false
            return element.canBeProtected()
        }
    }

    protected class ChangeToInternalFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.INTERNAL_KEYWORD) {

        override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
            val element = element ?: return false
            return element.canBeInternal()
        }
    }

    protected class ChangeToPrivateFix(element: KtModifierListOwner, elementName: String) :
        ChangeVisibilityFix(element, elementName, KtTokens.PRIVATE_KEYWORD), HighPriorityAction {

        override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
            val element = element ?: return false
            return element.canBePrivate()
        }
    }

    protected class ChangeToPublicExplicitlyFix(element: KtModifierListOwner, elementName: String) : ChangeVisibilityFix(
        element,
        elementName,
        KtTokens.PUBLIC_KEYWORD,
        addImplicitVisibilityModifier = true
    ), HighPriorityAction {
        override fun getText() = KotlinBundle.message("make.0.1.explicitly", elementName, visibilityModifier)
        override fun getFamilyName() = KotlinBundle.message("make.0.explicitly", visibilityModifier)
    }

    companion object {
        fun create(
            declaration: KtModifierListOwner,
            descriptor: DeclarationDescriptorWithVisibility,
            targetVisibility: DescriptorVisibility
        ): IntentionAction? {
            if (!ExposedVisibilityChecker(declaration.languageVersionSettings).checkDeclarationWithVisibility(
                    declaration, descriptor, targetVisibility
                )
            ) return null

            val name = descriptor.name.asString()

            return when (targetVisibility) {
                DescriptorVisibilities.PRIVATE -> ChangeToPrivateFix(declaration, name)
                DescriptorVisibilities.INTERNAL -> ChangeToInternalFix(declaration, name)
                DescriptorVisibilities.PROTECTED -> ChangeToProtectedFix(declaration, name)
                DescriptorVisibilities.PUBLIC -> ChangeToPublicFix(declaration, name)
                else -> null
            }
        }
    }

    object SetExplicitVisibilityFactory : KotlinIntentionActionsFactory() {
        @Suppress("UNCHECKED_CAST")
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val factory = diagnostic.factory as DiagnosticFactory0<KtDeclaration>
            val descriptor = factory.cast(diagnostic).psiElement.descriptor as? DeclarationDescriptorWithVisibility ?: return emptyList()
            val element = diagnostic.psiElement as? KtModifierListOwner ?: return emptyList()
            return listOf(
                ChangeToPublicExplicitlyFix(
                    element,
                    descriptor.name.asString()
                )
            )
        }
    }
}
