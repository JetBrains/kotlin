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
import com.intellij.psi.PsiFile
import com.intellij.psi.util.CachedValueProvider
import com.intellij.util.CachedValueImpl
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtStubbedPsiUtil

class MutableDiagnosticsWithSuppression(
    private val suppressCache: KotlinSuppressCache,
    private val delegateDiagnostics: Diagnostics,
) : Diagnostics {
    private val diagnosticList = ArrayList<Diagnostic>()

    @Volatile
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

    override fun setCallbackIfNotSet(callback: DiagnosticSink.DiagnosticsCallback): Boolean {
        return if (diagnosticsCallback == null) {
            diagnosticsCallback = callback
            delegateDiagnostics.setCallbackIfNotSet(callback)
            true
        } else false
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
        onTheFlyDiagnosticsCallback(diagnostic)?.callback(diagnostic)

        diagnosticList.add(diagnostic)
        modificationTracker.incModificationCount()
    }

    private fun onTheFlyDiagnosticsCallback(diagnostic: Diagnostic): DiagnosticSink.DiagnosticsCallback? {
        val callback = diagnosticsCallback ?: return null
        // Due to a potential recursion in filter.invoke (via LazyAnnotations) do not try to report
        // diagnostic on-the-fly if it happened in annotations, and do not report any potentially suppressed elements
        var element: PsiElement? = diagnostic.psiElement
        while (element != null && element !is PsiFile) {
            val annotated = KtStubbedPsiUtil.getPsiOrStubParent(element, KtAnnotated::class.java, false)
            val annotationEntries = annotated?.annotationEntries
            if (annotationEntries?.isNotEmpty() == true) return null
            element = annotated?.parent
        }
        val filtered = suppressCache.filter.invoke(diagnostic)
        if (!filtered) return null
        return callback
    }

    fun clear() {
        diagnosticList.clear()
        modificationTracker.incModificationCount()
    }

    @TestOnly
    fun getReadonlyView(): DiagnosticsWithSuppression = readonlyView()
}
