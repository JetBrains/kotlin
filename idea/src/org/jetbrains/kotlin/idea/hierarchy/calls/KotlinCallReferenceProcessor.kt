/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.ide.hierarchy.call.CallReferenceProcessor
import com.intellij.ide.hierarchy.call.JavaCallHierarchyData
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.references.KtReference

class KotlinCallReferenceProcessor : CallReferenceProcessor {
    override fun process(reference: PsiReference, data: JavaCallHierarchyData): Boolean {
        if (reference !is KtReference) return true
        val nodeDescriptor = data.nodeDescriptor as? HierarchyNodeDescriptor ?: return true
        @Suppress("UNCHECKED_CAST")
        return !KotlinCallerMethodsTreeStructure.defaultQueryProcessor(
                if (nodeDescriptor is KotlinCallHierarchyNodeDescriptor) nodeDescriptor.javaDelegate else nodeDescriptor,
                data.resultMap as MutableMap<PsiElement, HierarchyNodeDescriptor>,
                false,
                true
        ).process(reference)
    }
}
