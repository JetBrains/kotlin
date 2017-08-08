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

import com.google.common.collect.Maps
import com.intellij.find.findUsages.JavaFindUsagesOptions
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
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
) : HierarchyTreeStructure(element.project,
                           KotlinCallHierarchyNodeDescriptor(null, element, true, false)) {
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
            return CallerMethodsTreeStructure(myProject, psiMethod, scopeType).getChildElements(nodeDescriptor).toList()
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
        val callerToDescriptorMap = Maps.newHashMap<PsiElement, NodeDescriptor<*>>()
        val descriptor = element.resolveToDescriptor()
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
        }
        else {
            return buildChildren(element, nodeDescriptor, callerToDescriptorMap).toTypedArray()
        }
    }

    override fun isAlwaysShowPlus() = true
}