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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.move.MoveCallback
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor
import com.intellij.refactoring.move.moveClassesOrPackages.MoveClassHandler
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.refactoring.util.RefactoringUIUtil
import com.intellij.refactoring.util.TextOccurrencesUtil
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SmartList
import com.intellij.util.VisibilityUtil
import com.intellij.util.containers.MultiMap
import gnu.trove.THashMap
import gnu.trove.TObjectHashingStrategy
import org.jetbrains.kotlin.asJava.KtLightElement
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.KotlinFileReferencesResolver
import org.jetbrains.kotlin.idea.codeInsight.shorten.addToShorteningWaitSet
import org.jetbrains.kotlin.idea.core.deleteSingle
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.idea.refactoring.getUsageContext
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.refactoring.move.moveFilesOrDirectories.MoveKotlinClassHandler
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.isInsideOf
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.utils.keysToMap
import java.util.*

interface Mover: (KtNamedDeclaration, KtElement) -> KtNamedDeclaration {
    object Default : Mover {
        override fun invoke(originalElement: KtNamedDeclaration, targetContainer: KtElement): KtNamedDeclaration {
            return when (targetContainer) {
                is KtFile -> targetContainer.add(originalElement) as KtNamedDeclaration
                is KtClassOrObject -> targetContainer.addDeclaration(originalElement) as KtNamedDeclaration
                else -> error("Unexpected element: ${targetContainer.getElementTextWithContext()}")
            }.apply { originalElement.deleteSingle() }
        }
    }

    object Idle : Mover {
        override fun invoke(originalElement: KtNamedDeclaration, targetContainer: KtElement) = originalElement
    }
}

class MoveDeclarationsDescriptor(
        val elementsToMove: Collection<KtNamedDeclaration>,
        val moveTarget: KotlinMoveTarget,
        val delegate: MoveDeclarationsDelegate,
        val searchInCommentsAndStrings: Boolean = true,
        val searchInNonCode: Boolean = true,
        val updateInternalReferences: Boolean = true,
        val deleteSourceFiles: Boolean = false,
        val moveCallback: MoveCallback? = null,
        val openInEditor: Boolean = false
)

