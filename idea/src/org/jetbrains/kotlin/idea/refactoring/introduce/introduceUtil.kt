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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.core.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringUtil
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParentContainedIn

fun showErrorHint(project: Project, editor: Editor, message: String, title: String) {
    CodeInsightUtils.showErrorHint(project, editor, message, title, null)
}

fun showErrorHintByKey(project: Project, editor: Editor, messageKey: String, title: String) {
    showErrorHint(project, editor, KotlinRefactoringBundle.message(messageKey), title)
}

fun selectElementsWithTargetSibling(
        operationName: String,
        editor: Editor,
        file: PsiFile,
        getContainers: (elements: List<PsiElement>, commonParent: PsiElement) -> List<PsiElement>,
        continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit
) {
    fun onSelectionComplete(elements: List<PsiElement>, targetContainer: PsiElement) {
        val parent = PsiTreeUtil.findCommonParent(elements)
                     ?: throw AssertionError("Should have at least one parent: ${elements.joinToString("\n")}")

        if (parent == targetContainer) {
            continuation(elements, elements.first())
            return
        }

        val outermostParent = parent.getOutermostParentContainedIn(targetContainer)
        if (outermostParent == null) {
            showErrorHintByKey(file.getProject(), editor, "cannot.refactor.no.container", operationName)
            return
        }

        continuation(elements, outermostParent)
    }

    selectElementsWithTargetParent(operationName, editor, file, getContainers, ::onSelectionComplete)
}

fun selectElementsWithTargetParent(
        operationName: String,
        editor: Editor,
        file: PsiFile,
        getContainers: (elements: List<PsiElement>, commonParent: PsiElement) -> List<PsiElement>,
        continuation: (elements: List<PsiElement>, targetParent: PsiElement) -> Unit
) {
    fun showErrorHintByKey(key: String) {
        showErrorHintByKey(file.getProject(), editor, key, operationName)
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
                { continuation(elements, it) }
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
        KotlinRefactoringUtil.selectExpression(editor, file, false) { expr ->
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

fun PsiElement.findExpressionByCopyableDataAndClearIt(key: Key<Boolean>): KtExpression {
    val result = findDescendantOfType<KtExpression> { it.getCopyableUserData(key) != null }!!
    result.putCopyableUserData(key, null)
    return result
}

fun PsiElement.findElementByCopyableDataAndClearIt(key: Key<Boolean>): PsiElement {
    val result = findDescendantOfType<PsiElement> { it.getCopyableUserData(key) != null }!!
    result.putCopyableUserData(key, null)
    return result
}

fun PsiElement.findExpressionsByCopyableDataAndClearIt(key: Key<Boolean>): List<KtExpression> {
    val results = collectDescendantsOfType<KtExpression> { it.getCopyableUserData(key) != null }
    results.forEach { it.putCopyableUserData(key, null) }
    return results
}
