/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveHandler
import org.jetbrains.kotlin.descriptors.containingPackage
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.actions.internal.refactoringTesting.cases.*
import org.jetbrains.kotlin.idea.refactoring.move.getTargetPackageFqName
import org.jetbrains.kotlin.idea.refactoring.move.guessNewFileName
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveKotlinDeclarationsHandler
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveKotlinDeclarationsHandlerActions
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.KotlinAwareMoveFilesOrDirectoriesModel
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.MoveKotlinNestedClassesToUpperLevelModel
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.MoveKotlinTopLevelDeclarationsModel
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade

class MoveToSealedMatchingPackageFix(element: KtTypeReference) : KotlinQuickFixAction<KtTypeReference>(element) {

    private val moveHandler = 
        if (ApplicationManager.getApplication().isUnitTestMode) {
            MoveKotlinDeclarationsHandler(MoveKotlinDeclarationsHandlerTestActions())
        } else {
            MoveKotlinDeclarationsHandler(false)
        }

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

private class MoveKotlinDeclarationsHandlerTestActions : MoveKotlinDeclarationsHandlerActions {

    override fun invokeMoveKotlinTopLevelDeclarationsRefactoring(
        project: Project,
        elementsToMove: Set<KtNamedDeclaration>,
        targetPackageName: String,
        targetDirectory: PsiDirectory?,
        targetFile: KtFile?,
        freezeTargets: Boolean,
        moveToPackage: Boolean,
        moveCallback: MoveCallback?
    ) {
        val sourceFiles = getSourceFiles(elementsToMove)
        val targetFilePath =
            targetFile?.virtualFile?.path ?: sourceFiles[0].virtualFile.parent.path + "/" + guessNewFileName(elementsToMove)

        val model = MoveKotlinTopLevelDeclarationsModel(
            project = project,
            elementsToMove = elementsToMove.toList(),
            targetPackage = targetPackageName,
            selectedPsiDirectory = targetDirectory,
            fileNameInPackage = "Derived.kt",
            targetFilePath = targetFilePath,
            isMoveToPackage = true,
            isSearchReferences = false,
            isSearchInComments = false,
            isSearchInNonJavaFiles = false,
            isDeleteEmptyFiles = false,
            applyMPPDeclarations = false,
            moveCallback = null
        )

        model.computeModelResult(throwOnConflicts = true).processor.run()
    }

    private fun getSourceFiles(elementsToMove: Collection<KtNamedDeclaration>): List<KtFile> {
        return elementsToMove.map { obj: KtPureElement -> obj.containingKtFile }
            .distinct()
    }

    override fun invokeKotlinSelectNestedClassChooser(nestedClass: KtClassOrObject, targetContainer: PsiElement?) =
        doWithMoveKotlinNestedClassesToUpperLevelModel(nestedClass, targetContainer)

    private fun doWithMoveKotlinNestedClassesToUpperLevelModel(nestedClass: KtClassOrObject, targetContainer: PsiElement?) {

        val outerClass = nestedClass.containingClassOrObject ?: throw FailedToRunCaseException()
        val newTarget = targetContainer
            ?: outerClass.containingClassOrObject
            ?: outerClass.containingFile.let { it.containingDirectory ?: it }

        val packageName = getTargetPackageFqName(newTarget)?.asString() ?: ""

        val model = object : MoveKotlinNestedClassesToUpperLevelModel(
            project = nestedClass.project,
            innerClass = nestedClass,
            target = newTarget,
            parameter = "",
            className = nestedClass.name ?: "",
            passOuterClass = false,
            searchInComments = false,
            isSearchInNonJavaFiles = false,
            packageName = packageName,
            isOpenInEditor = false
        ) {
            override fun chooseSourceRoot(
                newPackage: PackageWrapper,
                contentSourceRoots: List<VirtualFile>,
                initialDir: PsiDirectory?
            ) = contentSourceRoots.firstOrNull()
        }

        model.computeModelResult(throwOnConflicts = true).processor.run()
    }

    override fun invokeKotlinAwareMoveFilesOrDirectoriesRefactoring(
        project: Project,
        initialDirectory: PsiDirectory?,
        elements: List<PsiFileSystemItem>,
        moveCallback: MoveCallback?
    ) {
        val targetPath =
            initialDirectory?.virtualFile?.path
                ?: elements.firstOrNull()?.containingFile?.virtualFile?.path
                ?: throw NotImplementedError()

        val model = KotlinAwareMoveFilesOrDirectoriesModel(
            project = project,
            elementsToMove = elements,
            targetDirectoryName = randomDirectoryPathMutator(targetPath),
            updatePackageDirective = randomBoolean(),
            searchReferences = randomBoolean(),
            moveCallback = null
        )

        project.executeCommand(MoveHandler.getRefactoringName()) {
            model.computeModelResult().processor.run()
        }
    }

    override fun showErrorHint(project: Project, editor: Editor?, message: String, title: String, helpId: String?) =
        throw NotImplementedError()

    override fun invokeMoveKotlinNestedClassesRefactoring(
        project: Project,
        elementsToMove: List<KtClassOrObject>,
        originalClass: KtClassOrObject,
        targetClass: KtClassOrObject,
        moveCallback: MoveCallback?
    ) = throw NotImplementedError()
}