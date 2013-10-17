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

package org.jetbrains.jet.lang.psi.psiUtil

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

fun PsiElement.getParentByTypeAndPredicate<T: PsiElement>(
        parentClass : Class<T>, strict : Boolean = false, predicate: (T) -> Boolean
) : T? {
    var element = if (strict) getParent() else this
    while (element != null) {
        [suppress("UNCHECKED_CAST")]
        when {
            parentClass.isInstance(element) && predicate(element as T) ->
                return element as T
            element is PsiFile ->
                return null
            else ->
                element = element?.getParent()
        }
    }

    return null
}