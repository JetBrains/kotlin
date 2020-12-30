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

package org.jetbrains.kotlin.resolve.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.ModificationTracker
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.GenericDiagnostics

interface Diagnostics : GenericDiagnostics<Diagnostic> {
    //should not be called on readonly views
    //any Diagnostics object returned by BindingContext#getDiagnostics() should implement this property
    val modificationTracker: ModificationTracker
        get() = throw IllegalStateException("Trying to obtain modification tracker for Diagnostics object of class ${this::class.java}")

    override fun all(): Collection<Diagnostic>

    override fun isEmpty(): Boolean = all().isEmpty()

    override fun iterator(): Iterator<Diagnostic> = all().iterator()

    fun forElement(psiElement: PsiElement): Collection<Diagnostic>

    fun noSuppression(): Diagnostics

    companion object {
        val EMPTY: Diagnostics = object : Diagnostics {
            override fun noSuppression(): Diagnostics = this
            override val modificationTracker: ModificationTracker = ModificationTracker.NEVER_CHANGED
            override fun all() = listOf<Diagnostic>()
            override fun forElement(psiElement: PsiElement) = listOf<Diagnostic>()
        }
    }
}
