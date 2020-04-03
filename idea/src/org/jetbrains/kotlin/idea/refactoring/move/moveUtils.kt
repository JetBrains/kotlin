/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move

import com.intellij.ide.util.DirectoryUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.move.moveMembers.MoveMemberHandler
import com.intellij.refactoring.move.moveMembers.MoveMembersOptions
import com.intellij.refactoring.move.moveMembers.MoveMembersProcessor
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.refactoring.util.NonCodeUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.IncorrectOperationException
import com.intellij.util.SmartList
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithAllCompilerChecks
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.codeInsight.shorten.addDelayedImportRequest
import org.jetbrains.kotlin.idea.core.getPackage
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.refactoring.fqName.isImported
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference.ShorteningMode
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.statistics.MoveRefactoringFUSCollector
import org.jetbrains.kotlin.idea.util.application.executeCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.lang.System.currentTimeMillis
import java.util.*

sealed class ContainerInfo {
    abstract val fqName: FqName?
    abstract fun matches(descriptor: DeclarationDescriptor): Boolean

    object UnknownPackage : ContainerInfo() {
        override val fqName: FqName? = null
        override fun matches(descriptor: DeclarationDescriptor) = descriptor is PackageViewDescriptor
    }

    class Package(override val fqName: FqName) : ContainerInfo() {
        override fun matches(descriptor: DeclarationDescriptor): Boolean {
            return descriptor is PackageFragmentDescriptor && descriptor.fqName == fqName
        }

        override fun equals(other: Any?) = other is Package && other.fqName == fqName

        override fun hashCode() = fqName.hashCode()
    }

    class Class(override val fqName: FqName) : ContainerInfo() {
        override fun matches(descriptor: DeclarationDescriptor): Boolean {
            return descriptor is ClassDescriptor && descriptor.importableFqName == fqName
        }

        override fun equals(other: Any?) = other is Class && other.fqName == fqName

        override fun hashCode() = fqName.hashCode()
    }
}

data class ContainerChangeInfo(val oldContainer: ContainerInfo, val newContainer: ContainerInfo)

fun KtElement.getInternalReferencesToUpdateOnPackageNameChange(containerChangeInfo: ContainerChangeInfo): List<UsageInfo> {
    val usages = ArrayList<UsageInfo>()
    processInternalReferencesToUpdateOnPackageNameChange(containerChangeInfo) { expr, factory -> usages.addIfNotNull(factory(expr)) }
    return usages
}

private typealias UsageInfoFactory = (KtSimpleNameExpression) -> UsageInfo?