class MoveKotlinDeclarationsProcessor(
        val project: Project,
        val descriptor: MoveDeclarationsDescriptor,
        val mover: Mover = Mover.Default) : BaseRefactoringProcessor(project) {
    companion object {
        private val REFACTORING_NAME: String = KotlinRefactoringBundle.message("refactoring.move.top.level.declarations")
    }

    private var nonCodeUsages: Array<NonCodeUsageInfo>? = null
    private val elementsToMove = descriptor.elementsToMove.filter { e -> e.parent != descriptor.moveTarget.getTargetPsiIfExists(e) }
    private val kotlinToLightElementsBySourceFile = elementsToMove
            .groupBy { it.getContainingKtFile() }
            .mapValues { it.value.keysToMap { it.toLightElements() } }
    private val conflicts = MultiMap<PsiElement, String>()

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        val targetContainerFqName = descriptor.moveTarget.targetContainerFqName?.let {
            if (it.isRoot) UsageViewBundle.message("default.package.presentable.name") else it.asString()
        }
        return MoveMultipleElementsViewDescriptor(elementsToMove.toTypedArray(), targetContainerFqName)
    }

    private val usagesToProcessBeforeMove = SmartList<UsageInfo>()

    public override fun findUsages(): Array<UsageInfo> {
        val newContainerName = descriptor.moveTarget.targetContainerFqName?.asString() ?: ""

        fun collectUsages(kotlinToLightElements: Map<KtNamedDeclaration, List<PsiNamedElement>>, result: MutableList<UsageInfo>) {
            kotlinToLightElements.values.flatMap { it }.flatMapTo(result) { lightElement ->
                val newFqName = StringUtil.getQualifiedName(newContainerName, lightElement.name)

                val foundReferences = HashSet<PsiReference>()
                val projectScope = lightElement.project.projectScope()
                val results = ReferencesSearch
                        .search(lightElement, projectScope, false)
                        .mapNotNullTo(ArrayList()) { ref ->
                            if (foundReferences.add(ref) && elementsToMove.all { !it.isAncestor(ref.element)}) {
                                createMoveUsageInfoIfPossible(ref, lightElement, true)
                            }
                            else null
                        }

                val name = lightElement.getKotlinFqName()?.asString()
                if (name != null) {
                    TextOccurrencesUtil.findNonCodeUsages(
                            lightElement,
                            name,
                            descriptor.searchInCommentsAndStrings,
                            descriptor.searchInNonCode,
                            newFqName,
                            results
                    )
                }

                MoveClassHandler.EP_NAME.extensions.forEach { handler ->
                    if (handler !is MoveKotlinClassHandler) handler.preprocessUsages(results)
                }

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
            val declarationToContainers = HashMap<KtNamedDeclaration, MutableSet<PsiElement>>()
            for (usage in usages) {
                val element = usage.element
                if (element == null || usage !is MoveRenameUsageInfo || usage is NonCodeUsageInfo) continue

                val declaration = usage.referencedElement?.namedUnwrappedElement as? KtNamedDeclaration
                if (declaration == null || !declaration.isPrivate()) continue

                if (element.isInsideOf(elementsToMove)) continue

                val container = element.getUsageContext()
                if (!declarationToContainers.getOrPut(declaration) { HashSet<PsiElement>() }.add(container)) continue

                // todo: ok for now, will be replaced by proper visibility analysis
                val currentPackage = element.containingFile?.containingDirectory?.getPackage()
                if (currentPackage?.qualifiedName == newContainerName) continue

                conflicts.putValue(
                        declaration,
                        KotlinRefactoringBundle.message(
                                "package.private.0.will.no.longer.be.accessible.from.1",
                                RefactoringUIUtil.getDescription(declaration, true),
                                RefactoringUIUtil.getDescription(container, true)
                        )
                )
            }
        }

        fun collectConflictsInDeclarations() {
            if (newContainerName == UNKNOWN_PACKAGE_FQ_NAME.asString()) return

            val declarationToReferenceTargets = HashMap<KtNamedDeclaration, MutableSet<PsiElement>>()
            for (declaration in elementsToMove) {
                val referenceToContext = KotlinFileReferencesResolver.resolve(element = declaration, resolveQualifiers = false)
                for ((refExpr, bindingContext) in referenceToContext) {
                    val refTarget = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.let { descriptor ->
                        DescriptorToSourceUtilsIde.getAnyDeclaration(declaration.project, descriptor)
                    }
                    if (refTarget == null || refTarget.isInsideOf(elementsToMove)) continue

                    val packagePrivate = when(refTarget) {
                        is KtModifierListOwner ->
                            refTarget.isPrivate()
                        is PsiModifierListOwner ->
                            VisibilityUtil.getVisibilityModifier(refTarget.modifierList) == PsiModifier.PACKAGE_LOCAL
                        else -> false
                    }

                    if (!packagePrivate) continue

                    if (!declarationToReferenceTargets.getOrPut(declaration) { HashSet<PsiElement>() }.add(refTarget)) continue

                    // todo: ok for now, will be replaced by proper visibility analysis
                    val currentPackage = declaration.containingFile?.containingDirectory?.getPackage()
                    if (currentPackage?.qualifiedName == newContainerName) continue

                    conflicts.putValue(
                            declaration,
                            KotlinRefactoringBundle.message(
                                    "0.uses.package.private.1",
                                    RefactoringUIUtil.getDescription(declaration, true),
                                    RefactoringUIUtil.getDescription(refTarget, true)
                            ).capitalize()
                    )
                }
            }
        }

        val usages = ArrayList<UsageInfo>()
        for ((sourceFile, kotlinToLightElements) in kotlinToLightElementsBySourceFile) {
            kotlinToLightElements.keys.forEach {
                if (descriptor.updateInternalReferences) {
                    val packageNameInfo = descriptor.delegate.getContainerChangeInfo(it, descriptor.moveTarget)
                    val (usagesToProcessLater, usagesToProcessEarly) = it
                            .getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo)
                            .partition { it is MoveRenameUsageInfoForExtension }
                    usages.addAll(usagesToProcessLater)
                    usagesToProcessBeforeMove.addAll(usagesToProcessEarly)
                }
            }

            // No need to find and process usages if package is not changed
            val originalContainerName = descriptor.delegate.getOriginalContainerFqName(descriptor, sourceFile).asString()
            if (originalContainerName == newContainerName) return UsageInfo.EMPTY_ARRAY

            usages += descriptor.delegate.findUsages(descriptor)
            collectUsages(kotlinToLightElements, usages)
            collectConflictsInUsages(usages)
            collectConflictsInDeclarations()
            descriptor.delegate.collectConflicts(usages, conflicts)
        }

        descriptor.delegate.collectConflicts(usagesToProcessBeforeMove, conflicts)

        return UsageViewUtil.removeDuplicatedUsages(usages.toTypedArray())
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        return showConflicts(conflicts, refUsages.get())
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        fun moveDeclaration(declaration: KtNamedDeclaration, moveTarget: KotlinMoveTarget): KtNamedDeclaration? {
            val file = declaration.containingFile as? KtFile
            assert(file != null) { "${declaration.javaClass}: ${declaration.text}" }

            val targetContainer = moveTarget.getOrCreateTargetPsi(declaration)
                                  ?: throw AssertionError("Couldn't create Kotlin file for: ${declaration.javaClass}: ${declaration.text}")

            descriptor.delegate.preprocessDeclaration(descriptor, declaration)
            val newElement = mover(declaration, targetContainer)

            newElement.addToShorteningWaitSet()

            return newElement
        }

        try {
            val usageList = usages.toArrayList()

            descriptor.delegate.preprocessUsages(project, usageList)

            postProcessMoveUsages(usagesToProcessBeforeMove, shorteningMode = ShorteningMode.NO_SHORTENING)

            val oldToNewElementsMapping = THashMap<PsiElement, PsiElement>(
                    object: TObjectHashingStrategy<PsiElement> {
                        override fun equals(e1: PsiElement?, e2: PsiElement?): Boolean {
                            if (e1 === e2) return true
                            // Name should be enough to distinguish different light elements based on the same original declaration
                            if (e1 is KtLightElement<*, *> && e2 is KtLightElement<*, *>) {
                                return e1.getOrigin() == e2.getOrigin() && e1.name == e2.name
                            }
                            return e1 == e2
                        }

                        override fun computeHashCode(e: PsiElement?): Int {
                            return when (e) {
                                null -> 0
                                is KtLightElement<*, *> -> (e.getOrigin()?.hashCode() ?: 0) * 31 + (e.name?.hashCode() ?: 0)
                                else -> e.hashCode()
                            }
                        }
                    }
            )
            for ((sourceFile, kotlinToLightElements) in kotlinToLightElementsBySourceFile) {
                for ((oldDeclaration, oldLightElements) in kotlinToLightElements) {
                    val newDeclaration = moveDeclaration(oldDeclaration, descriptor.moveTarget)
                    if (newDeclaration == null) {
                        for (oldElement in oldLightElements) {
                            oldToNewElementsMapping[oldElement] = oldElement
                        }
                        continue
                    }

                    oldToNewElementsMapping[sourceFile] = newDeclaration.getContainingKtFile()

                    transaction!!.getElementListener(oldDeclaration).elementMoved(newDeclaration)
                    for ((oldElement, newElement) in oldLightElements.asSequence().zip(newDeclaration.toLightElements().asSequence())) {
                        oldToNewElementsMapping[oldElement] = newElement
                    }

                    if (descriptor.openInEditor) {
                        EditorHelper.openInEditor(newDeclaration)
                    }
                }

                if (descriptor.deleteSourceFiles) {
                    sourceFile.delete()
                }
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
        descriptor.moveCallback?.refactoringCompleted()
    }

    fun execute(usages: List<UsageInfo>) {
        execute(usages.toTypedArray())
    }

    override fun getCommandName(): String = REFACTORING_NAME
}
