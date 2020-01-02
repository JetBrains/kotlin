/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.core.implicitModality
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class AddOpenModifierIntention : SelfTargetingIntention<KtCallableDeclaration>(
    KtCallableDeclaration::class.java, "Make open"
), LowPriorityAction {

    override fun isApplicableTo(element: KtCallableDeclaration, caretOffset: Int): Boolean {
        if (element !is KtProperty && element !is KtNamedFunction) {
            return false
        }
        if (element.hasModifier(KtTokens.OPEN_KEYWORD)
            || element.hasModifier(KtTokens.ABSTRACT_KEYWORD)
            || element.hasModifier(KtTokens.PRIVATE_KEYWORD)
        ) {
            return false
        }
        val implicitModality = element.implicitModality()
        if (implicitModality == KtTokens.OPEN_KEYWORD || implicitModality == KtTokens.ABSTRACT_KEYWORD) return false
        val ktClassOrObject = element.containingClassOrObject ?: return false
        return ktClassOrObject.hasModifier(KtTokens.ENUM_KEYWORD)
                || ktClassOrObject.hasModifier(KtTokens.OPEN_KEYWORD)
                || ktClassOrObject.hasModifier(KtTokens.ABSTRACT_KEYWORD)
                || ktClassOrObject.hasModifier(KtTokens.SEALED_KEYWORD)
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        element.addModifier(KtTokens.OPEN_KEYWORD)
    }
}