fun KtElement.processInternalReferencesToUpdateOnPackageNameChange(
    containerChangeInfo: ContainerChangeInfo,
    body: (originalRefExpr: KtSimpleNameExpression, usageFactory: UsageInfoFactory) -> Unit
) {
    val file = containingFile as? KtFile ?: return

    val importPaths = file.importDirectives.mapNotNull { it.importPath }

    tailrec fun isImported(descriptor: DeclarationDescriptor): Boolean {
        val fqName = DescriptorUtils.getFqName(descriptor).let { if (it.isSafe) it.toSafe() else return@isImported false }
        if (importPaths.any { fqName.isImported(it, false) }) return true

        return when (val containingDescriptor = descriptor.containingDeclaration) {
            is ClassDescriptor, is PackageViewDescriptor -> isImported(containingDescriptor)
            else -> false
        }
    }

    fun processReference(refExpr: KtSimpleNameExpression, bindingContext: BindingContext): (UsageInfoFactory)? {
        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.getImportableDescriptor() ?: return null
        val containingDescriptor = descriptor.containingDeclaration ?: return null

        val callableKind = (descriptor as? CallableMemberDescriptor)?.kind
        if (callableKind != null && callableKind != CallableMemberDescriptor.Kind.DECLARATION) return null

        // Special case for enum entry superclass references (they have empty text and don't need to be processed by the refactoring)
        if (refExpr.textRange.isEmpty) return null

        if (descriptor is ClassDescriptor && descriptor.isInner && refExpr.parent is KtCallExpression) return null

        val isCallable = descriptor is CallableDescriptor
        val isExtension = isCallable && refExpr.isExtensionRef(bindingContext)
        val isCallableReference = isCallableReference(refExpr.mainReference)

        val declaration by lazy {
            var result = DescriptorToSourceUtilsIde.getAnyDeclaration(project, descriptor) ?: return@lazy null

            if (descriptor.isCompanionObject() &&
                bindingContext[BindingContext.SHORT_REFERENCE_TO_COMPANION_OBJECT, refExpr] !== null
            ) {
                result = (result as? KtObjectDeclaration)?.containingClassOrObject ?: result
            }

            result
        }

        if (isCallable) {
            if (!isCallableReference) {
                if (isExtension && containingDescriptor is ClassDescriptor) {
                    val dispatchReceiver = refExpr.getResolvedCall(bindingContext)?.dispatchReceiver
                    val implicitClass = (dispatchReceiver as? ImplicitClassReceiver)?.classDescriptor
                    if (implicitClass?.isCompanionObject == true) {
                        return { ImplicitCompanionAsDispatchReceiverUsageInfo(it, implicitClass) }
                    }
                    if (dispatchReceiver != null || containingDescriptor.kind != ClassKind.OBJECT) return null
                }
            }

            if (!isExtension) {
                val isCompatibleDescriptor = containingDescriptor is PackageFragmentDescriptor ||
                        containingDescriptor is ClassDescriptor && containingDescriptor.kind == ClassKind.OBJECT ||
                        descriptor is JavaCallableMemberDescriptor && ((declaration as? PsiMember)?.hasModifierProperty(PsiModifier.STATIC) == true)
                if (!isCompatibleDescriptor) return null
            }
        }

        if (!DescriptorUtils.getFqName(descriptor).isSafe) return null

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

        val isImported = isImported(descriptor)
        if (isImported && this is KtFile) return null

        val declarationNotNull = declaration ?: return null

        if (isExtension || containerFqName != null || isImported) return {
            createMoveUsageInfoIfPossible(it.mainReference, declarationNotNull, addImportToOriginalFile = false, isInternal = true)
        }

        return null
    }

    @Suppress("DEPRECATION")
    val bindingContext = analyzeWithAllCompilerChecks().bindingContext
    forEachDescendantOfType<KtReferenceExpression> { refExpr ->
        if (refExpr !is KtSimpleNameExpression || refExpr.parent is KtThisExpression) return@forEachDescendantOfType

        processReference(refExpr, bindingContext)?.let { body(refExpr, it) }
    }
}

internal var KtSimpleNameExpression.internalUsageInfo: UsageInfo? by CopyablePsiUserDataProperty(Key.create("INTERNAL_USAGE_INFO"))

internal fun markInternalUsages(usages: Collection<UsageInfo>) {
    usages.forEach { (it.element as? KtSimpleNameExpression)?.internalUsageInfo = it }
}

internal fun restoreInternalUsages(
    scope: KtElement,
    oldToNewElementsMapping: Map<PsiElement, PsiElement>,
    forcedRestore: Boolean = false
): List<UsageInfo> {
    return scope.collectDescendantsOfType<KtSimpleNameExpression>().mapNotNull {
        val usageInfo = it.internalUsageInfo
        if (!forcedRestore && usageInfo?.element != null) return@mapNotNull usageInfo
        val referencedElement = (usageInfo as? MoveRenameUsageInfo)?.referencedElement ?: return@mapNotNull null
        val newReferencedElement = mapToNewOrThis(referencedElement, oldToNewElementsMapping)
        if (!newReferencedElement.isValid) return@mapNotNull null
        (usageInfo as? KotlinMoveUsage)?.refresh(it, newReferencedElement)
    }
}

internal fun cleanUpInternalUsages(usages: Collection<UsageInfo>) {
    usages.forEach { (it.element as? KtSimpleNameExpression)?.internalUsageInfo = null }
}

class ImplicitCompanionAsDispatchReceiverUsageInfo(
    callee: KtSimpleNameExpression,
    val companionDescriptor: ClassDescriptor
) : UsageInfo(callee)

interface KotlinMoveUsage {
    val isInternal: Boolean

