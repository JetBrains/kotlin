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

package org.jetbrains.kotlin.idea.codeInsight.spellchecker

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.Tokenizer
import com.intellij.spellchecker.tokenizer.TokenizerBase
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class KotlinSpellcheckingStrategy : SpellcheckingStrategy() {
    private val plainTextTokenizer = TokenizerBase<KtLiteralStringTemplateEntry>(PlainTextSplitter.getInstance())
    private val emptyTokenizer = EMPTY_TOKENIZER

    override fun getTokenizer(element: PsiElement?): Tokenizer<out PsiElement?> {
        return when (element) {
            is PsiComment -> super.getTokenizer(element)
            is KtParameter -> {
                val function = (element.parent as? KtParameterList)?.parent as? KtNamedFunction
                when {
                    function?.hasModifier(KtTokens.OVERRIDE_KEYWORD) == true -> emptyTokenizer
                    else -> super.getTokenizer(element)
                }
            }
            is PsiNameIdentifierOwner -> {
                when {
                    element is KtModifierListOwner && element.hasModifier(KtTokens.OVERRIDE_KEYWORD) -> emptyTokenizer
                    else -> super.getTokenizer(element)
                }
            }
            is KtLiteralStringTemplateEntry -> plainTextTokenizer
            else -> emptyTokenizer
        }
    }
}
