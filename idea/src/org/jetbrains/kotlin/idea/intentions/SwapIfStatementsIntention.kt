/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.*

class SwapIfStatementsIntention : SelfTargetingIntention<KtIfExpression>(
    KtIfExpression::class.java,
    KotlinBundle.lazyMessage("flip.if.statements")
) {
    override fun isApplicableTo(element: KtIfExpression, caretOffset: Int): Boolean {
        return element.ifKeyword.textRange.containsOffset(caretOffset)
                && element.condition != null
                && element.then != null
                && element.`else`?.takeIf { it !is KtIfExpression } != null
    }

    override fun applyTo(element: KtIfExpression, editor: Editor?) {
        element.replaceWithNewIfExpression { swapBranches() }
    }
}