    fun refresh(refExpr: KtSimpleNameExpression, referencedElement: PsiElement): UsageInfo?
}

class UnqualifiableMoveRenameUsageInfo(
    element: PsiElement,
    reference: PsiReference,
    referencedElement: PsiElement,
    val originalFile: PsiFile,
    val addImportToOriginalFile: Boolean,
    override val isInternal: Boolean
) : MoveRenameUsageInfo(
    element,
    reference,
    reference.rangeInElement.startOffset,
    reference.rangeInElement.endOffset,
    referencedElement,
    false
), KotlinMoveUsage {
    override fun refresh(refExpr: KtSimpleNameExpression, referencedElement: PsiElement): UsageInfo? {
        return UnqualifiableMoveRenameUsageInfo(
            refExpr,
            refExpr.mainReference,
            referencedElement,
            originalFile,
            addImportToOriginalFile,
            isInternal
        )
    }
}

class QualifiableMoveRenameUsageInfo(
    element: PsiElement,
    reference: PsiReference,
    referencedElement: PsiElement,
    override val isInternal: Boolean
) : MoveRenameUsageInfo(
    element,
    reference,
    reference.rangeInElement.startOffset,
    reference.rangeInElement.endOffset,
    referencedElement,
    false
),
    KotlinMoveUsage {
    override fun refresh(refExpr: KtSimpleNameExpression, referencedElement: PsiElement): UsageInfo? {
        return QualifiableMoveRenameUsageInfo(refExpr, refExpr.mainReference, referencedElement, isInternal)
    }
}

interface DeferredKotlinMoveUsage : KotlinMoveUsage {
    fun resolve(newElement: PsiElement): UsageInfo?
}

class CallableReferenceMoveRenameUsageInfo(
    element: PsiElement,
    reference: PsiReference,
    referencedElement: PsiElement,
    val originalFile: PsiFile,
    val addImportToOriginalFile: Boolean,
    override val isInternal: Boolean
) : MoveRenameUsageInfo(
    element,
    reference,
    reference.rangeInElement.startOffset,
    reference.rangeInElement.endOffset,
    referencedElement,
    false
), DeferredKotlinMoveUsage {
    override fun refresh(refExpr: KtSimpleNameExpression, referencedElement: PsiElement): UsageInfo? {
        return CallableReferenceMoveRenameUsageInfo(
            refExpr,
            refExpr.mainReference,
            referencedElement,
            originalFile,
            addImportToOriginalFile,
            isInternal
        )
    }

    override fun resolve(newElement: PsiElement): UsageInfo? {
        val target = newElement.unwrapped
        val element = element ?: return null
        val reference = reference ?: return null
        val referencedElement = referencedElement ?: return null
        if (target != null && target.isTopLevelKtOrJavaMember()) {
            element.getStrictParentOfType<KtCallableReferenceExpression>()?.receiverExpression?.delete()
            return UnqualifiableMoveRenameUsageInfo(
                element,
                reference,
                referencedElement,
                element.containingFile!!,
                addImportToOriginalFile,
                isInternal
            )
        }
        return QualifiableMoveRenameUsageInfo(element, reference, referencedElement, isInternal)
    }
}

fun createMoveUsageInfoIfPossible(
    reference: PsiReference,
    referencedElement: PsiElement,
    addImportToOriginalFile: Boolean,
    isInternal: Boolean
): UsageInfo? {
    val element = reference.element
    return when (getReferenceKind(reference, referencedElement)) {
        ReferenceKind.QUALIFIABLE -> QualifiableMoveRenameUsageInfo(
            element, reference, referencedElement, isInternal
        )
        ReferenceKind.UNQUALIFIABLE -> UnqualifiableMoveRenameUsageInfo(
            element, reference, referencedElement, element.containingFile!!, addImportToOriginalFile, isInternal
        )
        ReferenceKind.CALLABLE_REFERENCE -> CallableReferenceMoveRenameUsageInfo(
            element, reference, referencedElement, element.containingFile!!, addImportToOriginalFile, isInternal
        )
        else -> null
    }
}

private enum class ReferenceKind {
    QUALIFIABLE,
    UNQUALIFIABLE,
    CALLABLE_REFERENCE,
    IRRELEVANT
}

