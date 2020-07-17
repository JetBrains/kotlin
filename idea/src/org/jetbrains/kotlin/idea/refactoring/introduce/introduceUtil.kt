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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.chooseContainerElementIfNecessary
import org.jetbrains.kotlin.idea.refactoring.selectElement
import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.SmartList

fun showErrorHint(project: Project, editor: Editor, message: String, title: String) {
    CodeInsightUtils.showErrorHint(project, editor, message, title, null)
}

fun showErrorHintByKey(project: Project, editor: Editor, messageKey: String, title: String) {
    showErrorHint(project, editor, KotlinBundle.message(messageKey), title)
}

fun selectElementsWithTargetSibling(
    operationName: String,
    editor: Editor,
    file: KtFile,
    title: String,
    elementKinds: Collection<CodeInsightUtils.ElementKind>,
    elementValidator: (List<PsiElement>) -> String?,
    getContainers: (elements: List<PsiElement>, commonParent: PsiElement) -> List<PsiElement>,
    continuation: (elements: List<PsiElement>, targetSibling: PsiElement) -> Unit
) {
    fun onSelectionComplete(elements: List<PsiElement>, targetContainer: PsiElement) {
        val physicalElements = elements.map { it.substringContextOrThis }
        val parent = PsiTreeUtil.findCommonParent(physicalElements)
            ?: throw AssertionError("Should have at least one parent: ${physicalElements.joinToString("\n")}")

        if (parent == targetContainer) {
            continuation(elements, physicalElements.first())
            return
        }

        val outermostParent = parent.getOutermostParentContainedIn(targetContainer)
        if (outermostParent == null) {
            showErrorHintByKey(file.project, editor, "cannot.refactor.no.container", operationName)
            return
        }

        continuation(elements, outermostParent)
    }

    selectElementsWithTargetParent(operationName, editor, file, title, elementKinds, elementValidator, getContainers, ::onSelectionComplete)
}

fun selectElementsWithTargetParent(
    operationName: String,
    editor: Editor,
    file: KtFile,
    title: String,
    elementKinds: Collection<CodeInsightUtils.ElementKind>,
    elementValidator: (List<PsiElement>) -> String?,
    getContainers: (elements: List<PsiElement>, commonParent: PsiElement) -> List<PsiElement>,
    continuation: (elements: List<PsiElement>, targetParent: PsiElement) -> Unit
) {
    fun showErrorHintByKey(key: String) {
        showErrorHintByKey(file.project, editor, key, operationName)
    }

    fun selectTargetContainer(elements: List<PsiElement>) {
        elementValidator(elements)?.let {
            showErrorHint(file.project, editor, it, operationName)
            return
        }

        val physicalElements = elements.map { it.substringContextOrThis }
        val parent = PsiTreeUtil.findCommonParent(physicalElements)
            ?: throw AssertionError("Should have at least one parent: ${physicalElements.joinToString("\n")}")

        val containers = getContainers(physicalElements, parent)
        if (containers.isEmpty()) {
            showErrorHintByKey("cannot.refactor.no.container")
            return
        }

        chooseContainerElementIfNecessary(
            containers,
            editor,
            title,
            true,
            { it },
            { continuation(elements, it) }
        )
    }

    fun selectMultipleElements() {
        val startOffset = editor.selectionModel.selectionStart
        val endOffset = editor.selectionModel.selectionEnd

        val elements = elementKinds.flatMap { CodeInsightUtils.findElements(file, startOffset, endOffset, it).toList() }
        if (elements.isEmpty()) {
            return when (elementKinds.singleOrNull()) {
                CodeInsightUtils.ElementKind.EXPRESSION -> showErrorHintByKey("cannot.refactor.no.expression")
                CodeInsightUtils.ElementKind.TYPE_ELEMENT -> showErrorHintByKey("cannot.refactor.no.type")
                else -> showErrorHint(
                    file.project,
                    editor,
                    KotlinBundle.message("text.refactoring.can.t.be.performed.on.the.selected.code.element"),
                    title
                )
            }
        }

        selectTargetContainer(elements)
    }

    fun selectSingleElement() {
        selectElement(editor, file, false, elementKinds) { expr ->
            if (expr != null) {
                selectTargetContainer(listOf(expr))
            } else {
                if (!editor.selectionModel.hasSelection()) {
                    if (elementKinds.singleOrNull() == CodeInsightUtils.ElementKind.EXPRESSION) {
                        val elementAtCaret = file.findElementAt(editor.caretModel.offset)
                        elementAtCaret?.getParentOfTypeAndBranch<KtProperty> { nameIdentifier }?.let {
                            return@selectElement selectTargetContainer(listOf(it))
                        }
                    }

                    editor.selectionModel.selectLineAtCaret()
                }
                selectMultipleElements()
            }
        }
    }

    editor.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
    selectSingleElement()
}

