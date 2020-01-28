/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

fun isCallHierarchyElement(e: PsiElement): Boolean {
    return (e is KtNamedFunction && e.name != null) ||
            e is KtSecondaryConstructor ||
            (e is KtProperty && !e.isLocal) ||
            e is KtObjectDeclaration ||
            (e is KtClass && !e.isInterface()) ||
            e is KtFile
}

fun getCallHierarchyElement(element: PsiElement) = element.parentsWithSelf.firstOrNull(::isCallHierarchyElement) as? KtElement

private fun NodeDescriptor<*>.incrementUsageCount() {
    when (this) {
        is KotlinCallHierarchyNodeDescriptor -> incrementUsageCount()
        is CallHierarchyNodeDescriptor -> incrementUsageCount()
    }
}

private fun NodeDescriptor<*>.addReference(reference: PsiReference) {
    when (this) {
        is KotlinCallHierarchyNodeDescriptor -> addReference(reference)
        is CallHierarchyNodeDescriptor -> addReference(reference)
    }
}

internal fun getOrCreateNodeDescriptor(
    parent: HierarchyNodeDescriptor,
    originalElement: PsiElement,
    reference: PsiReference?,
    navigateToReference: Boolean,
    elementToDescriptorMap: MutableMap<PsiElement, NodeDescriptor<*>>,
    isJavaMap: Boolean
): HierarchyNodeDescriptor? {
    val element = (if (isJavaMap && originalElement is KtElement) originalElement.toLightElements().firstOrNull() else originalElement)
        ?: return null

    val existingDescriptor = elementToDescriptorMap[element] as? HierarchyNodeDescriptor
    val result = if (existingDescriptor != null) {
        existingDescriptor.incrementUsageCount()
        existingDescriptor
    } else {
        val newDescriptor: HierarchyNodeDescriptor = when (element) {
            is KtElement -> KotlinCallHierarchyNodeDescriptor(parent, element, false, navigateToReference)
            is PsiMember -> CallHierarchyNodeDescriptor(element.project, parent, element, false, navigateToReference)
            else -> return null
        }
        elementToDescriptorMap[element] = newDescriptor
        newDescriptor
    }

    if (reference != null) {
        result.addReference(reference)
    }

    return result
}