private fun KtSimpleNameExpression.isExtensionRef(bindingContext: BindingContext? = null): Boolean {
    val resolvedCall = getResolvedCall(bindingContext ?: analyze(BodyResolveMode.PARTIAL)) ?: return false
    if (resolvedCall is VariableAsFunctionResolvedCall) {
        return resolvedCall.variableCall.candidateDescriptor.isExtension || resolvedCall.functionCall.candidateDescriptor.isExtension
    }
    return resolvedCall.candidateDescriptor.isExtension
}

private fun getReferenceKind(reference: PsiReference, referencedElement: PsiElement): ReferenceKind {
    val target = referencedElement.unwrapped
    val element = reference.element as? KtSimpleNameExpression ?: return ReferenceKind.QUALIFIABLE

    if (element.getStrictParentOfType<KtSuperExpression>() != null) return ReferenceKind.IRRELEVANT

    if (element.isExtensionRef() &&
        reference.element.getNonStrictParentOfType<KtImportDirective>() == null
    ) return ReferenceKind.UNQUALIFIABLE

    element.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference }?.let {
        val receiverExpression = it.receiverExpression
        if (receiverExpression != null) {
            val lhs = it.analyze(BodyResolveMode.PARTIAL)[BindingContext.DOUBLE_COLON_LHS, receiverExpression]
            return if (lhs is DoubleColonLHS.Type) ReferenceKind.CALLABLE_REFERENCE else ReferenceKind.IRRELEVANT
        }
        if (target is KtDeclaration && target.parent is KtFile) return ReferenceKind.UNQUALIFIABLE
        if (target is PsiMember && target.containingClass == null) return ReferenceKind.UNQUALIFIABLE
    }

    return ReferenceKind.QUALIFIABLE
}

private fun isCallableReference(reference: PsiReference): Boolean {
    return reference is KtSimpleNameReference
            && reference.element.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference } != null
}

fun guessNewFileName(declarationsToMove: Collection<KtNamedDeclaration>): String? {
    if (declarationsToMove.isEmpty()) return null
    val representative = declarationsToMove.singleOrNull()
        ?: declarationsToMove.filterIsInstance<KtClassOrObject>().singleOrNull()
    val newFileName = representative?.run {
        if (containingKtFile.isScript()) "$name.kts" else "$name.${KotlinFileType.EXTENSION}"
    } ?: declarationsToMove.first().containingFile.name
    return newFileName.capitalize()
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

            val refactoringOptions = object : MoveMembersOptions {
                override fun getMemberVisibility(): String? = PsiModifier.PUBLIC
                override fun makeEnumConstant(): Boolean = true
                override fun getSelectedMembers(): Array<PsiMember> = arrayOf(newElement)
                override fun getTargetClassName(): String? = newClass.qualifiedName
            }

            val moveMembersUsageInfo = MoveMembersProcessor.MoveMembersUsageInfo(
                newElement, reference.element, newClass, reference.qualifierExpression, reference
            )

            val moveMemberHandler = MoveMemberHandler.EP_NAME.forLanguage(reference.element.language)
            if (moveMemberHandler != null) {
                moveMemberHandler.changeExternalUsage(refactoringOptions, moveMembersUsageInfo)
                return true
            }
        }
    }
    return false
}

internal fun mapToNewOrThis(e: PsiElement, oldToNewElementsMapping: Map<PsiElement, PsiElement>) = oldToNewElementsMapping[e] ?: e

