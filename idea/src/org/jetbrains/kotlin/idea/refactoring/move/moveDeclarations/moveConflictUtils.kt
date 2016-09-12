/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.*
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightMethods
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.MutablePackageFragmentDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.refactoring.getUsageContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contains
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isInsideOf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.utils.SmartSet
import java.util.*

class MoveConflictChecker(
        private val project: Project,
        private val elementsToMove: List<KtNamedDeclaration>,
        private val moveTarget: KotlinMoveTarget
) {
    private val resolutionFacade by lazy { KotlinCacheService.getInstance(project).getResolutionFacade(elementsToMove) }

    private val fakeFile = KtPsiFactory(project).createFile("")

    private fun PackageFragmentDescriptor.withSource(sourceFile: KtFile): PackageFragmentDescriptor {
        return object : PackageFragmentDescriptor by this {
            override fun getOriginal() = this
            override fun getSource() = KotlinSourceElement(sourceFile)
        }
    }

    private fun KotlinMoveTarget.getContainerDescriptor(): DeclarationDescriptor? {
        return when (this) {
            is KotlinMoveTargetForExistingElement -> {
                val targetElement = targetElement
                when (targetElement) {
                    is KtNamedDeclaration -> resolutionFacade.resolveToDescriptor(targetElement)

                    is KtFile -> {
                        val packageFragment = resolutionFacade.analyze(targetElement)[BindingContext.FILE_TO_PACKAGE_FRAGMENT, targetElement]
                        packageFragment?.withSource(targetElement)
                    }

                    else -> null
                }
            }

            is KotlinDirectoryBasedMoveTarget -> {
                val packageFqName = targetContainerFqName ?: return null
                val targetDir = directory
                val targetModuleDescriptor = if (targetDir != null) {
                    val targetModule = ModuleUtilCore.findModuleForPsiElement(targetDir) ?: return null
                    val moduleFileIndex = ModuleRootManager.getInstance(targetModule).fileIndex
                    val targetModuleInfo = when {
                        moduleFileIndex.isInSourceContent(targetDir.virtualFile) -> targetModule.productionSourceInfo()
                        moduleFileIndex.isInTestSourceContent(targetDir.virtualFile) -> targetModule.testSourceInfo()
                        else -> return null
                    }
                    resolutionFacade.findModuleDescriptor(targetModuleInfo) ?: return null
                }
                else {
                    resolutionFacade.moduleDescriptor
                }
                MutablePackageFragmentDescriptor(targetModuleDescriptor, packageFqName).withSource(fakeFile)
            }

            else -> null
        }
    }

    private fun DeclarationDescriptor.isVisibleIn(where: DeclarationDescriptor): Boolean {
        return when {
            this !is DeclarationDescriptorWithVisibility -> true
            !Visibilities.isVisibleIgnoringReceiver(this, where) -> false
            this is ConstructorDescriptor -> Visibilities.isVisibleIgnoringReceiver(containingDeclaration, where)
            else -> true
        }
    }

    private fun DeclarationDescriptor.asPredicted(newContainer: DeclarationDescriptor): DeclarationDescriptor? {
        val originalVisibility = (this as? DeclarationDescriptorWithVisibility)?.visibility ?: return null
        val visibility = if (originalVisibility == Visibilities.PROTECTED && newContainer is PackageFragmentDescriptor) {
            Visibilities.PUBLIC
        } else {
            originalVisibility
        }
        return when (this) {
        // We rely on visibility not depending on more specific type of CallableMemberDescriptor
            is CallableMemberDescriptor -> object : CallableMemberDescriptor by this {
                override fun getOriginal() = this
                override fun getContainingDeclaration() = newContainer
                override fun getVisibility(): Visibility = visibility
                override fun getSource() = SourceElement { DescriptorUtils.getContainingSourceFile(newContainer) }
            }
            is ClassDescriptor -> object: ClassDescriptor by this {
                override fun getOriginal() = this
                override fun getContainingDeclaration() = newContainer
                override fun getVisibility(): Visibility = visibility
                override fun getSource() = SourceElement { DescriptorUtils.getContainingSourceFile(newContainer) }
            }
            else -> null
        }
    }

    private fun render(declaration: PsiElement) = RefactoringUIUtil.getDescription(declaration, false)

    fun checkModuleConflictsInUsages(usages: List<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {
        val sourceRoot = moveTarget.targetFile ?: return
        RefactoringConflictsUtil.analyzeModuleConflicts(project, elementsToMove, usages.toTypedArray(), sourceRoot, conflicts)
    }

    fun checkModuleConflictsInDeclarations(conflicts: MultiMap<PsiElement, String>) {
        val sourceRoot = moveTarget.targetFile ?: return
        val targetModule = ModuleUtilCore.findModuleForFile(sourceRoot, project) ?: return
        val resolveScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(targetModule)
        for (declaration in elementsToMove) {
            declaration.forEachDescendantOfType<KtReferenceExpression> { refExpr ->
                refExpr.references
                        .forEach { ref ->
                            val target = ref.resolve() ?: return@forEach
                            if (target.isInsideOf(elementsToMove)) return@forEach
                            if (target in resolveScope) return@forEach

                            val superMethods = SmartSet.create<PsiMethod>()
                            target.toLightMethods().forEach { superMethods += it.findDeepestSuperMethods() }
                            if (superMethods.any { it in resolveScope }) return@forEach

                            val refContainer = ref.element.getStrictParentOfType<KtNamedDeclaration>() ?: return@forEach
                            val scopeDescription = RefactoringUIUtil.getDescription(refContainer, true)
                            val message = RefactoringBundle.message("0.referenced.in.1.will.not.be.accessible.in.module.2",
                                                                    RefactoringUIUtil.getDescription(target, true),
                                                                    scopeDescription,
                                                                    CommonRefactoringUtil.htmlEmphasize(targetModule.name))
                            conflicts.putValue(target, CommonRefactoringUtil.capitalize(message))
                        }
            }
        }
    }

    fun checkVisibilityInUsages(usages: List<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {
        val declarationToContainers = HashMap<KtNamedDeclaration, MutableSet<PsiElement>>()
        for (usage in usages) {
            val element = usage.element
            if (element == null || usage !is MoveRenameUsageInfo || usage is NonCodeUsageInfo) continue

            if (element.isInsideOf(elementsToMove)) continue

            val referencedElement = usage.referencedElement?.namedUnwrappedElement as? KtNamedDeclaration ?: continue
            val referencedDescriptor = resolutionFacade.resolveToDescriptor(referencedElement)

            val container = element.getUsageContext()
            if (!declarationToContainers.getOrPut(referencedElement) { HashSet<PsiElement>() }.add(container)) continue

            val referencingDescriptor = when (container) {
                                            is KtDeclaration -> container.resolveToDescriptor()
                                            is PsiMember -> container.getJavaMemberDescriptor()
                                            else -> null
                                        } ?: continue
            val targetContainer = moveTarget.getContainerDescriptor() ?: continue
            val descriptorToCheck = referencedDescriptor.asPredicted(targetContainer) ?: continue

            if (!descriptorToCheck.isVisibleIn(referencingDescriptor)) {
                val message = "${render(container)} uses ${render(referencedElement)} which will be inaccessible after move"
                conflicts.putValue(element, message.capitalize())
            }
        }
    }

    fun checkVisibilityInDeclarations(conflicts: MultiMap<PsiElement, String>) {
        val targetContainer = moveTarget.getContainerDescriptor() ?: return
        for (declaration in elementsToMove) {
            declaration.forEachDescendantOfType<KtReferenceExpression> { refExpr ->
                refExpr.references
                        .forEach { ref ->
                            val target = ref.resolve() ?: return@forEach
                            if (target.isInsideOf(elementsToMove)) return@forEach
                            val targetDescriptor = when (target) {
                                                       is KtDeclaration -> target.resolveToDescriptor()
                                                       is PsiMember -> target.getJavaMemberDescriptor()
                                                       else -> null
                                                   } ?: return@forEach
                            if (!targetDescriptor.isVisibleIn(targetContainer)) {
                                val message = "${render(declaration)} uses ${render(target)} which will be inaccessible after move"
                                conflicts.putValue(refExpr, message.capitalize())
                            }
                        }
            }
        }
    }

    fun checkAllConflicts(usages: List<UsageInfo>, conflicts: MultiMap<PsiElement, String>) {
        checkModuleConflictsInUsages(usages, conflicts)
        checkModuleConflictsInDeclarations(conflicts)
        checkVisibilityInUsages(usages, conflicts)
        checkVisibilityInDeclarations(conflicts)
    }
}
