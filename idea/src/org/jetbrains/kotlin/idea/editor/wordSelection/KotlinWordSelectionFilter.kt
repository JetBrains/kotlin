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
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.psi.KtContainerNode
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes

public class KotlinWordSelectionFilter : Condition<PsiElement>{
    override fun value(e: PsiElement): Boolean {
        if (e.getLanguage() != KotlinLanguage.INSTANCE) return true

        if (KotlinListSelectioner.canSelect(e)) return false
        if (e is KtContainerNode) return false
        if (e.getParent().getFirstChild().getNextSibling() == null && e.getParent() !is KtContainerNode) return false // skip nodes with the same range as their parent

        return when (e.getNode().getElementType()) {
            BLOCK, KDocElementTypes.KDOC_SECTION -> false
            else -> true
        }
    }
}