private fun postProcessMoveUsage(
    usage: UsageInfo,
    oldToNewElementsMapping: Map<PsiElement, PsiElement>,
    nonCodeUsages: ArrayList<NonCodeUsageInfo>,
    shorteningMode: ShorteningMode
) {
    if (usage is NonCodeUsageInfo) {
        nonCodeUsages.add(usage)
        return
    }

    if (usage !is MoveRenameUsageInfo) return

    val oldElement = usage.referencedElement!!
    val newElement = mapToNewOrThis(oldElement, oldToNewElementsMapping)

    when (usage) {
        is DeferredKotlinMoveUsage -> {
            val newUsage = usage.resolve(newElement) ?: return
            postProcessMoveUsage(newUsage, oldToNewElementsMapping, nonCodeUsages, shorteningMode)
        }

        is UnqualifiableMoveRenameUsageInfo -> {
            val file = with(usage) {
                if (addImportToOriginalFile) originalFile else mapToNewOrThis(
                    originalFile,
                    oldToNewElementsMapping
                )
            } as KtFile
            addDelayedImportRequest(newElement, file)
        }

        else -> {
            val reference = (usage.element as? KtSimpleNameExpression)?.mainReference ?: usage.reference
            processReference(reference, newElement, shorteningMode, oldElement)
        }
    }
}

private fun processReference(reference: PsiReference?, newElement: PsiElement, shorteningMode: ShorteningMode, oldElement: PsiElement) {
    try {
        when {
            reference is KtSimpleNameReference -> reference.bindToElement(newElement, shorteningMode)
            reference is PsiReferenceExpression && updateJavaReference(reference, oldElement, newElement) -> return
            else -> reference?.bindToElement(newElement)
        }
    } catch (e: IncorrectOperationException) {
        // Suppress exception if bindToElement is not implemented
    }
}

/**
 * Perform usage postprocessing and return non-code usages
 */
