/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.refactoring.move

import org.jetbrains.jet.plugin.codeInsight.JetFileReferencesResolver
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.plugin.references.JetSimpleNameReference
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.JetFileType
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.lang.psi.psiUtil.isAncestor
import java.util.Collections
import org.jetbrains.jet.lang.resolve.name.isImported
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import com.intellij.usageView.UsageInfo
import org.jetbrains.jet.lang.psi.psiUtil.isExtensionDeclaration
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.util.MoveRenameUsageInfo
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.asJava.namedUnwrappedElement
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetImportDirective
import java.util.ArrayList
import com.intellij.refactoring.util.NonCodeUsageInfo
import org.jetbrains.jet.plugin.quickfix.ImportInsertHelper
import org.jetbrains.jet.plugin.refactoring.fqName.getKotlinFqName
import org.jetbrains.jet.lang.psi.JetThisExpression
import org.jetbrains.jet.plugin.references.JetSimpleNameReference.ShorteningMode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.Comparing
import java.util.Comparator
import com.intellij.util.IncorrectOperationException
import com.intellij.psi.PsiFile
import org.jetbrains.jet.lang.resolve.descriptorUtil.getImportableDescriptor
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor.Kind
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector

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

        val declaration = DescriptorToDeclarationUtil.getDeclaration(file, descriptor) ?: return null
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
                (refExpr.getReference() as? JetSimpleNameReference)?.let { createMoveUsageInfo(it, declaration, false) }
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

fun createMoveUsageInfo(
        reference: PsiReference,
        referencedElement: PsiElement,
        addImportToOriginalFile: Boolean
): UsageInfo {
    val range = reference.getRangeInElement()!!
    val element = reference.getElement()
    val startOffset = range.getStartOffset()
    val endOffset = range.getEndOffset()

    if (reference is JetReference
        && referencedElement.namedUnwrappedElement!!.isExtensionDeclaration()
        && element.getParentByType(javaClass<JetImportDirective>()) == null) {
        return MoveRenameUsageInfoForExtension(
                element, reference, startOffset, endOffset, referencedElement, element.getContainingFile()!!, addImportToOriginalFile
        )
    }
    return MoveRenameUsageInfo(element, reference, startOffset, endOffset, referencedElement, false)
}

public fun JetNamedDeclaration.getFileNameAfterMove(): String? {
    return (getContainingFile() as? JetFile)?.let { file ->
        if (file.getDeclarations().size > 1) "${getName()}.${JetFileType.INSTANCE.getDefaultExtension()}" else file.getName()
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
                nonCodeUsages.add(usage as NonCodeUsageInfo)
            }

            is MoveRenameUsageInfoForExtension -> {
                val element = counterpart(usage.getReferencedElement()!!)
                val file = with(usage) { if (addImportToOriginalFile) originalFile else counterpart(originalFile) } as JetFile
                ImportInsertHelper.getInstance().addImportDirectiveIfNeeded(element.getKotlinFqName()!!, file)
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