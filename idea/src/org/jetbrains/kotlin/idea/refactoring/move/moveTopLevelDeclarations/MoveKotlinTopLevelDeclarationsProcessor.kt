/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassHandler
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassesOrPackagesUtil
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.refactoring.util.TextOccurrencesUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.VisibilityUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.JetFileReferencesResolver
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.deleteSingle
import org.jetbrains.kotlin.idea.core.refactoring.getUsageContext
import org.jetbrains.kotlin.idea.refactoring.JetRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetModifierListOwner
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isInsideOf
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.keysToMap
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

trait Mover: (originalElement: JetNamedDeclaration, targetFile: JetFile) -> JetNamedDeclaration {
    object Default: Mover {
        override fun invoke(originalElement: JetNamedDeclaration, targetFile: JetFile): JetNamedDeclaration {
            val newElement = targetFile.add(originalElement) as JetNamedDeclaration
            originalElement.deleteSingle()
            return newElement
        }
    }

    object Idle: Mover {
        override fun invoke(originalElement: JetNamedDeclaration, targetFile: JetFile) = originalElement
    }
}

public class MoveKotlinTopLevelDeclarationsOptions(
        val sourceFile: JetFile,
        val elementsToMove: Collection<JetNamedDeclaration>,
        val moveTarget: KotlinMoveTarget,
        val searchInCommentsAndStrings: Boolean = true,
        val searchInNonCode: Boolean = true,
        val updateInternalReferences: Boolean = true,
        val deleteSourceFile: Boolean = false,
        val moveCallback: MoveCallback? = null
)

