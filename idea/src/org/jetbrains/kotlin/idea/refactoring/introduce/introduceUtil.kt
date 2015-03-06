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

package org.jetbrains.kotlin.idea.refactoring.introduce

import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.idea.refactoring.*
import com.intellij.openapi.editor.*
import com.intellij.psi.*
import com.intellij.psi.util.*
import org.jetbrains.kotlin.idea.codeInsight.*
import com.intellij.openapi.project.*

fun showErrorHint(project: Project, editor: Editor, message: String, title: String) {
    CodeInsightUtils.showErrorHint(project, editor, message, title, null)
}

fun showErrorHintByKey(project: Project, editor: Editor, messageKey: String, title: String) {
    showErrorHint(project, editor, JetRefactoringBundle.message(messageKey), title)
}

fun selectElements(
        operationName: String,
        editor: Editor,
        file: PsiFile,
        getContainers: (elements: List<PsiElement>, commonParent: PsiElement) -> List<PsiElement>,
        continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit
) {
    fun showErrorHintByKey(key: String) {
        showErrorHintByKey(file.getProject(), editor, key, operationName)
    }

    fun onSelectionComplete(parent: PsiElement, elements: List<PsiElement>, targetContainer: PsiElement) {
        if (parent == targetContainer) {
            continuation(elements, elements.first())
            return
        }

        val outermostParent = parent.getOutermostParentContainedIn(targetContainer)
        if (outermostParent == null) {
            showErrorHintByKey("cannot.refactor.no.container")
            return
        }

        continuation(elements, outermostParent)
    }

    fun selectTargetContainer(elements: List<PsiElement>) {
        val parent = PsiTreeUtil.findCommonParent(elements)
            ?: throw AssertionError("Should have at least one parent: ${elements.joinToString("\n")}")

        val containers = getContainers(elements, parent)
        if (containers.isEmpty()) {
            showErrorHintByKey("cannot.refactor.no.container")
            return
        }

        chooseContainerElementIfNecessary(
                containers,
                editor,
                "Select target code block",
                true,
                { it },
                { onSelectionComplete(parent, elements, it) }
        )
    }

    fun selectMultipleExpressions() {
        val startOffset = editor.getSelectionModel().getSelectionStart()
        val endOffset = editor.getSelectionModel().getSelectionEnd()

        val elements = CodeInsightUtils.findStatements(file, startOffset, endOffset)
        if (elements.isEmpty()) {
            showErrorHintByKey("cannot.refactor.no.expression")
            return
        }

        selectTargetContainer(elements.toList())
    }

    fun selectSingleExpression() {
        JetRefactoringUtil.selectExpression(editor, file, false) { expr ->
            if (expr != null) {
                selectTargetContainer(listOf(expr))
            }
            else {
                if (!editor.getSelectionModel().hasSelection()) {
                    editor.getSelectionModel().selectLineAtCaret()
                }
                selectMultipleExpressions()
            }
        }
    }

    editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE)
    selectSingleExpression()
}