fun PsiElement.findExpressionByCopyableDataAndClearIt(key: Key<Boolean>): KtExpression? {
    val result = findDescendantOfType<KtExpression> { it.getCopyableUserData(key) != null } ?: return null
    result.putCopyableUserData(key, null)
    return result
}

fun PsiElement.findElementByCopyableDataAndClearIt(key: Key<Boolean>): PsiElement? {
    val result = findDescendantOfType<PsiElement> { it.getCopyableUserData(key) != null } ?: return null
    result.putCopyableUserData(key, null)
    return result
}

fun PsiElement.findExpressionsByCopyableDataAndClearIt(key: Key<Boolean>): List<KtExpression> {
    val results = collectDescendantsOfType<KtExpression> { it.getCopyableUserData(key) != null }
    results.forEach { it.putCopyableUserData(key, null) }
    return results
}

fun findExpressionOrStringFragment(file: KtFile, startOffset: Int, endOffset: Int): KtExpression? {
    val entry1 = file.findElementAt(startOffset)?.getNonStrictParentOfType<KtStringTemplateEntry>() ?: return null
    val entry2 = file.findElementAt(endOffset - 1)?.getNonStrictParentOfType<KtStringTemplateEntry>() ?: return null

    if (entry1 == entry2 && entry1 is KtStringTemplateEntryWithExpression) return entry1.expression

    val stringTemplate = entry1.parent as? KtStringTemplateExpression ?: return null
    if (entry2.parent != stringTemplate) return null

    val templateOffset = stringTemplate.startOffset
    if (stringTemplate.getContentRange().equalsToRange(startOffset - templateOffset, endOffset - templateOffset)) return stringTemplate

    val prefixOffset = startOffset - entry1.startOffset
    if (entry1 !is KtLiteralStringTemplateEntry && prefixOffset > 0) return null

    val suffixOffset = endOffset - entry2.startOffset
    if (entry2 !is KtLiteralStringTemplateEntry && suffixOffset < entry2.textLength) return null

    val prefix = entry1.text.substring(0, prefixOffset)
    val suffix = entry2.text.substring(suffixOffset)

    return ExtractableSubstringInfo(entry1, entry2, prefix, suffix).createExpression()
}

fun KotlinPsiRange.getPhysicalTextRange(): TextRange {
    return (elements.singleOrNull() as? KtExpression)?.extractableSubstringInfo?.contentRange ?: getTextRange()
}

fun ExtractableSubstringInfo.replaceWith(replacement: KtExpression): KtExpression {
    return with(this) {
        val psiFactory = KtPsiFactory(replacement)
        val parent = startEntry.parent

        psiFactory.createStringTemplate(prefix).entries.singleOrNull()?.let { parent.addBefore(it, startEntry) }

        val refEntry = psiFactory.createBlockStringTemplateEntry(replacement)
        val addedRefEntry = parent.addBefore(refEntry, startEntry) as KtStringTemplateEntryWithExpression

        psiFactory.createStringTemplate(suffix).entries.singleOrNull()?.let { parent.addAfter(it, endEntry) }

        parent.deleteChildRange(startEntry, endEntry)

        addedRefEntry.expression!!
    }
}

fun KtExpression.mustBeParenthesizedInInitializerPosition(): Boolean {
    if (this !is KtBinaryExpression) return false

    if (left?.mustBeParenthesizedInInitializerPosition() == true) return true
    return PsiChildRange(left, operationReference).any { (it is PsiWhiteSpace) && it.textContains('\n') }
}

fun isObjectOrNonInnerClass(e: PsiElement): Boolean = e is KtObjectDeclaration || (e is KtClass && !e.isInner())

fun <T : KtDeclaration> insertDeclaration(declaration: T, targetSibling: PsiElement): T {
    val targetParent = targetSibling.parent

    val anchorCandidates = SmartList<PsiElement>()
    anchorCandidates.add(targetSibling)
    if (targetSibling is KtEnumEntry) {
        anchorCandidates.add(targetSibling.siblings().last { it is KtEnumEntry })
    }

    val anchor = anchorCandidates.minBy { it.startOffset }!!.parentsWithSelf.first { it.parent == targetParent }
    val targetContainer = anchor.parent!!

    @Suppress("UNCHECKED_CAST")
    return (targetContainer.addBefore(declaration, anchor) as T).apply {
        targetContainer.addBefore(KtPsiFactory(declaration).createWhiteSpace("\n\n"), anchor)
    }
}

internal fun validateExpressionElements(elements: List<PsiElement>): String? {
    if (elements.any { it is KtConstructor<*> || it is KtParameter || it is KtTypeAlias || it is KtPropertyAccessor }) {
        return KotlinBundle.message("text.refactoring.is.not.applicable.to.this.code.fragment")
    }
    return null
}