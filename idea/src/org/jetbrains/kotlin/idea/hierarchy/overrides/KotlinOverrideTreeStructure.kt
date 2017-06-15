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

import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.ide.hierarchy.method.MethodHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.method.MethodHierarchyTreeStructure
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.asJava.toLightMethods
import com.intellij.util.containers.ContainerUtil

class KotlinOverrideTreeStructure(project: Project, val element: PsiElement) : HierarchyTreeStructure(project, null) {
    val javaTreeStructures = element.toLightMethods().map { method -> MethodHierarchyTreeStructure(project, method) }

    init {
        setBaseElement(javaTreeStructures.first().baseDescriptor!!)
    }

    override fun buildChildren(nodeDescriptor: HierarchyNodeDescriptor): Array<Any> {
        fun buildChildrenByTreeStructure(javaTreeStructure: MethodHierarchyTreeStructure): Array<Any> {
            return javaTreeStructure.getChildElements(nodeDescriptor as MethodHierarchyNodeDescriptor) ?: ArrayUtil.EMPTY_OBJECT_ARRAY
        }

        return javaTreeStructures
                .asSequence()
                .map (::buildChildrenByTreeStructure)
                .reduce { a, b -> ContainerUtil.union(a.toSet(), b.toSet()).toTypedArray() }
    }
}
