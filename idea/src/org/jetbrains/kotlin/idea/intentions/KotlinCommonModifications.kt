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

import com.intellij.codeInsight.intention.*
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.idea.quickfix.AddModifierFix
import org.jetbrains.kotlin.idea.quickfix.RemoveModifierFix
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.uast.UDeclaration


class KotlinCommonModifications : JvmCommonIntentionActionsFactory() {
    override fun createChangeModifierAction(declaration: UDeclaration, modifier: String, shouldPresent: Boolean): IntentionAction? {
        val kModifierOwner = (declaration.psi as? KtLightElement<*, *>?)?.kotlinOrigin as? KtModifierListOwner?
                             ?: throw IllegalArgumentException("$declaration is expected to contain KtLightElement with KtModifierListOwner")

        val (kToken, shouldPresentMapped) = if (PsiModifier.FINAL == modifier)
            KtTokens.OPEN_KEYWORD to !shouldPresent
        else
            javaPsiModifiersMapping[modifier] to shouldPresent

        if (kToken == null) return null
        return if (shouldPresentMapped)
            AddModifierFix.createIfApplicable(kModifierOwner, kToken)
        else
            RemoveModifierFix(kModifierOwner, kToken, false)
    }

    companion object {
        val javaPsiModifiersMapping = mapOf(
                PsiModifier.PRIVATE to KtTokens.PRIVATE_KEYWORD,
                PsiModifier.PUBLIC to KtTokens.PUBLIC_KEYWORD,
                PsiModifier.PROTECTED to KtTokens.PUBLIC_KEYWORD,
                PsiModifier.ABSTRACT to KtTokens.ABSTRACT_KEYWORD
        )
    }

}