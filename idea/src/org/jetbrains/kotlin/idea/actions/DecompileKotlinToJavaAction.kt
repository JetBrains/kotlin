/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions

import com.intellij.codeInsight.AttachSourcesProvider
import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.util.ActionCallback
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.internal.showDecompiledCode
import org.jetbrains.kotlin.idea.util.isRunningInCidrIde
import org.jetbrains.kotlin.psi.KtFile

class DecompileKotlinToJavaAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val binaryFile = getBinaryKotlinFile(e) ?: return

        showDecompiledCode(binaryFile)
    }

    override fun update(e: AnActionEvent) {
        if (isRunningInCidrIde) {
            e.presentation.isEnabledAndVisible = false
        } else {
            e.presentation.isEnabled = getBinaryKotlinFile(e) != null
        }
    }

    private fun getBinaryKotlinFile(e: AnActionEvent): KtFile? {
        val file = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return null
        if (!file.canBeDecompiledToJava()) return null

        return file
    }

}

fun KtFile.canBeDecompiledToJava() = isCompiled && virtualFile?.fileType == JavaClassFileType.INSTANCE

// Add action to "Attach sources" notification panel
class DecompileKotlinToJavaActionProvider : AttachSourcesProvider {
    override fun getActions(
        orderEntries: MutableList<LibraryOrderEntry>,
        psiFile: PsiFile
    ): Collection<AttachSourcesProvider.AttachSourcesAction> {
        if (psiFile !is KtFile || !psiFile.canBeDecompiledToJava()) return emptyList()

        return listOf(object : AttachSourcesProvider.AttachSourcesAction {
            override fun getName() = "Decompile to Java"

            override fun perform(orderEntriesContainingFile: List<LibraryOrderEntry>?): ActionCallback {
                showDecompiledCode(psiFile)
                return ActionCallback.DONE
            }

            override fun getBusyText() = "Kotlin Classfile"
        })
    }
}