public class MoveKotlinTopLevelDeclarationsProcessor(
        val project: Project,
        val options: MoveKotlinTopLevelDeclarationsOptions,
        val mover: Mover = Mover.Default) : BaseRefactoringProcessor(project) {
    companion object {
        private val REFACTORING_NAME: String = JetRefactoringBundle.message("refactoring.move.top.level.declarations")
    }

    private var nonCodeUsages: Array<NonCodeUsageInfo>? = null
    private val elementsToMove = options.elementsToMove.filter { e -> e.getContainingFile() != options.moveTarget.getTargetPsiIfExists(e) }
    private val kotlinToLightElements = elementsToMove.keysToMap { it.toLightElements() }
    private val conflicts = MultiMap<PsiElement, String>()

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return MoveMultipleElementsViewDescriptor(
                elementsToMove.toTypedArray(),
                MoveClassesOrPackagesUtil.getPackageName(options.moveTarget.packageWrapper)
        )
    }

    public override fun findUsages(): Array<UsageInfo> {
        val newPackageName = options.moveTarget.packageWrapper?.getQualifiedName() ?: ""

        fun collectUsages(): List<UsageInfo> {
            return kotlinToLightElements.values().flatMap { it }.flatMap { lightElement ->
                val newFqName = StringUtil.getQualifiedName(newPackageName, lightElement.getName())

                val foundReferences = HashSet<PsiReference>()
                val projectScope = lightElement.getProject().projectScope()
                val results = ReferencesSearch
                        .search(lightElement, projectScope, false)
                        .mapTo(ArrayList<UsageInfo?>()) { ref ->
                            if (foundReferences.add(ref) && elementsToMove.all { !it.isAncestor(ref.getElement())}) {
                                createMoveUsageInfoIfPossible(ref, lightElement, true)
                            }
                            else null
                        }
                        .filterNotNull()

                val name = lightElement.getKotlinFqName()?.asString()
                if (name != null) {
                    TextOccurrencesUtil.findNonCodeUsages(
                            lightElement,
                            name,
                            options.searchInCommentsAndStrings,
                            options.searchInNonCode,
                            newFqName,
                            results
                    )
                }

                MoveClassHandler.EP_NAME.getExtensions().forEach { handler -> handler.preprocessUsages(results) }

                results
            }
        }

        /*
            There must be a conflict if all of the following conditions are satisfied:
                declaration is package-private
                usage does not belong to target package
                usage is not being moved together with declaration
         */

        fun collectConflictsInUsages(usages: List<UsageInfo>) {
            val declarationToContainers = HashMap<JetNamedDeclaration, MutableSet<PsiElement>>()
            for (usage in usages) {
                val element = usage.getElement()
                if (element == null || usage !is MoveRenameUsageInfo || usage is NonCodeUsageInfo) continue

                val declaration = usage.getReferencedElement()?.namedUnwrappedElement as? JetNamedDeclaration
                if (declaration == null || !declaration.isPrivate()) continue

                if (element.isInsideOf(elementsToMove)) continue

                val container = element.getUsageContext()
                if (!declarationToContainers.getOrPut(declaration) { HashSet<PsiElement>() }.add(container)) continue

                val currentPackage = element.getContainingFile()?.getContainingDirectory()?.getPackage()
                if (currentPackage?.getQualifiedName() == newPackageName) continue

                conflicts.putValue(
                        declaration,
                        JetRefactoringBundle.message(
                                "package.private.0.will.no.longer.be.accessible.from.1",
                                RefactoringUIUtil.getDescription(declaration, true),
                                RefactoringUIUtil.getDescription(container, true)
                        )
                )
            }
        }

        fun collectConflictsInDeclarations() {
            val declarationToReferenceTargets = HashMap<JetNamedDeclaration, MutableSet<PsiElement>>()
            for (declaration in elementsToMove) {
                val referenceToContext = JetFileReferencesResolver.resolve(element = declaration, resolveQualifiers = false)
                for ((refExpr, bindingContext) in referenceToContext) {
                    val refTarget = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.let { descriptor ->
                        DescriptorToSourceUtilsIde.getAnyDeclaration(declaration.getProject(), descriptor)
                    }
                    if (refTarget == null || refTarget.isInsideOf(elementsToMove)) continue

                    val packagePrivate = when(refTarget) {
                        is JetModifierListOwner ->
                            refTarget.isPrivate()
                        is PsiModifierListOwner ->
                            VisibilityUtil.getVisibilityModifier(refTarget.getModifierList()) == PsiModifier.PACKAGE_LOCAL
                        else -> false
                    }

                    if (!packagePrivate) continue

                    if (!declarationToReferenceTargets.getOrPut(declaration) { HashSet<PsiElement>() }.add(refTarget)) continue

                    val currentPackage = declaration.getContainingFile()?.getContainingDirectory()?.getPackage()
                    if (currentPackage?.getQualifiedName() == newPackageName) continue

                    conflicts.putValue(
                            declaration,
                            JetRefactoringBundle.message(
                                    "0.uses.package.private.1",
                                    RefactoringUIUtil.getDescription(declaration, true),
                                    RefactoringUIUtil.getDescription(refTarget, true)
                            ).capitalize()
                    )
                }
            }
        }

        // No need to find and process usages if package is not changed
        if (options.sourceFile.getPackageFqName().asString() == newPackageName) return UsageInfo.EMPTY_ARRAY

        val usages = collectUsages()
        collectConflictsInUsages(usages)
        collectConflictsInDeclarations()
        return UsageViewUtil.removeDuplicatedUsages(usages.toTypedArray())
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        return showConflicts(conflicts, refUsages.get())
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        fun moveDeclaration(
                declaration: JetNamedDeclaration,
                moveTarget: KotlinMoveTarget,
                usagesToProcessAfterMove: MutableList<UsageInfo>
        ): JetNamedDeclaration? {
            val file = declaration.getContainingFile() as? JetFile
            assert(file != null) { "${declaration.javaClass}: ${declaration.getText()}" }

            val targetFile = moveTarget.getOrCreateTargetPsi(declaration) as? JetFile
                             ?: throw AssertionError("Couldn't create Kotlin file for: ${declaration.javaClass}: ${declaration.getText()}")

            if (options.updateInternalReferences) {
                val packageNameInfo = PackageNameInfo(file!!.getPackageFqName(), targetFile.getPackageFqName())
                val (usagesToProcessLater, usagesToProcessNow) = declaration
                        .getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo)
                        .partition { it is MoveRenameUsageInfoForExtension }
                postProcessMoveUsages(usagesToProcessNow, shorteningMode = ShorteningMode.NO_SHORTENING)
                usagesToProcessAfterMove.addAll(usagesToProcessLater)
            }

            val newElement = mover(declaration, targetFile)

            newElement.addToShorteningWaitSet()

            return newElement
        }

        try {
            val usageList = usages.toArrayList()

            val oldToNewElementsMapping = HashMap<PsiElement, PsiElement>()
            for ((oldDeclaration, oldLightElements) in kotlinToLightElements) {
                val oldFile = oldDeclaration.getContainingJetFile()

                val newDeclaration = moveDeclaration(oldDeclaration, options.moveTarget, usageList)
                if (newDeclaration == null) {
                    for (oldElement in oldLightElements) {
                        oldToNewElementsMapping[oldElement] = oldElement
                    }
                    continue
                }

                oldToNewElementsMapping[oldFile] = newDeclaration.getContainingJetFile()

                getTransaction()!!.getElementListener(oldDeclaration).elementMoved(newDeclaration)
                for ((oldElement, newElement) in oldLightElements.asSequence() zip newDeclaration.toLightElements().asSequence()) {
                    oldToNewElementsMapping[oldElement] = newElement
                }
            }

            if (options.deleteSourceFile) {
                options.sourceFile.delete()
            }

            nonCodeUsages = postProcessMoveUsages(usageList, oldToNewElementsMapping).toTypedArray()
        }
        catch (e: IncorrectOperationException) {
            nonCodeUsages = null
            RefactoringUIUtil.processIncorrectOperation(myProject, e)
        }
    }

    override fun performPsiSpoilingRefactoring() {
        nonCodeUsages?.let { nonCodeUsages -> RenameUtil.renameNonCodeUsages(myProject, nonCodeUsages) }
        options.moveCallback?.refactoringCompleted()
    }

    fun execute(usages: List<UsageInfo>) {
        execute(usages.toTypedArray())
    }

    override fun getCommandName(): String = REFACTORING_NAME
}
