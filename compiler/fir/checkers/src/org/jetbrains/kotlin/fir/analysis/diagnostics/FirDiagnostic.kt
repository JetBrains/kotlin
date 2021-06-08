/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.UnboundDiagnostic
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

// ------------------------------ diagnostics ------------------------------

sealed class FirDiagnostic<out E : FirSourceElement> : UnboundDiagnostic {
    abstract val element: E
    abstract override val severity: Severity
    abstract override val factory: AbstractFirDiagnosticFactory<*, *>
    abstract val positioningStrategy: SourceElementPositioningStrategy<*>

    override val textRanges: List<TextRange>
        get() = positioningStrategy.markDiagnostic(this)

    override val isValid: Boolean
        get() = positioningStrategy.isValid(element)
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

sealed class FirDiagnosticWithParameters4<out E : FirSourceElement, A, B, C, D> : FirDiagnostic<E>() {
    abstract val a: A
    abstract val b: B
    abstract val c: C
    abstract val d: D
    abstract override val factory: FirDiagnosticFactory4<*, A, B, C, D>
}

// ------------------------------ psi diagnostics ------------------------------

interface FirPsiDiagnostic<P : PsiElement> : Diagnostic {
    val element: FirPsiSourceElement<P>

    override val psiElement: PsiElement
        get() = element.psi

    override val psiFile: PsiFile
        get() = psiElement.containingFile
}

data class FirPsiSimpleDiagnostic<P : PsiElement>(
    override val element: FirPsiSourceElement<P>,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0<P>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirSimpleDiagnostic<FirPsiSourceElement<P>>(), FirPsiDiagnostic<P>

data class FirPsiDiagnosticWithParameters1<P : PsiElement, A>(
    override val element: FirPsiSourceElement<P>,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<P, A>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters1<FirPsiSourceElement<P>, A>(), FirPsiDiagnostic<P>

data class FirPsiDiagnosticWithParameters2<P : PsiElement, A, B>(
    override val element: FirPsiSourceElement<P>,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<P, A, B>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters2<FirPsiSourceElement<P>, A, B>(), FirPsiDiagnostic<P>

data class FirPsiDiagnosticWithParameters3<P : PsiElement, A, B, C>(
    override val element: FirPsiSourceElement<P>,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<P, A, B, C>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters3<FirPsiSourceElement<P>, A, B, C>(), FirPsiDiagnostic<P>

data class FirPsiDiagnosticWithParameters4<P : PsiElement, A, B, C, D>(
    override val element: FirPsiSourceElement<P>,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory4<P, A, B, C, D>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters4<FirPsiSourceElement<P>, A, B, C, D>(), FirPsiDiagnostic<P>

// ------------------------------ light tree diagnostics ------------------------------

interface FirLightDiagnostic : UnboundDiagnostic {
    val element: FirLightSourceElement
}

data class FirLightSimpleDiagnostic(
    override val element: FirLightSourceElement,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0<*>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirSimpleDiagnostic<FirLightSourceElement>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters1<A>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<*, A>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters1<FirLightSourceElement, A>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters2<A, B>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<*, A, B>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters2<FirLightSourceElement, A, B>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters3<A, B, C>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<*, A, B, C>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters3<FirLightSourceElement, A, B, C>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters4<A, B, C, D>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory4<*, A, B, C, D>,
    override val positioningStrategy: SourceElementPositioningStrategy<*>
) : FirDiagnosticWithParameters4<FirLightSourceElement, A, B, C, D>(), FirLightDiagnostic
