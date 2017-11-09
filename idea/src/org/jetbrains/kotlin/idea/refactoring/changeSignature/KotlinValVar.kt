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

package org.jetbrains.kotlin.idea.refactoring.changeSignature

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPsiFactory

enum class KotlinValVar(val keywordName: String) {
    None("none") {
        override fun createKeyword(factory: KtPsiFactory) = null
    },
    Val("val") {
        override fun createKeyword(factory: KtPsiFactory) = factory.createValKeyword()
    },
    Var("var"){
        override fun createKeyword(factory: KtPsiFactory) = factory.createVarKeyword()
    };

    override fun toString(): String = keywordName

    abstract fun createKeyword(factory: KtPsiFactory): PsiElement?
}

fun PsiElement?.toValVar(): KotlinValVar {
    return when {
        this == null -> KotlinValVar.None
        node.elementType == KtTokens.VAL_KEYWORD -> KotlinValVar.Val
        node.elementType == KtTokens.VAR_KEYWORD -> KotlinValVar.Var
        else -> throw IllegalArgumentException("Unknown val/var token: " + text)
    }
}

fun KtParameter.setValOrVar(valOrVar: KotlinValVar): PsiElement? {
    val newKeyword = valOrVar.createKeyword(KtPsiFactory(this))
    val currentKeyword = valOrVarKeyword

    if (currentKeyword != null) {
        return if (newKeyword == null) {
            currentKeyword.delete()
            null
        }
        else {
            currentKeyword.replace(newKeyword)
        }
    }

    if (newKeyword == null) return null

    nameIdentifier?.let { return addBefore(newKeyword, it) }
    modifierList?.let { return addAfter(newKeyword, it) }
    return addAfter(newKeyword, null)
}