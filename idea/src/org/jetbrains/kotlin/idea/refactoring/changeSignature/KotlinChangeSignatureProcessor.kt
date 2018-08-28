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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessorBase
import com.intellij.refactoring.changeSignature.ChangeSignatureUsageProcessor
import com.intellij.refactoring.changeSignature.JavaChangeSignatureUsageProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.refactoring.broadcastRefactoringExit
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinFunctionCallUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinImplicitReceiverUsage
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinUsageInfo
import org.jetbrains.kotlin.idea.refactoring.changeSignature.usages.KotlinWrapperForJavaUsageInfos
import java.util.*

class KotlinChangeSignatureProcessor(
        project: Project,
        changeInfo: KotlinChangeInfo,
        private val commandName: String
) : ChangeSignatureProcessorBase(project, KotlinChangeInfoWrapper(changeInfo)) {
    val ktChangeInfo
        get() = changeInfo.delegate!!

    override fun createUsageViewDescriptor(usages: Array<UsageInfo>): UsageViewDescriptor {
        val subject = if (ktChangeInfo.kind.isConstructor) "constructor" else "function"
        return KotlinUsagesViewDescriptor(myChangeInfo.method, RefactoringBundle.message("0.to.change.signature", subject))
    }

    override fun getChangeInfo() = super.getChangeInfo() as KotlinChangeInfoWrapper

    override fun findUsages(): Array<UsageInfo> {
        val allUsages = ArrayList<UsageInfo>()
        ktChangeInfo.getOrCreateJavaChangeInfos()?.let { javaChangeInfos ->
            val javaProcessor = JavaChangeSignatureUsageProcessor()
            javaChangeInfos.mapTo(allUsages) {
                KotlinWrapperForJavaUsageInfos(it, javaProcessor.findUsages(it), changeInfo.method)
            }
        }
        super.findUsages().filterTo(allUsages) { it is KotlinUsageInfo<*> || it is UnresolvableCollisionUsageInfo }

        return allUsages.toTypedArray()
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val usageProcessors = ChangeSignatureUsageProcessor.EP_NAME.extensions

        if (!usageProcessors.all { it.setupDefaultValues(myChangeInfo, refUsages, myProject) }) return false

        val conflictDescriptions = object: MultiMap<PsiElement, String>() {
            override fun createCollection() = LinkedHashSet<String>()
        }
        usageProcessors.forEach { conflictDescriptions.putAllValues(it.findConflicts(myChangeInfo, refUsages)) }

        val usages = refUsages.get()
        val usagesSet = usages.toHashSet()

        RenameUtil.addConflictDescriptions(usages, conflictDescriptions)
        RenameUtil.removeConflictUsages(usagesSet)
        if (!conflictDescriptions.isEmpty) {
            if (ApplicationManager.getApplication().isUnitTestMode) {
                throw BaseRefactoringProcessor.ConflictsInTestsException(conflictDescriptions.values())
            }

            val dialog = prepareConflictsDialog(conflictDescriptions, usages)
            dialog.show()
            if (!dialog.isOK) {
                if (dialog.isShowConflicts) prepareSuccessful()
                return false
            }
        }

        val usageArray = usagesSet.toTypedArray()
        Arrays.sort(usageArray) { u1, u2 ->
            if (u1 is KotlinImplicitReceiverUsage && u2 is KotlinFunctionCallUsage) return@sort -1
            if (u2 is KotlinImplicitReceiverUsage && u1 is KotlinFunctionCallUsage) return@sort 1
            val element1 = u1.element
            val element2 = u2.element
            val rank1 = element1?.textOffset ?: -1
            val rank2 = element2?.textOffset ?: -1
            rank2 - rank1 // Reverse order
        }
        refUsages.set(usageArray)

        prepareSuccessful()

        return true
    }

    override fun isPreviewUsages(usages: Array<out UsageInfo>): Boolean = isPreviewUsages

    override fun getCommandName() = commandName

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        try {
            super.performRefactoring(usages)
        }
        finally {
            changeInfo.invalidate()
        }
    }

    override fun doRun() {
        try {
            super.doRun()
        } finally {
            broadcastRefactoringExit(myProject, refactoringId!!)
        }
    }
}
