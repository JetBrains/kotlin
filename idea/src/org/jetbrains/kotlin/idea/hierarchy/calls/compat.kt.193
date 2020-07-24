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
fun createCallerMethodsTreeStructure(project: Project, method: PsiMethod, scopeType: String): CallerMethodsTreeStructure {
    return CallerMethodsTreeStructure(project, method, scopeType)
}

// BUNCH: 193
fun createCalleeMethodsTreeStructure(project: Project, method: PsiMethod, scopeType: String): CalleeMethodsTreeStructure {
    return CalleeMethodsTreeStructure(project, method, scopeType)
}
