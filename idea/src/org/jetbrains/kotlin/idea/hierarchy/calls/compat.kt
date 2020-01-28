/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION", "DEPRECATION", "UnstableApiUsage")

package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.ide.hierarchy.call.CalleeMethodsTreeStructure
import com.intellij.ide.hierarchy.call.CallerMethodsTreeStructure
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod

// BUNCH: 193
typealias HierarchyNodeDescriptor = com.intellij.ide.hierarchy.HierarchyNodeDescriptor
typealias HierarchyTreeStructure = com.intellij.ide.hierarchy.HierarchyTreeStructure
typealias CallHierarchyBrowserBase = com.intellij.ide.hierarchy.CallHierarchyBrowserBase
typealias HierarchyScopeType = String
typealias HierarchyBrowserBaseEx = com.intellij.ide.hierarchy.HierarchyBrowserBaseEx
typealias MethodHierarchyBrowserBase = com.intellij.ide.hierarchy.MethodHierarchyBrowserBase

fun getCallerTypeCompat(): String = CallHierarchyBrowserBase.CALLER_TYPE
fun getCalleeTypeCompat(): String = CallHierarchyBrowserBase.CALLEE_TYPE
fun getMethodTypeCompat(): String = MethodHierarchyBrowserBase.METHOD_TYPE

fun createCallerMethodsTreeStructure(project: Project, method: PsiMethod, scopeType: String): CallerMethodsTreeStructure {
    return CallerMethodsTreeStructure(project, method, scopeType)
}

fun createCalleeMethodsTreeStructure(project: Project, method: PsiMethod, scopeType: String): CalleeMethodsTreeStructure {
    return CalleeMethodsTreeStructure(project, method, scopeType)
}
