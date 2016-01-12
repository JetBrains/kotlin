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

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.RefactoringSettings
import com.intellij.refactoring.copy.CopyFilesOrDirectoriesHandler
import com.intellij.refactoring.move.MoveHandler
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesProcessor
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFilesOrDirectoriesUtil
import com.intellij.refactoring.move.moveMembers.MockMoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMemberHandler
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SmartList
import org.jetbrains.kotlin.asJava.namedUnwrappedElement
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.KotlinFileReferencesResolver
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.fqName.isImported
import org.jetbrains.kotlin.idea.refactoring.isInJavaSourceRoot
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.ui.KotlinAwareMoveFilesOrDirectoriesDialog
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.parents
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

val UNKNOWN_PACKAGE_FQ_NAME = FqNameUnsafe("org.jetbrains.kotlin.idea.refactoring.move.<unknown-package>")

sealed class ContainerInfo() {
    abstract val fqName: FqName?
    abstract fun matches(descriptor: DeclarationDescriptor): Boolean

    object UnknownPackage : ContainerInfo() {
        override val fqName = null
        override fun matches(descriptor: DeclarationDescriptor) = descriptor is PackageViewDescriptor
    }

    class Package(override val fqName: FqName): ContainerInfo() {
        override fun matches(descriptor: DeclarationDescriptor): Boolean {
            return descriptor is PackageFragmentDescriptor && descriptor.fqName == fqName
        }
    }

    class Class(override val fqName: FqName) : ContainerInfo() {
        override fun matches(descriptor: DeclarationDescriptor): Boolean {
            return descriptor is ClassDescriptor && descriptor.importableFqName == fqName
        }
    }
}

data class ContainerChangeInfo(val oldContainer: ContainerInfo, val newContainer: ContainerInfo)

fun KtElement.getInternalReferencesToUpdateOnPackageNameChange(containerChangeInfo: ContainerChangeInfo): List<UsageInfo> {
    val usages = ArrayList<UsageInfo>()
    lazilyProcessInternalReferencesToUpdateOnPackageNameChange(containerChangeInfo) { expr, factory -> usages.addIfNotNull(factory(expr)) }
    return usages
}

