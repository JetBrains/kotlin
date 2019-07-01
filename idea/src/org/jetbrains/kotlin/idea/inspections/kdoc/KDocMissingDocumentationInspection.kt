/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.kdoc

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElementVisitor
import com.siyeh.ig.psiutils.TestUtils
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.unblockDocument
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.idea.inspections.describe
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.idea.kdoc.KDocElementFactory
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.namedDeclarationVisitor
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.descriptorUtil.isEffectivelyPublicApi

class KDocMissingDocumentationInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        namedDeclarationVisitor { element ->
            if (TestUtils.isInTestSourceContent(element)) {
                return@namedDeclarationVisitor
            }
            val nameIdentifier = element.nameIdentifier
            if (nameIdentifier != null) {
                if (element.findKDoc { DescriptorToSourceUtilsIde.getAnyDeclaration(element.project, it) } == null) {
                    val descriptor =
                        element.resolveToDescriptorIfAny() as? DeclarationDescriptorWithVisibility ?: return@namedDeclarationVisitor
                    if (descriptor.isEffectivelyPublicApi) {
                        val message = element.describe()?.let { "$it is missing documentation" } ?: "Missing documentation"
                        holder.registerProblem(nameIdentifier, message, AddDocumentationFix())
                    }
                }
            }

        }

    override fun runForWholeFile(): Boolean = true

    class AddDocumentationFix : LocalQuickFix {
        override fun getName(): String = "Add documentation"

        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            if (!FileModificationService.getInstance().preparePsiElementForWrite(descriptor.psiElement)) return
            val declaration = descriptor.psiElement.getParentOfType<KtNamedDeclaration>(true)
                    ?: throw IllegalStateException("Can't find declaration")

            declaration.addBefore(KDocElementFactory(project).createKDocFromText("/**\n*\n*/\n"), declaration.firstChild)

            val editor = descriptor.psiElement.findExistingEditor() ?: return


            // If we just add whitespace
            // /**
            //  *[HERE]
            // it will be erased by formatter, so following code adds it right way and moves caret then
            editor.unblockDocument()

            val section = declaration.firstChild.getChildOfType<KDocSection>() ?: return
            val asterisk = section.firstChild

            editor.caretModel.moveToOffset(asterisk.endOffset)
            EditorModificationUtil.insertStringAtCaret(editor, " ")
        }
    }
}
