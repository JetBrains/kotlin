/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil
import com.intellij.refactoring.util.RefactoringMessageUtil
import com.intellij.refactoring.util.RefactoringUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.refactoring.createKotlinFile
import org.jetbrains.kotlin.idea.refactoring.move.getTargetPackageFqName
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.*
import org.jetbrains.kotlin.idea.roots.getSuitableDestinationSourceRoots
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

internal class MoveKotlinNestedClassesToUpperLevelModel(
    val project: Project,
    val innerClass: KtClassOrObject,
    val target: PsiElement,
    val parameter: String?,
    val className: String,
    val passOuterClass: Boolean,
    val searchInComments: Boolean,
    val isSearchInNonJavaFiles: Boolean,
    val packageName: String,
    val isOpenInEditor: Boolean
) : Model<MoveKotlinDeclarationsProcessor> {

    private val innerClassDescriptor = innerClass.unsafeResolveToDescriptor(BodyResolveMode.FULL) as ClassDescriptor

    private fun getTargetContainer(): PsiElement? {
        if (target is PsiDirectory) {
            val oldPackageFqName = getTargetPackageFqName(target)
            val targetName = packageName
            if (!Comparing.equal(oldPackageFqName?.asString(), targetName)) {
                val projectRootManager = ProjectRootManager.getInstance(project)

                val contentSourceRoots = getSuitableDestinationSourceRoots(project)
                val newPackage = PackageWrapper(PsiManager.getInstance(project), targetName)

                val targetSourceRoot: VirtualFile
                if (contentSourceRoots.size > 1) {
                    val oldPackage = oldPackageFqName?.let {
                        JavaPsiFacade.getInstance(project).findPackage(it.asString())
                    }

                    var initialDir: PsiDirectory? = null
                    if (oldPackage != null) {
                        val root = projectRootManager.fileIndex.getContentRootForFile(target.virtualFile)
                        initialDir = oldPackage.directories.firstOrNull {
                            Comparing.equal(projectRootManager.fileIndex.getContentRootForFile(it.virtualFile), root)
                        }
                    }

                    targetSourceRoot = MoveClassesOrPackagesUtil.chooseSourceRoot(newPackage, contentSourceRoots, initialDir) ?: return null
                } else {
                    targetSourceRoot = contentSourceRoots[0]
                }

                var directory = RefactoringUtil.findPackageDirectoryInSourceRoot(newPackage, targetSourceRoot);
                if (directory === null) {
                    runWriteAction {
                        try {
                            directory = RefactoringUtil.createPackageDirectoryInSourceRoot(newPackage, targetSourceRoot)
                        } catch (e: IncorrectOperationException) {
                            directory = null;
                        }
                    }
                }
                return directory;
            }

            return target
        }

        return if (target is KtFile || target is KtClassOrObject) target else null

    }

    @Throws(ConfigurationException::class)
    private fun getTargetContainerWithValidation(): PsiElement {

        if (className.isEmpty()) {
            throw ConfigurationException(RefactoringBundle.message("no.class.name.specified"))
        }
        if (!className.isIdentifier()) {
            throw ConfigurationException(RefactoringMessageUtil.getIncorrectIdentifierMessage(className))
        }

        if (passOuterClass) {
            if (parameter.isNullOrEmpty()) {
                throw ConfigurationException(RefactoringBundle.message("no.parameter.name.specified"))
            }
            if (!parameter.isIdentifier()) {
                throw ConfigurationException(RefactoringMessageUtil.getIncorrectIdentifierMessage(parameter))
            }
        }

        val targetContainer = getTargetContainer()

        if (targetContainer is KtClassOrObject) {
            val targetClass = targetContainer as KtClassOrObject?
            for (member in targetClass!!.declarations) {
                if (member is KtClassOrObject && className == member.getName()) {
                    throw ConfigurationException(RefactoringBundle.message("inner.class.exists", className, targetClass.name))
                }
            }
        }

        if (targetContainer is PsiDirectory || targetContainer is KtFile) {
            val targetPackageFqName = getTargetPackageFqName(target)
                ?: throw ConfigurationException("No package corresponds to this directory")

            val existingClass = DescriptorUtils
                .getContainingModule(innerClassDescriptor)
                .getPackage(targetPackageFqName)
                .memberScope
                .getContributedClassifier(Name.identifier(className), NoLookupLocation.FROM_IDE)
            if (existingClass != null) {
                throw ConfigurationException("Class $className already exists in package $targetPackageFqName")
            }

            val targetDir = targetContainer as? PsiDirectory ?: targetContainer.containingFile.containingDirectory
            val message = RefactoringMessageUtil.checkCanCreateFile(targetDir, "$className.kt")
            if (message != null) throw ConfigurationException(message)
        }

        return targetContainer ?: throw ConfigurationException("Invalid target specified")
    }

    @Throws(ConfigurationException::class)
    private fun getMoveTarget(): KotlinMoveTarget {
        val target = getTargetContainerWithValidation()
        if (target is PsiDirectory) {
            val targetDir = target

            val targetPackageFqName = getTargetPackageFqName(target)
                ?: throw ConfigurationException("Cannot find target package name")

            val suggestedName = KotlinNameSuggester.suggestNameByName(className) {
                targetDir.findFile(it + "." + KotlinFileType.EXTENSION) == null
            }

            val targetFileName = suggestedName + "." + KotlinFileType.EXTENSION

            return KotlinMoveTargetForDeferredFile(
                targetPackageFqName,
                targetDir, null
            ) { createKotlinFile(targetFileName, targetDir, targetPackageFqName.asString()) }
        } else {
            return KotlinMoveTargetForExistingElement(target as KtElement)
        }
    }

    @Throws(ConfigurationException::class)
    override fun computeModelResult() = computeModelResult(throwOnConflicts = false)

    @Throws(ConfigurationException::class)
    override fun computeModelResult(throwOnConflicts: Boolean): MoveKotlinDeclarationsProcessor {

        val moveTarget = getMoveTarget()

        val outerInstanceParameterName = if (passOuterClass) packageName else null
        val delegate = MoveDeclarationsDelegate.NestedClass(className, outerInstanceParameterName)
        val moveDescriptor = MoveDeclarationsDescriptor(
            project,
            MoveSource(innerClass),
            moveTarget,
            delegate,
            searchInComments,
            isSearchInNonJavaFiles,
            deleteSourceFiles = false,
            moveCallback = null,
            openInEditor = isOpenInEditor
        )

        return MoveKotlinDeclarationsProcessor(moveDescriptor, Mover.Default, throwOnConflicts)
    }
}
