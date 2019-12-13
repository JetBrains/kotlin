/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToParameterDescriptorIfAny
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class RenameParameterToMatchOverriddenMethodFix(
    parameter: KtParameter,
    private val newName: String
) : KotlinQuickFixAction<KtParameter>(parameter) {
    override fun getFamilyName() = "Rename"

    override fun getText() = "Rename parameter to match overridden method"

    override fun startInWriteAction(): Boolean = false

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        RenameProcessor(project, element ?: return, newName, false, false).run()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val parameter = diagnostic.psiElement.getNonStrictParentOfType<KtParameter>() ?: return null
            val parameterDescriptor = parameter.resolveToParameterDescriptorIfAny(BodyResolveMode.FULL) ?: return null
            val parameterFromSuperclassName = parameterDescriptor.overriddenDescriptors
                .map { it.name.asString() }
                .distinct()
                .singleOrNull() ?: return null
            return RenameParameterToMatchOverriddenMethodFix(parameter, parameterFromSuperclassName)
        }
    }
}
