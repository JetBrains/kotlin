/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION", "DEPRECATION")

package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod

// BUNCH: 193
typealias HierarchyNodeDescriptor = com.intellij.ide.hierarchy.newAPI.HierarchyNodeDescriptor
typealias HierarchyTreeStructure = com.intellij.ide.hierarchy.newAPI.HierarchyTreeStructure
typealias CallHierarchyBrowserBase = com.intellij.ide.hierarchy.newAPI.CallHierarchyBrowserBase
typealias HierarchyScopeType = com.intellij.ide.hierarchy.newAPI.HierarchyScopeType
typealias HierarchyBrowserBaseEx = com.intellij.ide.hierarchy.newAPI.HierarchyBrowserBaseEx
typealias MethodHierarchyBrowserBase = com.intellij.ide.hierarchy.newAPI.MethodHierarchyBrowserBase

fun getCallerTypeCompat() = CallHierarchyBrowserBase.getCallerType()
fun getCalleeTypeCompat() = CallHierarchyBrowserBase.getCalleeType()
fun getMethodTypeCompat() = MethodHierarchyBrowserBase.getMethodType()

fun createCallerMethodsTreeStructure(project: Project, method: PsiMethod, scopeType: String): CallerMethodsTreeStructure {
    return CallerMethodsTreeStructure(project, method as PsiMember, scopeType)
}

fun createCalleeMethodsTreeStructure(project: Project, method: PsiMethod, scopeType: String): CalleeMethodsTreeStructure {
    return CalleeMethodsTreeStructure(project, method as PsiMember, scopeType)
}