fun postProcessMoveUsages(
    usages: Collection<UsageInfo>,
    oldToNewElementsMapping: Map<PsiElement, PsiElement> = Collections.emptyMap(),
    shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING
): List<NonCodeUsageInfo> {
    val sortedUsages = usages.sortedWith(
        Comparator { o1, o2 ->
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

    val progressStep = 1.0 / sortedUsages.size
    val progressIndicator = ProgressManager.getInstance().progressIndicator
    progressIndicator?.text = KotlinBundle.message("text.updating.usages.progress")
    usageLoop@ for ((i, usage) in sortedUsages.withIndex()) {
        progressIndicator?.fraction = (i + 1) * progressStep
        postProcessMoveUsage(usage, oldToNewElementsMapping, nonCodeUsages, shorteningMode)
    }
    progressIndicator?.text = ""

    return nonCodeUsages
}

var KtFile.updatePackageDirective: Boolean? by UserDataProperty(Key.create("UPDATE_PACKAGE_DIRECTIVE"))

sealed class OuterInstanceReferenceUsageInfo(element: PsiElement, private val isIndirectOuter: Boolean) : UsageInfo(element) {
    open fun reportConflictIfAny(conflicts: MultiMap<PsiElement, String>): Boolean {
        val element = element ?: return false

        if (isIndirectOuter) {
            conflicts.putValue(element, KotlinBundle.message("text.indirect.outer.instances.will.not.be.extracted.0", element.text))
            return true
        }

        return false
    }

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
        private val isDoubleReceiver: Boolean
    ) : OuterInstanceReferenceUsageInfo(callElement, isIndirectOuter) {
        val callElement: KtElement?
            get() = element as? KtElement

        override fun reportConflictIfAny(conflicts: MultiMap<PsiElement, String>): Boolean {
            if (super.reportConflictIfAny(conflicts)) return true

            val fullCall = callElement?.let { it.getQualifiedExpressionForSelector() ?: it } ?: return false
            return when {
                fullCall is KtQualifiedExpression -> {
                    conflicts.putValue(fullCall, KotlinBundle.message("text.qualified.call.will.not.be.processed.0", fullCall.text))
                    true
                }

                isDoubleReceiver -> {
                    conflicts.putValue(fullCall, KotlinBundle.message("text.member.extension.call.will.not.be.processed.0", fullCall.text))
                    true
                }
                else -> false
            }
        }
    }
}

@JvmOverloads
fun traverseOuterInstanceReferences(
    member: KtNamedDeclaration,
    stopAtFirst: Boolean,
    body: (OuterInstanceReferenceUsageInfo) -> Unit = {}
): Boolean {
    if (member is KtObjectDeclaration || member is KtClass && !member.isInner()) return false

    val context = member.analyzeWithContent()
    val containingClassOrObject = member.containingClassOrObject ?: return false
    val outerClassDescriptor = containingClassOrObject.unsafeResolveToDescriptor() as ClassDescriptor
    var found = false
    member.accept(
        object : PsiRecursiveElementWalkingVisitor() {
            private fun getOuterInstanceReference(element: PsiElement): OuterInstanceReferenceUsageInfo? {
                return when (element) {
                    is KtThisExpression -> {
                        val descriptor = context[BindingContext.REFERENCE_TARGET, element.instanceReference]
                        val isIndirect = when {
                            descriptor == outerClassDescriptor -> false
                            descriptor?.isAncestorOf(outerClassDescriptor, true) ?: false -> true
                            else -> return null
                        }
                        OuterInstanceReferenceUsageInfo.ExplicitThis(element, isIndirect)
                    }
                    is KtSimpleNameExpression -> {
                        val resolvedCall = element.getResolvedCall(context) ?: return null
                        val dispatchReceiver = resolvedCall.dispatchReceiver as? ImplicitReceiver
                        val extensionReceiver = resolvedCall.extensionReceiver as? ImplicitReceiver
                        var isIndirect = false
                        val isDoubleReceiver = when (outerClassDescriptor) {
                            dispatchReceiver?.declarationDescriptor -> extensionReceiver != null
                            extensionReceiver?.declarationDescriptor -> dispatchReceiver != null
                            else -> {
                                isIndirect = true
                                when {
                                    dispatchReceiver?.declarationDescriptor?.isAncestorOf(outerClassDescriptor, true) ?: false ->
                                        extensionReceiver != null
                                    extensionReceiver?.declarationDescriptor?.isAncestorOf(outerClassDescriptor, true) ?: false ->
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

fun collectOuterInstanceReferences(member: KtNamedDeclaration): List<OuterInstanceReferenceUsageInfo> {
    val result = SmartList<OuterInstanceReferenceUsageInfo>()
    traverseOuterInstanceReferences(member, false) { result += it }
    return result
}

@Throws(IncorrectOperationException::class)
internal fun getOrCreateDirectory(path: String, project: Project): PsiDirectory {

    File(path).toPsiDirectory(project)?.let { return it }

    return project.executeCommand(RefactoringBundle.message("move.title"), null) {
        runWriteAction {
            val fixUpSeparators = path.replace(File.separatorChar, '/')
            DirectoryUtil.mkdirs(PsiManager.getInstance(project), fixUpSeparators)
        }
    }
}

internal fun getTargetPackageFqName(targetContainer: PsiElement): FqName? {
    if (targetContainer is PsiDirectory) {
        val targetPackage = targetContainer.getPackage()
        return if (targetPackage != null) FqName(targetPackage.qualifiedName) else null
    }
    return if (targetContainer is KtFile) targetContainer.packageFqName else null
}

internal fun logFusForMoveRefactoring(
    numberOfEntities: Int,
    entity: MoveRefactoringFUSCollector.MovedEntity,
    destination: MoveRefactoringFUSCollector.MoveRefactoringDestination,
    isDefault: Boolean,
    body: Runnable
) {
    val timeStarted = currentTimeMillis()

    var succeeded = false
    try {
        body.run()
        succeeded = true
    } finally {
        MoveRefactoringFUSCollector.log(
            timeStarted = timeStarted,
            timeFinished = currentTimeMillis(),
            numberOfEntities = numberOfEntities,
            destination = destination,
            isDefault = isDefault,
            entity = entity,
            isSucceeded = succeeded,
        )
    }
}

internal fun <T> List<KtNamedDeclaration>.mapWithReadActionInProcess(
    project: Project,
    title: String,
    body: (KtNamedDeclaration) -> T
): List<T> = let { declarations ->
    val result = mutableListOf<T>()
    val task: Task.Modal = object : Task.Modal(project, title, false) {
        override fun run(indicator: ProgressIndicator) {
            val fraction: Double = 1.0 / declarations.size
            indicator.fraction = 0.0
            runReadAction {
                declarations.forEachIndexed { index, declaration ->
                    result.add(body(declaration))
                    indicator.fraction = fraction * index
                }
            }
        }
    }
    ProgressManager.getInstance().run(task)
    return result
}
