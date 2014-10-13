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

package org.jetbrains.jet.resolve

import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace
import org.junit.Assert
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.psi.JetPsiFactory
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment
import org.jetbrains.jet.config.CompilerConfiguration
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.Diagnostics
import org.jetbrains.jet.lang.resolve.MutableDiagnosticsWithSuppression

class MutableDiagnosticsTest : KotlinTestWithEnvironment() {
    override fun createEnvironment(): JetCoreEnvironment? {
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, CompilerConfiguration())
    }

    private val BindingTrace.diagnostics: Diagnostics
        get() = getBindingContext().getDiagnostics()

    fun testPropagatingModification() {
        val base = BindingTraceContext()
        val middle = DelegatingBindingTrace(base.getBindingContext(), "middle")
        val derived = DelegatingBindingTrace(middle.getBindingContext(), "derived")

        Assert.assertTrue(base.diagnostics.isEmpty())
        Assert.assertTrue(middle.diagnostics.isEmpty())
        Assert.assertTrue(derived.diagnostics.isEmpty())

        middle.reportDiagnostic()

        Assert.assertTrue(base.diagnostics.isEmpty())
        Assert.assertFalse(middle.diagnostics.isEmpty())
        Assert.assertFalse(derived.diagnostics.isEmpty())

        base.clearDiagnostics()
        derived.clear()

        Assert.assertTrue(base.diagnostics.isEmpty())
        Assert.assertFalse(middle.diagnostics.isEmpty())
        Assert.assertFalse(derived.diagnostics.isEmpty())

        middle.clear()

        Assert.assertTrue(base.diagnostics.isEmpty())
        Assert.assertTrue(middle.diagnostics.isEmpty())
        Assert.assertTrue(derived.diagnostics.isEmpty())

        base.reportDiagnostic()
        middle.reportDiagnostic()
        derived.reportDiagnostic()

        Assert.assertEquals(1, base.diagnostics.all().size)
        Assert.assertEquals(2, middle.diagnostics.all().size)
        Assert.assertEquals(3, derived.diagnostics.all().size)

        middle.clear()

        Assert.assertEquals(1, base.diagnostics.all().size)
        Assert.assertEquals(1, middle.diagnostics.all().size)
        Assert.assertEquals(2, derived.diagnostics.all().size)
    }

    fun testCaching() {
        val base = BindingTraceContext()
        val middle = DelegatingBindingTrace(base.getBindingContext(), "middle")
        val derived = DelegatingBindingTrace(middle.getBindingContext(), "derived")

        base.reportDiagnostic()
        middle.reportDiagnostic()
        derived.reportDiagnostic()

        val cachedBase = base.diagnostics
        val cachedMiddle = middle.diagnostics
        val cachedDerived = derived.diagnostics

        val cachedListForBase = cachedBase.contents()
        val cachedListForMiddle = cachedMiddle.contents()
        val cachedListForDerived = cachedDerived.contents()

        Assert.assertSame(cachedListForBase, base.diagnostics.contents())
        Assert.assertSame(cachedListForMiddle, middle.diagnostics.contents())
        Assert.assertSame(cachedListForDerived, derived.diagnostics.contents())

        Assert.assertSame(cachedBase, base.diagnostics)
        Assert.assertSame(cachedMiddle, middle.diagnostics)
        Assert.assertSame(cachedDerived, derived.diagnostics)

        derived.reportDiagnostic()

        Assert.assertSame(cachedListForBase, base.diagnostics.contents())
        Assert.assertSame(cachedListForMiddle, middle.diagnostics.contents())
        Assert.assertNotSame(cachedListForDerived, derived.diagnostics.contents())

        Assert.assertSame(cachedBase, base.diagnostics)
        Assert.assertSame(cachedMiddle, middle.diagnostics)

        middle.reportDiagnostic()

        Assert.assertSame(cachedListForBase, base.diagnostics.contents())
        Assert.assertNotSame(cachedListForMiddle, middle.diagnostics.contents())
        Assert.assertNotSame(cachedListForDerived, derived.diagnostics.contents())

        Assert.assertSame(cachedBase, base.diagnostics)
    }

    private fun BindingTrace.reportDiagnostic() {
        report(DummyDiagnostic())
    }

    //NOTE: cannot simply call all() since it applies filter on every query and produces new collection
    private fun Diagnostics.contents(): MutableCollection<Diagnostic> {
        return (this as MutableDiagnosticsWithSuppression).getReadonlyView().getDiagnostics()
    }

    private inner class DummyDiagnostic : Diagnostic {
        val dummyElement = JetPsiFactory(getEnvironment().getProject()).createType("Int")

        override fun getFactory() = unimplemented()
        override fun getSeverity() = unimplemented()
        override fun getPsiElement() = dummyElement
        override fun getTextRanges() = unimplemented()
        override fun getPsiFile() = unimplemented()
        override fun isValid() = unimplemented()

        private fun unimplemented(): Nothing = throw UnsupportedOperationException()

    }
}
