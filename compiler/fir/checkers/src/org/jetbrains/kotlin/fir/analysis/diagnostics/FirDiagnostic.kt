/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

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
    abstract override val factory: FirDiagnosticFactory0<*>
}

sealed class FirDiagnosticWithParameters1<out E : FirSourceElement, A> : FirDiagnostic<E>() {
    abstract val a: A
    abstract override val factory: FirDiagnosticFactory1<*, A>
}

sealed class FirDiagnosticWithParameters2<out E : FirSourceElement, A, B> : FirDiagnostic<E>() {
    abstract val a: A
    abstract val b: B
    abstract override val factory: FirDiagnosticFactory2<*, A, B>
}

sealed class FirDiagnosticWithParameters3<out E : FirSourceElement, A, B, C> : FirDiagnostic<E>() {
    abstract val a: A
    abstract val b: B
    abstract val c: C
    abstract override val factory: FirDiagnosticFactory3<*, A, B, C>
}

// ------------------------------ psi diagnostics ------------------------------

interface FirPsiDiagnostic<out E : FirPsiSourceElement> {
    fun asPsiBasedDiagnostic(): Diagnostic
    val element: E
}

data class FirPsiSimpleDiagnostic<out E : FirPsiSourceElement>(
    override val element: E,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0<*>
) : FirSimpleDiagnostic<E>(), FirPsiDiagnostic<E> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi)
    }
}

data class FirPsiDiagnosticWithParameters1<out E : FirPsiSourceElement, A>(
    override val element: E,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<*, A>
) : FirDiagnosticWithParameters1<E, A>(), FirPsiDiagnostic<E> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi, a)
    }
}

data class FirPsiDiagnosticWithParameters2<out E : FirPsiSourceElement, A, B>(
    override val element: E,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<*, A, B>
) : FirDiagnosticWithParameters2<E, A, B>(), FirPsiDiagnostic<E> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi, a, b)
    }
}

data class FirPsiDiagnosticWithParameters3<out E : FirPsiSourceElement, A, B, C>(
    override val element: E,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<*, A, B, C>
) : FirDiagnosticWithParameters3<E, A, B, C>(), FirPsiDiagnostic<E> {
    override fun asPsiBasedDiagnostic(): Diagnostic {
        return factory.psiDiagnosticFactory.on(element.psi, a, b, c)
    }
}

// ------------------------------ light tree diagnostics ------------------------------

interface FirLightDiagnostic<out E: FirLightSourceElement> {
    val element: E
}

data class FirLightSimpleDiagnostic<out E : FirLightSourceElement>(
    override val element: E,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0<*>
) : FirSimpleDiagnostic<E>(), FirLightDiagnostic<E>

data class FirLightDiagnosticWithParameters1<out E : FirLightSourceElement, A>(
    override val element: E,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<*, A>
) : FirDiagnosticWithParameters1<E, A>(), FirLightDiagnostic<E>

data class FirLightDiagnosticWithParameters2<out E : FirLightSourceElement, A, B>(
    override val element: E,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<*, A, B>
) : FirDiagnosticWithParameters2<E, A, B>(), FirLightDiagnostic<E>

data class FirLightDiagnosticWithParameters3<out E : FirLightSourceElement, A, B, C>(
    override val element: E,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<*, A, B, C>
) : FirDiagnosticWithParameters3<E, A, B, C>(), FirLightDiagnostic<E>