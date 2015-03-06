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

import org.jetbrains.kotlin.idea.codeInsight.JetFileReferencesResolver
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import java.util.Collections
import org.jetbrains.kotlin.idea.refactoring.fqName.isImported
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.psi.psiUtil.isExtensionDeclaration
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.util.MoveRenameUsageInfo
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.util.ArrayList
import com.intellij.refactoring.util.NonCodeUsageInfo
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.references.JetSimpleNameReference.ShorteningMode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.Comparing
import java.util.Comparator
import com.intellij.util.IncorrectOperationException
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor.Kind
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.asJava.*
import org.jetbrains.kotlin.psi.JetSuperExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

public class PackageNameInfo(val oldPackageName: FqName, val newPackageName: FqName)

public fun JetElement.getInternalReferencesToUpdateOnPackageNameChange(packageNameInfo: PackageNameInfo): List<UsageInfo> {
    val file = getContainingFile() as? JetFile
    if (file == null) return Collections.emptyList()

    val importPaths = file.getImportDirectives().map { it.getImportPath() }.filterNotNull()

    [tailRecursive] fun isImported(descriptor: DeclarationDescriptor): Boolean {
        val fqName = DescriptorUtils.getFqName(descriptor).let { if (it.isSafe()) it.toSafe() else return@isImported false }
        if (importPaths.any { fqName.isImported(it, false) }) return true

        val containingDescriptor = descriptor.getContainingDeclaration()
        return when (containingDescriptor) {
            is ClassDescriptor, is PackageViewDescriptor -> isImported(containingDescriptor)
            else -> false
        }
    }

    fun processReference(refExpr: JetSimpleNameExpression, bindingContext: BindingContext): UsageInfo? {
        val descriptor = bindingContext[BindingContext.REFERENCE_TARGET, refExpr]?.getImportableDescriptor() ?: return null

        val declaration = DescriptorToSourceUtilsIde.getAnyDeclaration(getProject(), descriptor) ?: return null
        if (isAncestor(declaration, false)) return null

        val isCallable = descriptor is CallableDescriptor
        val isExtension = isCallable && (descriptor as CallableDescriptor).getExtensionReceiverParameter() != null

        if (isCallable && !isExtension) {
            val containingDescriptor = descriptor.getContainingDeclaration()
            val receiver = refExpr.getReceiverExpression()
            if (receiver != null) {
                val receiverRef = receiver.getQualifiedElementSelector() as? JetSimpleNameExpression ?: return null
                if (bindingContext[BindingContext.QUALIFIER, receiverRef] == null) return null
                if (descriptor is CallableMemberDescriptor && descriptor.getKind() == Kind.SYNTHESIZED) {
                    return processReference(receiverRef, bindingContext)
                }
            }
            else {
                if (containingDescriptor !is PackageFragmentDescriptor) return null
            }
        }

        val fqName = DescriptorUtils.getFqName(descriptor)
        if (!fqName.isSafe()) return null

        val packageName = DescriptorUtils.getParentOfType(descriptor, javaClass<PackageFragmentDescriptor>(), false)?.let {
            DescriptorUtils.getFqName(it).toSafe()
        }

        return when {
            isExtension,
            packageName == packageNameInfo.oldPackageName,
            packageName == packageNameInfo.newPackageName,
            isImported(descriptor) -> {
                (refExpr.getReference() as? JetSimpleNameReference)?.let { createMoveUsageInfoIfPossible(it, declaration, false) }
            }

            else -> null
        }
    }

    val referenceToContext = JetFileReferencesResolver.resolve(file = file, elements = listOf(this))

    val usages = ArrayList<UsageInfo>()
    for ((refExpr, bindingContext) in referenceToContext) {
        if (refExpr !is JetSimpleNameExpression || refExpr.getParent() is JetThisExpression) continue
        if (bindingContext[BindingContext.QUALIFIER, refExpr] != null) continue

        processReference(refExpr, bindingContext)?.let { usages.add(it) }
    }

    return usages
}

