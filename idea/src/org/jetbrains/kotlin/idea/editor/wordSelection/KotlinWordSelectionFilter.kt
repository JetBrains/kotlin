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

package org.jetbrains.kotlin.idea.editor.wordSelection

import com.intellij.openapi.util.Condition
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtNodeTypes.BLOCK
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtContainerNode

class KotlinWordSelectionFilter : Condition<PsiElement> {
    override fun value(e: PsiElement): Boolean {
        if (e.language != KotlinLanguage.INSTANCE) return true

        if (KotlinListSelectioner.canSelect(e)) return false
        if (KotlinCodeBlockSelectioner.canSelect(e)) return false

        val elementType = e.node.elementType
        if (elementType == KtTokens.REGULAR_STRING_PART || elementType == KtTokens.ESCAPE_SEQUENCE) return true

        if (e is KtContainerNode) return false

        return when (e.node.elementType) {
            BLOCK, KDocElementTypes.KDOC_SECTION -> false
            else -> true
        }
    }
}
