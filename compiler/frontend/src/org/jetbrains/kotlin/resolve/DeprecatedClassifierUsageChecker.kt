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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStubbedPsiUtil
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry

class DeprecatedClassifierUsageChecker : ClassifierUsageChecker {
    override fun check(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) {
        // Do not check types in annotation entries to prevent cycles in resolve, rely on call message
        val annotationEntry = KtStubbedPsiUtil.getPsiOrStubParent(element, KtAnnotationEntry::class.java, true)
        if (annotationEntry != null && annotationEntry.calleeExpression!!.constructorReferenceExpression == element) return

        // Do not check types in calls to super constructor in extends list, rely on call message
        val superExpression = KtStubbedPsiUtil.getPsiOrStubParent(element, KtSuperTypeCallEntry::class.java, true)
        if (superExpression != null && superExpression.calleeExpression.constructorReferenceExpression == element) return

        val deprecation = targetDescriptor.getDeprecation()
        if (deprecation != null) {
            trace.report(createDeprecationDiagnostic(element, deprecation))
        }
    }
}
