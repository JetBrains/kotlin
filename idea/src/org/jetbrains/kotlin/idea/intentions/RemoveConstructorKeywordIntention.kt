/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class RemoveConstructorKeywordIntention : SelfTargetingIntention<KtPrimaryConstructor>(
    KtPrimaryConstructor::class.java,
    "Remove constructor keyword"
) {
    override fun applyTo(element: KtPrimaryConstructor, editor: Editor?) {
        element.removeRedundantConstructorKeywordAndSpace()
    }

    override fun isApplicableTo(element: KtPrimaryConstructor, caretOffset: Int): Boolean {
        if (element.containingClassOrObject !is KtClass) return false
        if (element.getConstructorKeyword() == null) return false
        return element.modifierList == null
    }
}