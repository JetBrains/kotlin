/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.util

import com.intellij.psi.filters.position.PositionElementFilter
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

public class FirstChildInParentFilter(val level: Int = 1) : PositionElementFilter() {
    override fun isAcceptable(element: Any?, context: PsiElement?): Boolean {
        if (element !is PsiElement) return false

        var parent: PsiElement? = element
        for (i in 1..level) {
            if (parent == null) break
            parent = parent?.getContext()
        }

        return (parent != null) && PsiTreeUtil.isAncestor(parent?.getFirstChild(), element, true)
    }


    override fun toString(): String {
        return "firstChildInParent($level)"
    }
}

