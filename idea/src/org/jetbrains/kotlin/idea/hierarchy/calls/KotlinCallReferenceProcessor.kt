/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.hierarchy.calls

import com.intellij.ide.hierarchy.call.CallReferenceProcessor
import com.intellij.ide.hierarchy.call.JavaCallHierarchyData
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

class KotlinCallReferenceProcessor : CallReferenceProcessor {
    override fun process(reference: PsiReference, data: JavaCallHierarchyData): Boolean {
        val nodeDescriptor = data.nodeDescriptor as? HierarchyNodeDescriptor ?: return false
        @Suppress("UNCHECKED_CAST")
        KotlinCallerTreeStructure.processReference(
            reference,
            reference.element,
            nodeDescriptor,
            data.resultMap as MutableMap<PsiElement, NodeDescriptor<*>>,
            true
        )
        return true
    }
}
