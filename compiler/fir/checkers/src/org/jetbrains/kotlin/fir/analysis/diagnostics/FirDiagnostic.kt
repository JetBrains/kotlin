/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

// ------------------------------ diagnostics ------------------------------

sealed class FirDiagnostic<out E : FirSourceElement> {
    abstract val element: E
    abstract val severity: Severity
    abstract val factory: AbstractFirDiagnosticFactory<*, *>
}

sealed class FirSimpleDiagnostic<out E : FirSourceElement> : FirDiagnostic<E>() {
    abstract override val factory: FirDiagnosticFactory0<*, *>
}

sealed class FirDiagnosticWithParameters1<out E : FirSourceElement, A> : FirDiagnostic<E>() {
    abstract val a: A
    abstract override val factory: FirDiagnosticFactory1<*, *, A>
}

sealed class FirDiagnosticWithParameters2<out E : FirSourceElement, A, B> : FirDiagnostic<E>() {
    abstract val a: A
    abstract val b: B
    abstract override val factory: FirDiagnosticFactory2<*, *, A, B>
}

sealed class FirDiagnosticWithParameters3<out E : FirSourceElement, A, B, C> : FirDiagnostic<E>() {
    abstract val a: A
    abstract val b: B
    abstract val c: C
    abstract override val factory: FirDiagnosticFactory3<*, *, A, B, C>
}

// ------------------------------ psi diagnostics ------------------------------

interface FirPsiDiagnostic<P : PsiElement> {
    fun asPsiBasedDiagnostic(): Diagnostic
    val element: FirPsiSourceElement<P>
}

data class FirPsiSimpleDiagnostic<P : PsiElement>(
    override val element: FirPsiSourceElement<P>,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0<FirPsiSourceElement<P>, P>
) : FirSimpleDiagnostic<FirPsiSourceElement<P>>(), FirPsiDiagnostic<P> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi)
    }
}

data class FirPsiDiagnosticWithParameters1<P : PsiElement, A>(
    override val element: FirPsiSourceElement<P>,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<FirPsiSourceElement<P>, P, A>
) : FirDiagnosticWithParameters1<FirPsiSourceElement<P>, A>(), FirPsiDiagnostic<P> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi, a)
    }
}

data class FirPsiDiagnosticWithParameters2<P : PsiElement, A, B>(
    override val element: FirPsiSourceElement<P>,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<FirPsiSourceElement<P>, P, A, B>
) : FirDiagnosticWithParameters2<FirPsiSourceElement<P>, A, B>(), FirPsiDiagnostic<P> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi, a, b)
    }
}

data class FirPsiDiagnosticWithParameters3<P : PsiElement, A, B, C>(
    override val element: FirPsiSourceElement<P>,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<FirPsiSourceElement<P>, P, A, B, C>
) : FirDiagnosticWithParameters3<FirPsiSourceElement<P>, A, B, C>(), FirPsiDiagnostic<P> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi, a, b, c)
    }
}

// ------------------------------ light tree diagnostics ------------------------------

interface FirLightDiagnostic {
    val element: FirLightSourceElement
}

data class FirLightSimpleDiagnostic(
    override val element: FirLightSourceElement,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0<*, *>
) : FirSimpleDiagnostic<FirLightSourceElement>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters1<A>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<*, *, A>
) : FirDiagnosticWithParameters1<FirLightSourceElement, A>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters2<A, B>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<*, *, A, B>
) : FirDiagnosticWithParameters2<FirLightSourceElement, A, B>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters3<A, B, C>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<*, *, A, B, C>
) : FirDiagnosticWithParameters3<FirLightSourceElement, A, B, C>(), FirLightDiagnostic