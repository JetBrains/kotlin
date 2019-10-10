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

import org.jetbrains.kotlin.diagnostics.Diagnostic
import com.intellij.openapi.util.CompositeModificationTracker
import com.intellij.util.CachedValueImpl
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.resolve.BindingContext

class MutableDiagnosticsWithSuppression @JvmOverloads constructor(
    private val bindingContext: BindingContext,
    private val delegateDiagnostics: Diagnostics = Diagnostics.EMPTY
) : Diagnostics {
    // Usually, we don't report on an element more than one diagnostic, so it's better to use SmartList here
    private val diagnosticMap = HashMap<PsiElement, SmartList<Diagnostic>>()

    //NOTE: CachedValuesManager is not used because it requires Project passed to this object
    private val cache = CachedValueImpl(CachedValueProvider {
        val allDiagnostics = delegateDiagnostics.noSuppression().all() + getOwnDiagnostics()
        CachedValueProvider.Result(DiagnosticsWithSuppression(bindingContext, allDiagnostics), modificationTracker)
    })

    private fun readonlyView(): DiagnosticsWithSuppression = cache.value!!

    override val modificationTracker = CompositeModificationTracker(delegateDiagnostics.modificationTracker)

    override fun all(): Collection<Diagnostic> = readonlyView().all()
    override fun forElement(psiElement: PsiElement) = readonlyView().forElement(psiElement)
    override fun noSuppression() = readonlyView().noSuppression()

    //essential that this list is readonly
    fun getOwnDiagnostics(): List<Diagnostic> {
        return diagnosticMap.values.flatten()
    }

    fun report(diagnostic: Diagnostic) {
        diagnosticMap.getOrPut(diagnostic.psiElement, { SmartList() }).add(diagnostic)
        modificationTracker.incModificationCount()
    }

    fun clear() {
        diagnosticMap.clear()
        modificationTracker.incModificationCount()
    }

    override fun hasDiagnostic(diagnostic: Diagnostic): Boolean {
        if (cache.hasUpToDateValue()) return cache.value.hasDiagnostic(diagnostic)
        if (delegateDiagnostics.hasDiagnostic(diagnostic)) return true

        // It's totally fine to check without any suppression logic as if passed diagnostic has the same factory & psiElement,
        // then it has the same suppression logic, so it doesn't matter if we add it or not later
        val list = diagnosticMap[diagnostic.psiElement] ?: return false
        return list.any { it.factory == diagnostic.factory && it.psiElement == diagnostic.psiElement }
    }

    @TestOnly
    fun getReadonlyView(): DiagnosticsWithSuppression = readonlyView()
}
