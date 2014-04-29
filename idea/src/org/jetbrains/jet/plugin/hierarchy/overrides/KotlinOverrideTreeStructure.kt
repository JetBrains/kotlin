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

package org.jetbrains.jet.plugin.hierarchy.overrides

import com.intellij.ide.hierarchy.HierarchyTreeStructure
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetElement
import com.intellij.ide.hierarchy.method.MethodHierarchyNodeDescriptor
import com.intellij.ide.hierarchy.method.MethodHierarchyTreeStructure
import com.intellij.psi.PsiMethod
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.resolve.java.jetAsJava.KotlinLightMethod
import org.jetbrains.jet.plugin.search.declarationsSearch.HierarchySearchRequest
import com.intellij.psi.search.GlobalSearchScopes
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.plugin.search.declarationsSearch.searchOverriders
import org.jetbrains.jet.lang.psi.JetDeclaration
import com.intellij.util.ArrayUtil
import org.jetbrains.jet.lang.psi.JetClassOrObject
import com.intellij.psi.PsiClass
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.asJava.toLightMethods
import com.siyeh.ig.psiutils.CollectionUtils
import com.intellij.util.containers.ContainerUtil
import java.util.HashSet

class KotlinOverrideTreeStructure(project: Project, val element: PsiElement) : HierarchyTreeStructure(project, null) {
    val javaTreeStructures = element.toLightMethods().map { method -> MethodHierarchyTreeStructure(project, method) };

    {
        setBaseElement(javaTreeStructures.first!!.getBaseDescriptor()!!)
    }

    override fun buildChildren(descriptor: HierarchyNodeDescriptor): Array<Any> {
        fun buildChildrenByTreeStructure(javaTreeStructure: MethodHierarchyTreeStructure): Array<Any> {
            return javaTreeStructure.getChildElements(descriptor as MethodHierarchyNodeDescriptor) ?: ArrayUtil.EMPTY_OBJECT_ARRAY
        }

        return javaTreeStructures
                .stream()
                .map (::buildChildrenByTreeStructure)
                .reduce { (a, b) -> ContainerUtil.union(a.toSet(), b.toSet()).copyToArray() }
    }
}