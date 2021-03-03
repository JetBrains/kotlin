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

import com.intellij.openapi.util.CompositeModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.CachedValueImpl
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtStubbedPsiUtil

class MutableDiagnosticsWithSuppression(
    private val suppressCache: KotlinSuppressCache,
    private val delegateDiagnostics: Diagnostics,
) : Diagnostics {
    private val diagnosticList = ArrayList<Diagnostic>()
    private var diagnosticsCallback: DiagnosticSink.DiagnosticsCallback? = null

    //NOTE: CachedValuesManager is not used because it requires Project passed to this object
    private val cache = CachedValueImpl {
        val allDiagnostics = delegateDiagnostics.noSuppression().all() + diagnosticList
        CachedValueProvider.Result(DiagnosticsWithSuppression(suppressCache, allDiagnostics), modificationTracker)
    }

    private fun readonlyView(): DiagnosticsWithSuppression = cache.value!!

    override val modificationTracker = CompositeModificationTracker(delegateDiagnostics.modificationTracker)

    override fun all(): Collection<Diagnostic> = readonlyView().all()
    override fun forElement(psiElement: PsiElement) = readonlyView().forElement(psiElement)
    override fun noSuppression() = readonlyView().noSuppression()

    override fun setCallback(callback: DiagnosticSink.DiagnosticsCallback) {
        // TODO: [VD] temporary dirty patch, proper investigation is required
        // assert(diagnosticsCallback == null) { "diagnostic callback has been already registered" }
        diagnosticsCallback = callback
        delegateDiagnostics.setCallback(callback)
    }

    override fun resetCallback() {
        diagnosticsCallback = null
        delegateDiagnostics.resetCallback()
    }

    //essential that this list is readonly
    fun getOwnDiagnostics(): List<Diagnostic> {
        return diagnosticList
    }

    fun report(diagnostic: Diagnostic) {
        onFlyDiagnosticsCallback(diagnostic)?.callback(diagnostic)

        diagnosticList.add(diagnostic)
        modificationTracker.incModificationCount()
    }

    private fun onFlyDiagnosticsCallback(diagnostic: Diagnostic): DiagnosticSink.DiagnosticsCallback? =
        diagnosticsCallback.takeIf {
            diagnosticsCallback != null &&
                    // Due to a potential recursion in filter.invoke (via LazyAnnotations) do not try to report
                    // diagnostic on fly if it happened in annotations
                    KtStubbedPsiUtil.getPsiOrStubParent(diagnostic.psiElement, KtAnnotationEntry::class.java, false) == null &&
                    suppressCache.filter.invoke(diagnostic)
        }

    fun clear() {
        diagnosticList.clear()
        modificationTracker.incModificationCount()
    }

    @TestOnly
    fun getReadonlyView(): DiagnosticsWithSuppression = readonlyView()
}
