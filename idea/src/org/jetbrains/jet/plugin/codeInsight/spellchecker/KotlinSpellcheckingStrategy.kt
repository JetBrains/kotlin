/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin

import com.intellij.spellchecker.tokenizer.SuppressibleSpellcheckingStrategy
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.tokenizer.Tokenizer
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.PsiIdentifierOwnerTokenizer
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiComment
import com.intellij.codeInspection.SuppressIntentionAction

class KotlinSpellcheckingStrategy: SuppressibleSpellcheckingStrategy() {
    private val nameIdentifierTokenizer = PsiIdentifierOwnerTokenizer()

    [suppress("UNCHECKED_CAST")]
    private val emptyTokenizer = SpellcheckingStrategy.EMPTY_TOKENIZER as Tokenizer<out PsiElement?>

    override fun getTokenizer(element: PsiElement?): Tokenizer<out PsiElement?> {
        [suppress("UNCHECKED_CAST")]
        return when {
            element is PsiNameIdentifierOwner || element is PsiComment ->
                super.getTokenizer(element) as Tokenizer<out PsiElement?>

            else ->
                emptyTokenizer
        }
    }

    public override fun isSuppressedFor(element : PsiElement, name : String) : Boolean = false
    public override fun getSuppressActions(element : PsiElement, name : String) : Array<SuppressIntentionAction> = SuppressIntentionAction.EMPTY_ARRAY;
}

