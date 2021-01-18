/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiDirectory
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveKotlinDeclarationsHandler
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

class MoveToSealedMatchingPackageFix(element: KtTypeReference) : KotlinQuickFixAction<KtTypeReference>(element) {

    private val moveHandler = MoveKotlinDeclarationsHandler(false)

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val typeReference = element ?: return

        // 'element' references sealed class/interface in extension list
        val classToMove = typeReference.parentOfType<KtClass>() ?: return
        val defaultTargetDir = typeReference.resolveToDir() ?: return

        val parentContext = SimpleDataContext.getProjectContext(project)
        val context = SimpleDataContext.getSimpleContext(LangDataKeys.TARGET_PSI_ELEMENT.name, defaultTargetDir, parentContext)

        moveHandler.tryToMove(classToMove, project, context, null, editor)
    }

    private fun KtTypeReference.resolveToDir(): PsiDirectory? {
        val ktUserType = typeElement as? KtUserType ?: return null
        val ktNameReferenceExpression = ktUserType.referenceExpression as? KtNameReferenceExpression ?: return null
        val declDescriptor = ktNameReferenceExpression.resolveMainReferenceToDescriptors().singleOrNull() ?: return null

        val packageName = declDescriptor.containingPackage()?.asString() ?: return null

        val projectFileIndex = ProjectFileIndex.getInstance(project)
        val ktClassInQuestion = DescriptorToSourceUtils.getSourceFromDescriptor(declDescriptor) as? KtClass ?: return null
        val module = projectFileIndex.getModuleForFile(ktClassInQuestion.containingFile.virtualFile) ?: return null
        val psiPackage =
            KotlinJavaPsiFacade.getInstance(project).findPackage(packageName, GlobalSearchScope.moduleScope(module)) ?: return null

        return psiPackage.directories.find { it.module == module }
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun getText(): String {
        val typeReference = element ?: return ""
        val referencedName = (typeReference.typeElement as? KtUserType)?.referenceExpression?.getReferencedName() ?: return ""

        val classToMove = typeReference.parentOfType<KtClass>() ?: return ""
        return KotlinBundle.message("fix.move.to.sealed.text", classToMove.nameAsSafeName.asString(), referencedName)
    }

    override fun getFamilyName(): String {
        return KotlinBundle.message("fix.move.to.sealed.family")
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): MoveToSealedMatchingPackageFix? {
            val annotationEntry = diagnostic.psiElement as? KtTypeReference ?: return null
            return MoveToSealedMatchingPackageFix(annotationEntry)
        }
    }
}