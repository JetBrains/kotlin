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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.core.implicitModality
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class AddOpenModifierIntention : SelfTargetingIntention<KtCallableDeclaration>(KtCallableDeclaration::class.java, "Make open"), LowPriorityAction {

    override fun isApplicableTo(element: KtCallableDeclaration, caretOffset: Int): Boolean {
        if (element.hasModifier(KtTokens.OPEN_KEYWORD)
            || element.hasModifier(KtTokens.ABSTRACT_KEYWORD)
            || element.hasModifier(KtTokens.PRIVATE_KEYWORD)) return false
        if (element is KtDestructuringDeclaration || element is KtParameter) return false
        val implicitModality = element.implicitModality()
        if (implicitModality == KtTokens.OPEN_KEYWORD || implicitModality == KtTokens.ABSTRACT_KEYWORD) return false
        val ktClassOrObject = element.containingClassOrObject ?: return false
        return ktClassOrObject.hasModifier(KtTokens.OPEN_KEYWORD)
               || ktClassOrObject.hasModifier(KtTokens.ABSTRACT_KEYWORD)
               || ktClassOrObject.hasModifier(KtTokens.SEALED_KEYWORD)
    }

    override fun applyTo(element: KtCallableDeclaration, editor: Editor?) {
        element.addModifier(KtTokens.OPEN_KEYWORD)
    }
}