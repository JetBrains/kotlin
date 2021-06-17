/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.IntentionWrapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.psi.CREATE_BY_PATTERN_MAY_NOT_REFORMAT
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.containsInside
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf

@Suppress("EqualsOrHashCode")
abstract class SelfTargetingIntention<TElement : PsiElement>(
    val elementType: Class<TElement>,
    @Nls private var textGetter: () -> String,
    @Nls private var familyNameGetter: () -> String = textGetter,
) : IntentionAction {
    @Deprecated("Replace with primary constructor", ReplaceWith("SelfTargetingIntention<TElement>(elementType, { text }, { familyName })"))
    constructor(
        elementType: Class<TElement>,
        @Nls text: String,
        @Nls familyName: String = text,
    ) : this(elementType, { text }, { familyName })

    protected val defaultText: String get() = defaultTextGetter()
    protected val defaultTextGetter: () -> String = textGetter

    @Deprecated("Replace with `setTextGetter`", ReplaceWith("setTextGetter { text }"))
    protected fun setText(@Nls text: String) {
        this.textGetter = { text }
    }

    protected fun setTextGetter(@Nls textGetter: () -> String) {
        this.textGetter = textGetter
    }

    protected fun setFamilyNameGetter(@Nls familyNameGetter: () -> String) {
        this.familyNameGetter = familyNameGetter
    }


    final override fun getText() = textGetter()
    final override fun getFamilyName() = familyNameGetter()

    abstract fun isApplicableTo(element: TElement, caretOffset: Int): Boolean

    abstract fun applyTo(element: TElement, editor: Editor?)

    open fun applyTo(element: TElement, project: Project, editor: Editor?) {
        applyTo(element, editor)
    }

    fun getTarget(offset: Int, file: PsiFile): TElement? {
        val leaf1 = file.findElementAt(offset)
        val leaf2 = file.findElementAt(offset - 1)
        val commonParent = if (leaf1 != null && leaf2 != null) PsiTreeUtil.findCommonParent(leaf1, leaf2) else null

        var elementsToCheck: Sequence<PsiElement> = emptySequence()
        if (leaf1 != null) elementsToCheck += leaf1.parentsWithSelf.takeWhile { it != commonParent }
        if (leaf2 != null) elementsToCheck += leaf2.parentsWithSelf.takeWhile { it != commonParent }
        if (commonParent != null && commonParent !is PsiFile) elementsToCheck += commonParent.parentsWithSelf

        for (element in elementsToCheck) {
            @Suppress("UNCHECKED_CAST")
            if (elementType.isInstance(element) && isApplicableTo(element as TElement, offset)) {
                return element
            }
            if (element.textRange.containsInside(offset) && !allowCaretInsideElement(element)) break
        }
        return null
    }

    fun getTarget(editor: Editor, file: PsiFile): TElement? {
        val offset = editor.caretModel.offset
        return getTarget(offset, file)
    }

    /** Whether to keep looking for targets after having processed the given element, which contains the cursor. */
    protected open fun allowCaretInsideElement(element: PsiElement): Boolean = element !is KtBlockExpression

    final override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            CREATE_BY_PATTERN_MAY_NOT_REFORMAT = true
        }
        try {
            return getTarget(editor, file) != null
        } finally {
            CREATE_BY_PATTERN_MAY_NOT_REFORMAT = false
        }
    }

    var inspection: IntentionBasedInspection<TElement>? = null
        internal set

    final override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        editor ?: return
        val target = getTarget(editor, file) ?: return
        if (!preparePsiElementForWriteIfNeeded(target)) return
        applyTo(target, project, editor)
    }

    /**
     * If [startInWriteAction] returns true, that means that the platform already called `preparePsiElementForWrite`
     * for us (we do not want to call it again because it will throw if the intention is used with Intention Preview).
     *
     * Otherwise we have to call it ourselves (see javadoc for [getElementToMakeWritable]).
     */
    private fun preparePsiElementForWriteIfNeeded(target: TElement): Boolean {
        if (startInWriteAction()) return true
        return FileModificationService.getInstance().preparePsiElementForWrite(target)
    }

    override fun startInWriteAction() = true

    override fun toString(): String = text

    override fun equals(other: Any?): Boolean {
        // Nasty code because IntentionWrapper itself does not override equals
        if (other is IntentionWrapper) return this == other.action
        if (other is IntentionBasedInspection<*>.IntentionBasedQuickFix) return this == other.intention
        return other is SelfTargetingIntention<*> && javaClass == other.javaClass && text == other.text
    }

    // Intentionally missed hashCode (IntentionWrapper does not override it)
}

abstract class SelfTargetingRangeIntention<TElement : PsiElement>(
    elementType: Class<TElement>,
    @Nls textGetter: () -> String,
    @Nls familyNameGetter: () -> String = textGetter,
) : SelfTargetingIntention<TElement>(elementType, textGetter, familyNameGetter) {

    @Deprecated(
        "Replace with primary constructor",
        ReplaceWith("SelfTargetingRangeIntention<TElement>(elementType, { text }, { familyName })")
    )
    constructor(
        elementType: Class<TElement>,
        @Nls text: String,
        @Nls familyName: String = text,
    ) : this(elementType, { text }, { familyName })

    abstract fun applicabilityRange(element: TElement): TextRange?

    final override fun isApplicableTo(element: TElement, caretOffset: Int): Boolean {
        val range = applicabilityRange(element) ?: return false
        return range.containsOffset(caretOffset)
    }
}

abstract class SelfTargetingOffsetIndependentIntention<TElement : KtElement>(
    elementType: Class<TElement>,
    @Nls textGetter: () -> String,
    @Nls familyNameGetter: () -> String = textGetter,
) : SelfTargetingRangeIntention<TElement>(elementType, textGetter, familyNameGetter) {

    @Deprecated(
        "Replace with primary constructor",
        ReplaceWith("SelfTargetingOffsetIndependentIntention<TElement>(elementType, { text }, { familyName })")
    )
    constructor(
        elementType: Class<TElement>,
        @Nls text: String,
        @Nls familyName: String = text,
    ) : this(elementType, { text }, { familyName })

    abstract fun isApplicableTo(element: TElement): Boolean

    final override fun applicabilityRange(element: TElement): TextRange? {
        return if (isApplicableTo(element)) element.textRange else null
    }
}