fun KtElement.lazilyProcessInternalReferencesToUpdateOnPackageNameChange(
        containerChangeInfo: ContainerChangeInfo,
        body: (originalRefExpr: KtSimpleNameExpression, usageFactory: (KtSimpleNameExpression) -> UsageInfo?) -> Unit
) {
    val file = containingFile as? KtFile ?: return

    val importPaths = file.importDirectives.mapNotNull { it.importPath }

    tailrec fun isImported(descriptor: DeclarationDescriptor): Boolean {
        val fqName = DescriptorUtils.getFqName(descriptor).let { if (it.isSafe) it.toSafe() else return@isImported false }
        if (importPaths.any { fqName.isImported(it, false) }) return true

        val containingDescriptor = descriptor.containingDeclaration
        return when (containingDescriptor) {
            is ClassDescriptor, is PackageViewDescriptor -> isImported(containingDescriptor)
            else -> false
        }
    }

    fun processReference(refExpr: KtSimpleNameExpression, bindingContext: BindingContext): ((KtSimpleNameExpression) -> UsageInfo?)? {
        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.getImportableDescriptor() ?: return null

        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) ?: return null

        // Special case for enum entry superclass references (they have empty text and don't need to be processed by the refactoring)
        if (refExpr.textRange.isEmpty) return null

        if (descriptor is ClassDescriptor && descriptor.isInner && refExpr.parent is KtCallExpression) return null

        val isCallable = descriptor is CallableDescriptor
        val isExtension = isCallable && declaration.isExtensionDeclaration()

        if (isCallable) {
            val containingDescriptor = descriptor.containingDeclaration
            if (isExtension && containingDescriptor is ClassDescriptor) {
                val implicitClass = (refExpr.getResolvedCall(bindingContext)?.dispatchReceiver as? ImplicitClassReceiver)?.classDescriptor
                if (DescriptorUtils.isCompanionObject(implicitClass)) {
                    return { ImplicitCompanionAsDispatchReceiverUsageInfo(it) }
                }
                return null
            }
            if (!isExtension) {
                if (refExpr.getReceiverExpression() != null) {
                    return fun(refExpr: KtSimpleNameExpression): UsageInfo? {
                        val receiver = refExpr.getReceiverExpression() ?: return null
                        val receiverRef = receiver.getQualifiedElementSelector() as? KtSimpleNameExpression ?: return null
                        if (bindingContext[BindingContext.QUALIFIER, receiverRef] == null) return null
                        return processReference(receiverRef, bindingContext)?.invoke(receiverRef)
                    }
                }
                if (!(containingDescriptor is PackageFragmentDescriptor
                      || containingDescriptor is ClassDescriptor && containingDescriptor.kind == ClassKind.OBJECT)) return null
            }
        }

        val fqName = DescriptorUtils.getFqName(descriptor)
        if (!fqName.isSafe) return null

        val (oldContainer, newContainer) = containerChangeInfo

        val containerFqName = descriptor
                .parents
                .mapNotNull {
                    when {
                        oldContainer.matches(it) -> oldContainer.fqName
                        newContainer.matches(it) -> newContainer.fqName
                        else -> null
                    }
                }
                .firstOrNull()

        fun doCreateUsageInfo(refExpr: KtSimpleNameExpression): UsageInfo? {
            if (isAncestor(declaration, false)) {
                if (descriptor.importableFqName == null) return null
                if (isUnqualifiedExtensionReference(refExpr.mainReference, declaration)) return null
                if (containerFqName == null || newContainer is ContainerInfo.UnknownPackage) return null
                return fqName.asString().let {
                    val prefix = containerFqName.asString()
                    val prefixOffset = it.indexOf(prefix)
                    val newFqName = FqName(it.replaceRange(prefixOffset..prefixOffset + prefix.length - 1, newContainer.fqName!!.asString()))
                    MoveRenameSelfUsageInfo(refExpr.mainReference, declaration, newFqName)
                }
            }

            return createMoveUsageInfoIfPossible(refExpr.mainReference, declaration, false)
        }

        if (isExtension || containerFqName != null || isImported(descriptor)) return ::doCreateUsageInfo
        return null
    }

    val referenceToContext = KotlinFileReferencesResolver.resolve(file = file, elements = listOf(this))

    for ((refExpr, bindingContext) in referenceToContext) {
        if (refExpr !is KtSimpleNameExpression || refExpr.parent is KtThisExpression) continue
        if (bindingContext[BindingContext.QUALIFIER, refExpr] != null) continue

        processReference(refExpr, bindingContext)?.let { body(refExpr, it) }
    }
}

class ImplicitCompanionAsDispatchReceiverUsageInfo(callee: KtSimpleNameExpression) : UsageInfo(callee)

class MoveRenameUsageInfoForExtension(
        element: PsiElement,
        reference: PsiReference,
        startOffset: Int,
        endOffset: Int,
        referencedElement: PsiElement,
        val originalFile: PsiFile,
        val addImportToOriginalFile: Boolean
): MoveRenameUsageInfo(element, reference, startOffset, endOffset, referencedElement, false)

class MoveRenameSelfUsageInfo(ref: KtSimpleNameReference, refTarget: PsiElement, val newFqName: FqName):
        MoveRenameUsageInfo(ref.element, ref, ref.rangeInElement.startOffset, ref.rangeInElement.endOffset, refTarget, false) {
    override fun getReference() = super.getReference() as? KtSimpleNameReference
}

