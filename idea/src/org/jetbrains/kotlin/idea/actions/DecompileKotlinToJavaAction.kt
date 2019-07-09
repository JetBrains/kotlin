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
    override fun getActions(orderEntries: MutableList<LibraryOrderEntry>, psiFile: PsiFile): Collection<AttachSourcesProvider.AttachSourcesAction> {
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
