/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.find.findUsages.JavaFindUsagesOptions
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.unsafeResolveToDescriptor
import org.jetbrains.kotlin.idea.codeInsight.DescriptorToSourceUtilsIde
import org.jetbrains.kotlin.idea.core.getDeepestSuperDeclarations
import org.jetbrains.kotlin.idea.findUsages.KotlinClassFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinFunctionFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.KotlinPropertyFindUsagesOptions
import org.jetbrains.kotlin.idea.findUsages.processAllUsages
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class KotlinCallerTreeStructure(
    element: KtElement,
    private val scopeType: String
) : HierarchyTreeStructure(element.project, KotlinCallHierarchyNodeDescriptor(null, element, true, false)) {
    companion object {
        internal fun processReference(
            reference: PsiReference?,
            refElement: PsiElement,
            nodeDescriptor: HierarchyNodeDescriptor,
            callerToDescriptorMap: MutableMap<PsiElement, NodeDescriptor<*>>,
            isJavaMap: Boolean
        ) {
            var callerElement: PsiElement? = when (refElement) {
                is KtElement -> getCallHierarchyElement(refElement)
                else -> {
                    val psiMember = refElement.getNonStrictParentOfType<PsiMember>()
                    psiMember as? PsiMethod ?: psiMember?.containingClass
                }
            }
            if (callerElement is KtProperty) {
                if (PsiTreeUtil.isAncestor(callerElement.initializer, refElement, false)) {
                    callerElement = getCallHierarchyElement(callerElement.parent)
                }
            }
            if (callerElement == null) return

            getOrCreateNodeDescriptor(nodeDescriptor, callerElement, reference, true, callerToDescriptorMap, isJavaMap)
        }
    }

    private fun buildChildren(
        element: PsiElement,
        nodeDescriptor: HierarchyNodeDescriptor,
        callerToDescriptorMap: MutableMap<PsiElement, NodeDescriptor<*>>
    ): Collection<Any> {
        if (nodeDescriptor is CallHierarchyNodeDescriptor) {
            val psiMethod = nodeDescriptor.enclosingElement as? PsiMethod ?: return emptyList()
            return createCallerMethodsTreeStructure(myProject, psiMethod, scopeType).getChildElements(nodeDescriptor).toList()
        }

        if (element !is KtDeclaration) return emptyList()

        val baseClass = (element as? KtDeclaration)?.containingClassOrObject
        val searchScope = getSearchScope(scopeType, baseClass)

        val findOptions: JavaFindUsagesOptions = when (element) {
            is KtNamedFunction, is KtConstructor<*> -> KotlinFunctionFindUsagesOptions(myProject)
            is KtProperty -> KotlinPropertyFindUsagesOptions(myProject)
            is KtPropertyAccessor -> KotlinPropertyFindUsagesOptions(myProject).apply {
                isReadAccess = element.isGetter
                isWriteAccess = element.isSetter
            }
            is KtClass -> KotlinClassFindUsagesOptions(myProject).apply {
                isUsages = false
            }
            else -> return emptyList()
        }
        findOptions.isSkipImportStatements = true
        findOptions.searchScope = searchScope
        findOptions.isSearchForTextOccurrences = false

        val elementToSearch = when (element) {
            is KtPropertyAccessor -> element.property
            else -> element
        }

        // If reference belongs to property initializer, show enclosing declaration instead
        elementToSearch.processAllUsages(findOptions) {
            processReference(it.reference, it.element ?: return@processAllUsages, nodeDescriptor, callerToDescriptorMap, false)
        }

        return callerToDescriptorMap.values
    }

    override fun buildChildren(nodeDescriptor: HierarchyNodeDescriptor): Array<Any> {
        val element = nodeDescriptor.psiElement as? KtDeclaration ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val callerToDescriptorMap = hashMapOf<PsiElement, NodeDescriptor<*>>()
        val descriptor = element.unsafeResolveToDescriptor()
        if (descriptor is CallableMemberDescriptor) {
            return descriptor.getDeepestSuperDeclarations().flatMap { rootDescriptor ->
                val rootElement = DescriptorToSourceUtilsIde.getAnyDeclaration(myProject, rootDescriptor)
                    ?: return@flatMap emptyList<Any>()
                val rootNodeDescriptor = when (rootElement) {
                    is KtElement -> nodeDescriptor
                    is PsiMethod -> CallHierarchyNodeDescriptor(
                        myProject,
                        nodeDescriptor.parentDescriptor as HierarchyNodeDescriptor?,
                        rootElement,
                        nodeDescriptor.parentDescriptor == null,
                        false
                    )
                    else -> return@flatMap emptyList<Any>()
                }
                buildChildren(rootElement, rootNodeDescriptor, callerToDescriptorMap)
            }.toTypedArray()
        } else {
            return buildChildren(element, nodeDescriptor, callerToDescriptorMap).toTypedArray()
        }
    }

    override fun isAlwaysShowPlus() = true
}