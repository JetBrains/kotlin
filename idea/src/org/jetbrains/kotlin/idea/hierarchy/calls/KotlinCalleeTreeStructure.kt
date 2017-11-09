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
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.call.CallHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.util.ArrayUtil
import com.intellij.util.containers.HashMap
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchOverriders
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class KotlinCalleeTreeStructure(
        element: KtElement,
        private val scopeType: String
) : HierarchyTreeStructure(element.project,
                           KotlinCallHierarchyNodeDescriptor(null, element, true, false)) {
    private fun KtElement.getCalleeSearchScope(): List<KtElement> {
        return when (this) {
            is KtNamedFunction, is KtFunctionLiteral, is KtPropertyAccessor -> listOf((this as KtDeclarationWithBody).bodyExpression)
            is KtProperty -> accessors.map { it.bodyExpression }
            is KtClassOrObject -> {
                superTypeListEntries.filterIsInstance<KtCallElement>() +
                getAnonymousInitializers().map { it.body } +
                declarations.filterIsInstance<KtProperty>().map { it.initializer }
            }
            else -> emptyList()
        }.filterNotNull()
    }

    override fun buildChildren(nodeDescriptor: HierarchyNodeDescriptor): Array<Any> {
        if (nodeDescriptor is CallHierarchyNodeDescriptor) {
            val psiMethod = nodeDescriptor.enclosingElement as? PsiMethod ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
            return CalleeMethodsTreeStructure(myProject, psiMethod, scopeType).getChildElements(nodeDescriptor)
        }

        val element = nodeDescriptor.psiElement as? KtElement ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val result = LinkedHashSet<HierarchyNodeDescriptor>()
        val baseClass = (element as? KtDeclaration)?.containingClassOrObject
        val calleeToDescriptorMap = HashMap<PsiElement, NodeDescriptor<*>>()

        element.getCalleeSearchScope().forEach {
            it.accept(
                    object : CalleeReferenceVisitorBase(it.analyze(), false) {
                        override fun processDeclaration(reference: KtSimpleNameExpression, declaration: PsiElement) {
                            if (!isInScope(baseClass, declaration, scopeType)) return
                            result += (getOrCreateNodeDescriptor(nodeDescriptor, declaration, null, false, calleeToDescriptorMap, false) ?: return)
                        }
                    }
            )
        }

        for (it in HierarchySearchRequest(element, element.useScope).searchOverriders()) {
            val overrider = it.unwrapped as? KtElement ?: continue
            if (!isInScope(baseClass, overrider, scopeType)) continue
            result += KotlinCallHierarchyNodeDescriptor(nodeDescriptor, overrider, false, false)
        }

        return result.toArray()
    }
}