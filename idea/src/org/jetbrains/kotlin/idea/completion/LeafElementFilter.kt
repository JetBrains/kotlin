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

package org.jetbrains.kotlin.idea.completion

import com.intellij.psi.PsiElement
import com.intellij.psi.filters.ClassFilter
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.tree.IElementType

public class LeafElementFilter(private val elementType: IElementType) : ElementFilter {

    override fun isAcceptable(element: Any?, context: PsiElement?)
            = element is LeafPsiElement && element.getElementType() == elementType

    override fun isClassAcceptable(hintClass: Class<*>)
            = LEAF_CLASS_FILTER.isClassAcceptable(hintClass)

    default object {
        private val LEAF_CLASS_FILTER = ClassFilter(javaClass<LeafPsiElement>())
    }
}