class MoveRenameUsageInfoForExtension(
        element: PsiElement,
        reference: PsiReference,
        startOffset: Int,
        endOffset: Int,
        referencedElement: PsiElement,
        val originalFile: PsiFile,
        val addImportToOriginalFile: Boolean
): MoveRenameUsageInfo(element, reference, startOffset, endOffset, referencedElement, false)

fun createMoveUsageInfoIfPossible(
        reference: PsiReference,
        referencedElement: PsiElement,
        addImportToOriginalFile: Boolean
): UsageInfo? {
    val element = reference.getElement()
    if (element.getStrictParentOfType<JetSuperExpression>() != null) return null

    val range = reference.getRangeInElement()!!
    val startOffset = range.getStartOffset()
    val endOffset = range.getEndOffset()

    if (reference is JetReference
        && referencedElement.namedUnwrappedElement!!.isExtensionDeclaration()
        && element.getNonStrictParentOfType<JetImportDirective>() == null) {
        return MoveRenameUsageInfoForExtension(
                element, reference, startOffset, endOffset, referencedElement, element.getContainingFile()!!, addImportToOriginalFile
        )
    }
    return MoveRenameUsageInfo(element, reference, startOffset, endOffset, referencedElement, false)
}

public fun JetNamedDeclaration.getFileNameAfterMove(): String? {
    return (getContainingFile() as? JetFile)?.let { file ->
        if (file.getDeclarations().size() > 1) "${getName()}.${JetFileType.EXTENSION}" else file.getName()
    }
}

/**
 * Perform usage postprocessing and return non-code usages
 */
fun postProcessMoveUsages(usages: List<UsageInfo>,
                          oldToNewElementsMapping: Map<PsiElement, PsiElement> = Collections.emptyMap(),
                          shorteningMode: ShorteningMode = ShorteningMode.DELAYED_SHORTENING
): List<NonCodeUsageInfo> {
    fun counterpart(e: PsiElement) = oldToNewElementsMapping[e] ?: e

    val sortedUsages = usages.sortBy(
            object : Comparator<UsageInfo> {
                override fun compare(o1: UsageInfo, o2: UsageInfo): Int {
                    val file1 = o1.getVirtualFile()
                    val file2 = o2.getVirtualFile()
                    if (Comparing.equal<VirtualFile>(file1, file2)) {
                        val rangeInElement1 = o1.getRangeInElement()
                        val rangeInElement2 = o2.getRangeInElement()
                        if (rangeInElement1 != null && rangeInElement2 != null) {
                            return rangeInElement2.getStartOffset() - rangeInElement1.getStartOffset()
                        }
                        return 0
                    }
                    if (file1 == null) return -1
                    if (file2 == null) return 1
                    return Comparing.compare<String>(file1.getPath(), file2.getPath())
                }
            }
    )

    val nonCodeUsages = ArrayList<NonCodeUsageInfo>()

    for (usage in sortedUsages) {
        when (usage) {
            is NonCodeUsageInfo -> {
                nonCodeUsages.add(usage)
            }

            is MoveRenameUsageInfoForExtension -> {
                val file = with(usage) { if (addImportToOriginalFile) originalFile else counterpart(originalFile) } as JetFile
                val declaration = counterpart(usage.getReferencedElement()!!).unwrapped as JetDeclaration
                ImportInsertHelper.getInstance(usage.getProject()).importDescriptor(file, declaration.resolveToDescriptor())
            }

            is MoveRenameUsageInfo -> {
                val moveRenameUsage = usage as MoveRenameUsageInfo
                val oldElement = moveRenameUsage.getReferencedElement()!!
                val newElement = counterpart(oldElement)
                moveRenameUsage.getReference()?.let {
                    try {
                        if (it is JetSimpleNameReference) {
                            it.bindToElement(newElement, shorteningMode)
                        }
                        else {
                            it.bindToElement(newElement)
                        }
                    }
                    catch (e: IncorrectOperationException) {
                        // Suppress exception if bindToElement is not implemented
                    }
                }
            }
        }
    }

    return nonCodeUsages
}