fun createMoveUsageInfoIfPossible(
        reference: PsiReference,
        referencedElement: PsiElement,
        addImportToOriginalFile: Boolean
): UsageInfo? {
    val element = reference.element
    if (element.getStrictParentOfType<KtSuperExpression>() != null) return null

    val range = reference.rangeInElement!!
    val startOffset = range.startOffset
    val endOffset = range.endOffset

    if (isUnqualifiedExtensionReference(reference, referencedElement)) {
        return MoveRenameUsageInfoForExtension(
                element, reference, startOffset, endOffset, referencedElement, element.containingFile!!, addImportToOriginalFile
        )
    }
    return MoveRenameUsageInfo(element, reference, startOffset, endOffset, referencedElement, false)
}

private fun isUnqualifiedExtensionReference(reference: PsiReference, referencedElement: PsiElement): Boolean {
    return reference is KtReference
           && (referencedElement.namedUnwrappedElement as? KtDeclaration)?.isExtensionDeclaration() ?: false
           && reference.element.getNonStrictParentOfType<KtImportDirective>() == null
}

fun guessNewFileName(declarationsToMove: Collection<KtNamedDeclaration>): String? {
    if (declarationsToMove.isEmpty()) return null

    val representative = declarationsToMove.singleOrNull()
                         ?: declarationsToMove.filterIsInstance<KtClassOrObject>().singleOrNull()
    representative?.let { return "${it.name}.${KotlinFileType.EXTENSION}" }

    return declarationsToMove.first().containingFile.name
}

// returns true if successful
private fun updateJavaReference(reference: PsiReferenceExpression, oldElement: PsiElement, newElement: PsiElement): Boolean {
    if (oldElement is PsiMember && newElement is PsiMember) {
        // Remove import of old package facade, if any
        val oldClassName = oldElement.containingClass?.qualifiedName
        if (oldClassName != null) {
            val importOfOldClass = (reference.containingFile as? PsiJavaFile)?.importList?.allImportStatements?.firstOrNull {
                when (it) {
                    is PsiImportStatement -> it.qualifiedName == oldClassName
                    is PsiImportStaticStatement -> it.isOnDemand && it.importReference?.canonicalText == oldClassName
                    else -> false
                }
            }
            if (importOfOldClass != null && importOfOldClass.resolve() == null) {
                importOfOldClass.delete()
            }
        }

        val newClass = newElement.containingClass
        if (newClass != null && reference.qualifierExpression != null) {
            val mockMoveMembersOptions = MockMoveMembersOptions(newClass.qualifiedName, arrayOf(newElement))
            val moveMembersUsageInfo = MoveMembersProcessor.MoveMembersUsageInfo(
                    newElement, reference.element, newClass, reference.qualifierExpression, reference)
            val moveMemberHandler = MoveMemberHandler.EP_NAME.forLanguage(reference.element.language)
            if (moveMemberHandler != null) {
                moveMemberHandler.changeExternalUsage(mockMoveMembersOptions, moveMembersUsageInfo)
                return true
            }
        }
    }
    return false
}

/**
 * Perform usage postprocessing and return non-code usages
 */
