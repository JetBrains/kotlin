/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringBundle
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.usageView.UsageViewUtil

internal class MoveFilesWithDeclarationsViewDescriptor(
        private val myElementsToMove: Array<PsiElement>,
        newParent: PsiDirectory
) : UsageViewDescriptor {
    private var myProcessedElementsHeader: String? = null
    private val myCodeReferencesText: String

    init {
        if (myElementsToMove.size == 1) {
            myProcessedElementsHeader = RefactoringBundle.message("move.single.element.elements.header",
                                                                  UsageViewUtil.getType(myElementsToMove[0]),
                                                                  newParent.virtualFile.presentableUrl).capitalize()
            myCodeReferencesText = "References in code to ${UsageViewUtil.getType(myElementsToMove[0])} ${UsageViewUtil.getLongName(myElementsToMove[0])} and its declarations"
        }
        else {
            myProcessedElementsHeader = StringUtil.capitalize(RefactoringBundle.message("move.files.elements.header", newParent.virtualFile.presentableUrl))
            myCodeReferencesText = RefactoringBundle.message("references.found.in.code")
        }
    }

    override fun getElements() = myElementsToMove

    override fun getProcessedElementsHeader() = myProcessedElementsHeader

    override fun getCodeReferencesText(usagesCount: Int, filesCount: Int): String {
        return myCodeReferencesText + UsageViewBundle.getReferencesString(usagesCount, filesCount)
    }

    override fun getCommentReferencesText(usagesCount: Int, filesCount: Int): String? {
        return RefactoringBundle.message("comments.elements.header", UsageViewBundle.getOccurencesString(usagesCount, filesCount))
    }
}