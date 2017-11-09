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

package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.asJava.toLightElements
import org.jetbrains.kotlin.asJava.toLightMethods
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
    }
    else {
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