fun postProcessMoveUsages(usages: List<UsageInfo>,
                          oldToNewElementsMapping: Map<PsiElement, PsiElement> = Collections.emptyMap(),
                          shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING
): List<NonCodeUsageInfo> {
    fun counterpart(e: PsiElement) = oldToNewElementsMapping[e] ?: e

    val sortedUsages = usages.sortedWith(
            Comparator<UsageInfo> { o1, o2 ->
                val file1 = o1.virtualFile
                val file2 = o2.virtualFile
                if (Comparing.equal(file1, file2)) {
                    val rangeInElement1 = o1.rangeInElement
                    val rangeInElement2 = o2.rangeInElement
                    if (rangeInElement1 != null && rangeInElement2 != null) {
                        return@Comparator rangeInElement2.startOffset - rangeInElement1.startOffset
                    }
                    return@Comparator 0
                }
                if (file1 == null) return@Comparator -1
                if (file2 == null) return@Comparator 1
                Comparing.compare(file1.path, file2.path)
            }
    )

    val nonCodeUsages = ArrayList<NonCodeUsageInfo>()

    usageLoop@ for (usage in sortedUsages) {
        when (usage) {
            is NonCodeUsageInfo -> {
                nonCodeUsages.add(usage)
            }

            is MoveRenameSelfUsageInfo -> {
                usage.reference?.bindToFqName(usage.newFqName, shorteningMode)
            }

            is MoveRenameUsageInfoForExtension -> {
                val file = with(usage) { if (addImportToOriginalFile) originalFile else counterpart(originalFile) } as KtFile
                val declaration = counterpart(usage.referencedElement!!).unwrapped as KtDeclaration
                ImportInsertHelper.getInstance(usage.project).importDescriptor(file, declaration.resolveToDescriptor())
            }

            is MoveRenameUsageInfo -> {
                val oldElement = usage.referencedElement!!
                val newElement = counterpart(oldElement)
                val reference = usage.reference ?: (usage.element as? KtSimpleNameExpression)?.mainReference
                try {
                    when {
                        reference is KtSimpleNameReference -> reference.bindToElement(newElement, shorteningMode)
                        reference is PsiReferenceExpression && updateJavaReference(reference, oldElement, newElement) -> continue@usageLoop
                        else -> reference?.bindToElement(newElement)
                    }
                }
                catch (e: IncorrectOperationException) {
                    // Suppress exception if bindToElement is not implemented
                }
            }
        }
    }

    return nonCodeUsages
}

var KtFile.updatePackageDirective: Boolean? by UserDataProperty(Key.create("UPDATE_PACKAGE_DIRECTIVE"))

// Mostly copied from MoveFilesOrDirectoriesUtil.doMove()
fun moveFilesOrDirectories(
        project: Project,
        elements: Array<PsiElement>,
        targetElement: PsiElement?,
        moveCallback: (() -> Unit)? = null
) {
    elements.forEach { if (it !is PsiFile && it !is PsiDirectory) throw IllegalArgumentException("unexpected element type: " + it) }

    val targetDirectory = MoveFilesOrDirectoriesUtil.resolveToDirectory(project, targetElement)
    if (targetElement != null && targetDirectory == null) return

    val initialTargetDirectory = MoveFilesOrDirectoriesUtil.getInitialTargetDirectory(targetDirectory, elements)

    fun doRun(moveDialog: KotlinAwareMoveFilesOrDirectoriesDialog?) {
        fun closeDialog() {
            moveDialog?.close(DialogWrapper.CANCEL_EXIT_CODE)
        }

        project.executeCommand(MoveHandler.REFACTORING_NAME) {
            val selectedDir = if (moveDialog != null) moveDialog.targetDirectory else initialTargetDirectory
            val updatePackageDirective = (moveDialog as? KotlinAwareMoveFilesOrDirectoriesDialog)?.updatePackageDirective

            try {
                val choice = if (elements.size > 1 || elements[0] is PsiDirectory) intArrayOf(-1) else null
                val elementsToMove = elements.filterNot {
                    it is PsiFile
                    && runWriteAction { CopyFilesOrDirectoriesHandler.checkFileExist(selectedDir, choice, it, it.name, "Move") }
                }

                elementsToMove.forEach {
                    MoveFilesOrDirectoriesUtil.checkMove(it, selectedDir!!)
                    if (it is KtFile && it.isInJavaSourceRoot()) {
                        it.updatePackageDirective = updatePackageDirective
                    }
                }

                if (elementsToMove.isNotEmpty()) {
                    MoveFilesOrDirectoriesProcessor(
                            project,
                            elementsToMove.toTypedArray(),
                            selectedDir,
                            RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE,
                            false,
                            false,
                            moveCallback,
                            ::closeDialog
                    ).run()
                }
                else {
                    closeDialog()
                }
            }
            catch (e: IncorrectOperationException) {
                CommonRefactoringUtil.showErrorMessage(RefactoringBundle.message("error.title"), e.message, "refactoring.moveFile", project)
            }
        }
    }

    if (ApplicationManager.getApplication().isUnitTestMode) {
        doRun(null)
        return
    }

    with(KotlinAwareMoveFilesOrDirectoriesDialog(project, ::doRun)) {
        setData(elements, initialTargetDirectory, "refactoring.moveFile")
        show()
    }
}

