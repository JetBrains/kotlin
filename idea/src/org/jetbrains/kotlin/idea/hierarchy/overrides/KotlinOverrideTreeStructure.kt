/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.hierarchy.overrides

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.hierarchy.calls.HierarchyNodeDescriptor
import org.jetbrains.kotlin.idea.hierarchy.calls.HierarchyTreeStructure
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
        return subclasses.mapNotNull {
                val subclass = it.unwrapped ?: return@mapNotNull null
                KotlinOverrideHierarchyNodeDescriptor(nodeDescriptor, subclass, baseElement)
            }
            .filter { it.calculateState() != AllIcons.Hierarchy.MethodNotDefined }
            .toTypedArray()
    }
}
