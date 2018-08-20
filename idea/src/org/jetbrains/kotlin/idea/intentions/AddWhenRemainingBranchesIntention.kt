/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.quickfix.AddWhenRemainingBranchesFix
import org.jetbrains.kotlin.psi.KtWhenExpression

class AddWhenRemainingBranchesIntention : SelfTargetingIntention<KtWhenExpression>(
    KtWhenExpression::class.java, "Add remaining branches"
) {
    override fun isApplicableTo(element: KtWhenExpression, caretOffset: Int): Boolean {
        if (element.entries.none { it.isElse }) return false
        return AddWhenRemainingBranchesFix.isAvailable(element)
    }

    override fun applyTo(element: KtWhenExpression, editor: Editor?) {
        AddWhenRemainingBranchesFix.addRemainingBranches(element)
    }
}