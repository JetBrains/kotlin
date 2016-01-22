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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.references.AbstractKtReference
import java.util.*

fun checkConflictsAndReplaceUsageInfos(result: MutableList<UsageInfo>) {
    val usagesToAdd = ArrayList<UsageInfo>()
    val usagesToRemove = ArrayList<UsageInfo>()

    for (usageInfo in result) {
        val ref = usageInfo.reference
        if (usageInfo !is MoveRenameUsageInfo || ref !is AbstractKtReference<*> || ref.canRename()) continue

        val refElement = usageInfo.element
        val referencedElement = usageInfo.referencedElement
        if (refElement != null && referencedElement != null) {
            usagesToAdd.add(UnresolvableConventionViolationUsageInfo(refElement, referencedElement))
            usagesToRemove.add(usageInfo)
        }
    }

    result.removeAll(usagesToRemove)
    result.addAll(usagesToAdd)
}

class UnresolvableConventionViolationUsageInfo(
        element: PsiElement,
        referencedElement: PsiElement
) : UnresolvableCollisionUsageInfo(element, referencedElement) {
    override fun getDescription(): String = KotlinRefactoringBundle.message("naming.convention.will.be.violated.after.rename")
}
