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

package org.jetbrains.kotlin.idea.hierarchy.overrides

import com.intellij.icons.AllIcons
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.openapi.project.Project
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.search.declarationsSearch.HierarchySearchRequest
import org.jetbrains.kotlin.idea.search.declarationsSearch.searchInheritors
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer

class KotlinOverrideTreeStructure(project: Project, declaration: KtCallableDeclaration) : HierarchyTreeStructure(project, null) {
    init {
        setBaseElement(KotlinOverrideHierarchyNodeDescriptor(null, declaration.containingClassOrObject!!, declaration))
    }

    private val baseElement = declaration.createSmartPointer()

    override fun buildChildren(nodeDescriptor: HierarchyNodeDescriptor): Array<Any> {
        val baseElement = baseElement.element ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val psiElement = nodeDescriptor.psiElement ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val subclasses = HierarchySearchRequest(psiElement, psiElement.useScope, false).searchInheritors().findAll()
        return subclasses
                .mapNotNull {
                    val subclass = it.unwrapped ?: return@mapNotNull null
                    KotlinOverrideHierarchyNodeDescriptor(nodeDescriptor, subclass, baseElement)
                }
                .filter { it.calculateState() != AllIcons.Hierarchy.MethodNotDefined }
                .toTypedArray()
    }
}
