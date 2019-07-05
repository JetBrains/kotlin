/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix.replaceWith

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.codeInliner.UsageReplacementStrategy
import org.jetbrains.kotlin.idea.codeInliner.replaceUsagesInWholeProject
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.ClassifierNamePolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy

class DeprecatedSymbolUsageInWholeProjectFix(
    element: KtSimpleNameExpression,
    replaceWith: ReplaceWith,
    private val text: String
) : DeprecatedSymbolUsageFixBase(element, replaceWith) {

    override fun getFamilyName() = "Replace deprecated symbol usage in whole project"

    override fun getText() = text

    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false
        return targetPsiElement() != null
    }

    override fun invoke(replacementStrategy: UsageReplacementStrategy, project: Project, editor: Editor?) {
        val psiElement = targetPsiElement()!!
        replacementStrategy.replaceUsagesInWholeProject(psiElement, progressTitle = "Applying '$text'", commandName = text)
    }

    private fun targetPsiElement(): KtDeclaration? = when (val referenceTarget = element?.mainReference?.resolve()) {
        is KtNamedFunction -> referenceTarget
        is KtProperty -> referenceTarget
        is KtTypeAlias -> referenceTarget
        is KtConstructor<*> -> referenceTarget.getContainingClassOrObject() //TODO: constructor can be deprecated itself
        is KtClass -> referenceTarget.takeIf { it.isAnnotation() }
        else -> null
    }

    companion object : KotlinSingleIntentionActionFactory() {
        //TODO: better rendering needed
        private val RENDERER = DescriptorRenderer.withOptions {
            modifiers = emptySet()
            classifierNamePolicy = ClassifierNamePolicy.SHORT
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            receiverAfterName = true
            renderCompanionObjectName = true
            withoutSuperTypes = true
            startFromName = true
            withDefinedIn = false
        }

        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val (nameExpression, replacement, descriptor) = extractDataFromDiagnostic(diagnostic, true) ?: return null
            val descriptorName = RENDERER.render(descriptor)
            return DeprecatedSymbolUsageInWholeProjectFix(
                nameExpression,
                replacement,
                "Replace usages of '$descriptorName' in whole project"
            )
        }
    }
}
