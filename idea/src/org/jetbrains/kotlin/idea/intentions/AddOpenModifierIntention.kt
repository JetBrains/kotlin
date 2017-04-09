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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddOpenModifierIntention : SelfTargetingIntention<KtDeclaration>(KtDeclaration::class.java, "Make open"), LowPriorityAction {
    override fun isApplicableTo(element: KtDeclaration, caretOffset: Int): Boolean {
        if (element.hasModifier(KtTokens.OPEN_KEYWORD) || element.hasModifier(KtTokens.PRIVATE_KEYWORD)) return false
        when (element) {
            is KtProperty -> if (element.isLocal) return false
            is KtFunction -> if (element.isLocal) return false
        }
        val ktClass = element.getNonStrictParentOfType<KtClass>() ?: return false
        return ktClass.hasModifier(KtTokens.OPEN_KEYWORD)
    }

    override fun applyTo(element: KtDeclaration, editor: Editor?) {
        element.addModifier(KtTokens.OPEN_KEYWORD)
    }
}