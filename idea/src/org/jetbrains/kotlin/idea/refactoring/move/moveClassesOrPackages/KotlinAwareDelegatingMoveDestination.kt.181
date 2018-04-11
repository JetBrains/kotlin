/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveClassesOrPackages

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.MoveDestination
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.refactoring.move.createMoveUsageInfoIfPossible
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinDirectoryMoveTarget
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.analyzeConflictsInFile
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.stubindex.KotlinExactPackagesIndex
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

class KotlinAwareDelegatingMoveDestination(
        private val delegate: MoveDestination,
        private val targetPackage: PsiPackage?,
        private val targetDirectory: PsiDirectory?
) : MoveDestination by delegate {
    override fun analyzeModuleConflicts(
            elements: MutableCollection<PsiElement>,
            conflicts: MultiMap<PsiElement, String>,
            usages: Array<out UsageInfo>
    ) {
        delegate.analyzeModuleConflicts(elements, conflicts, usages)

        if (targetPackage == null || targetDirectory == null) return

        val project = targetDirectory.project
        val moveTarget = KotlinDirectoryMoveTarget(FqName(targetPackage.qualifiedName), targetDirectory)
        val packagesIndex = KotlinExactPackagesIndex.getInstance()
        val directoriesToMove = elements.flatMapTo(LinkedHashSet<PsiDirectory>()) {
            (it as? PsiPackage)?.directories?.toList() ?: emptyList()
        }
        val projectScope = project.projectScope()
        val filesToProcess = elements.flatMapTo(LinkedHashSet<KtFile>()) {
            if (it is PsiPackage) packagesIndex[it.qualifiedName, project, projectScope] else emptyList()
        }

        val extraElementsForReferenceSearch = LinkedHashSet<PsiElement>()
        val extraElementCollector = object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element is KtNamedDeclaration && element.hasModifier(KtTokens.INTERNAL_KEYWORD)) {
                    element.parentsWithSelf.lastOrNull { it is KtNamedDeclaration }?.let { extraElementsForReferenceSearch += it }
                    stopWalking()
                }
                super.visitElement(element)
            }
        }
        filesToProcess.flatMap {it.declarations}.forEach { it.accept(extraElementCollector) }

        val progressIndicator = ProgressManager.getInstance().progressIndicator!!
        progressIndicator.pushState()

        val extraUsages = ArrayList<UsageInfo>()
        try {
            progressIndicator.text = "Looking for Usages"
            for ((index, element) in extraElementsForReferenceSearch.withIndex()) {
                progressIndicator.fraction = (index + 1)/extraElementsForReferenceSearch.size.toDouble()
                ReferencesSearch.search(element, projectScope).mapNotNullTo(extraUsages) { ref ->
                    createMoveUsageInfoIfPossible(ref, element, true, false)
                }
            }
        }
        finally {
            progressIndicator.popState()
        }

        filesToProcess.forEach {
            analyzeConflictsInFile(it, extraUsages, moveTarget, directoriesToMove, conflicts) {}
        }
    }
}