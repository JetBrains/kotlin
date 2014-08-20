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

package org.jetbrains.jet.lang.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ReadOnly
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import java.util.Collections

public trait Diagnostics : Iterable<Diagnostic> {
    public fun all(): Collection<Diagnostic>

    public fun forElement(psiElement: PsiElement): Collection<Diagnostic>

    public fun isEmpty(): Boolean

    public fun noSuppression(): Diagnostics

    class object {

        public val EMPTY: Diagnostics = object : Diagnostics {
            override fun all() = listOf<Diagnostic>()
            override fun forElement(psiElement: PsiElement) = listOf<Diagnostic>()
            override fun isEmpty() = true
            override fun noSuppression() = this
            override fun iterator() = all().iterator()
        }
    }
}