sealed class OuterInstanceReferenceUsageInfo(element: PsiElement, val isIndirectOuter: Boolean) : UsageInfo(element) {
    class ExplicitThis(
            expression: KtThisExpression,
            isIndirectOuter: Boolean
    ) : OuterInstanceReferenceUsageInfo(expression, isIndirectOuter) {
        val expression: KtThisExpression?
            get() = element as? KtThisExpression
    }

    class ImplicitReceiver(
            callElement: KtElement,
            isIndirectOuter: Boolean,
            val isDoubleReceiver: Boolean
    ) : OuterInstanceReferenceUsageInfo(callElement, isIndirectOuter) {
        val callElement: KtElement?
            get() = element as? KtElement
    }
}

@JvmOverloads
fun traverseOuterInstanceReferences(innerClass: KtClass, stopAtFirst: Boolean, body: (OuterInstanceReferenceUsageInfo) -> Unit = {}): Boolean {
    if (!innerClass.isInner()) return false

    val context = innerClass.analyzeFully()
    val innerClassDescriptor = innerClass.resolveToDescriptorIfAny() as? ClassDescriptor ?: return false
    val outerClassDescriptor = innerClassDescriptor.containingDeclaration as? ClassDescriptor ?: return false
    var found = false
    innerClass.accept(
            object : PsiRecursiveElementWalkingVisitor() {
                private fun getOuterInstanceReference(element: PsiElement): OuterInstanceReferenceUsageInfo? {
                    return when (element) {
                        is KtThisExpression -> {
                            val descriptor = context[BindingContext.REFERENCE_TARGET, element.instanceReference]
                            val isIndirect = when {
                                descriptor == outerClassDescriptor -> false
                                DescriptorUtils.isAncestor(descriptor, outerClassDescriptor, true) -> true
                                else -> return null
                            }
                            OuterInstanceReferenceUsageInfo.ExplicitThis(element, isIndirect)
                        }
                        is KtSimpleNameExpression -> {
                            val resolvedCall = element.getResolvedCall(context) ?: return null
                            val dispatchReceiver = resolvedCall.dispatchReceiver as? ImplicitReceiver
                            val extensionReceiver = resolvedCall.extensionReceiver as? ImplicitReceiver
                            var isIndirect = false
                            val isDoubleReceiver = when {
                                dispatchReceiver?.declarationDescriptor == outerClassDescriptor -> extensionReceiver != null
                                extensionReceiver?.declarationDescriptor == outerClassDescriptor -> dispatchReceiver != null
                                else -> {
                                    isIndirect = true
                                    when {
                                        DescriptorUtils.isAncestor(dispatchReceiver?.declarationDescriptor, outerClassDescriptor, true) ->
                                            extensionReceiver != null
                                        DescriptorUtils.isAncestor(extensionReceiver?.declarationDescriptor, outerClassDescriptor, true) ->
                                            dispatchReceiver != null
                                        else -> return null
                                    }
                                }
                            }
                            OuterInstanceReferenceUsageInfo.ImplicitReceiver(resolvedCall.call.callElement, isIndirect, isDoubleReceiver)
                        }
                        else -> null
                    }
                }

                override fun visitElement(element: PsiElement) {
                    getOuterInstanceReference(element)?.let {
                        body(it)
                        found = true
                        if (stopAtFirst) stopWalking()
                        return
                    }
                    super.visitElement(element)
                }
            }
    )
    return found
}

fun collectOuterInstanceReferences(innerClass: KtClass): List<OuterInstanceReferenceUsageInfo> {
    return SmartList<OuterInstanceReferenceUsageInfo>().apply { traverseOuterInstanceReferences(innerClass, false) { add(it) } }
}
