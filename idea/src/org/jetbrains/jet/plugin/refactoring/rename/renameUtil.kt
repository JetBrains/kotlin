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

package org.jetbrains.jet.plugin.refactoring.rename

import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import java.util.ArrayList
import com.intellij.refactoring.util.MoveRenameUsageInfo
import org.jetbrains.jet.plugin.references.JetMultiDeclarationReference
import org.jetbrains.jet.lang.psi.psiUtil.getParentByTypeAndBranch
import org.jetbrains.jet.lang.psi.JetWhenConditionInRange
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle
import org.jetbrains.jet.plugin.references.JetForLoopInReference
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.plugin.references.AbstractJetReference

fun checkConflictsAndReplaceUsageInfos(result: MutableList<UsageInfo>) {
    val usagesToAdd = ArrayList<UsageInfo>()
    val usagesToRemove = ArrayList<UsageInfo>()

    for (usageInfo in result) {
        val ref = usageInfo.getReference()
        if (usageInfo !is MoveRenameUsageInfo || ref !is AbstractJetReference<*> || ref.canRename()) continue

        val refElement = usageInfo.getElement()
        val referencedElement = usageInfo.getReferencedElement()
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
    override fun getDescription(): String = JetRefactoringBundle.message("naming.convention.will.be.violated.after.rename")
}