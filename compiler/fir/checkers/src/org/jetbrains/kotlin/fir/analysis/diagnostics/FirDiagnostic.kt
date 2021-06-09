/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.fir.FirLightSourceElement
import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement

// ------------------------------ diagnostics ------------------------------

sealed class FirDiagnostic {
    abstract val element: FirSourceElement
    abstract val severity: Severity
    abstract val factory: AbstractFirDiagnosticFactory
    abstract val positioningStrategy: SourceElementPositioningStrategy

    val textRanges: List<TextRange>
        get() = positioningStrategy.markDiagnostic(this)

    val isValid: Boolean
        get() = positioningStrategy.isValid(element)
}

sealed class FirSimpleDiagnostic : FirDiagnostic() {
    abstract override val factory: FirDiagnosticFactory0
}

sealed class FirDiagnosticWithParameters1<A> : FirDiagnostic() {
    abstract val a: A
    abstract override val factory: FirDiagnosticFactory1<A>
}

sealed class FirDiagnosticWithParameters2<A, B> : FirDiagnostic() {
    abstract val a: A
    abstract val b: B
    abstract override val factory: FirDiagnosticFactory2<A, B>
}

sealed class FirDiagnosticWithParameters3<A, B, C> : FirDiagnostic() {
    abstract val a: A
    abstract val b: B
    abstract val c: C
    abstract override val factory: FirDiagnosticFactory3<A, B, C>
}

sealed class FirDiagnosticWithParameters4<A, B, C, D> : FirDiagnostic() {
    abstract val a: A
    abstract val b: B
    abstract val c: C
    abstract val d: D
    abstract override val factory: FirDiagnosticFactory4<A, B, C, D>
}

// ------------------------------ psi diagnostics ------------------------------

interface FirPsiDiagnostic {
    val factory: AbstractFirDiagnosticFactory
    val element: FirPsiSourceElement
    val textRanges: List<TextRange>
    val severity: Severity

    val psiElement: PsiElement
        get() = element.psi

    val psiFile: PsiFile
        get() = psiElement.containingFile
}

private const val CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS = true

private fun FirPsiDiagnostic.checkPsiTypeConsistency() {
    if (CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS) {
        require(factory.psiType.isInstance(element.psi)) {
            "${element.psi::class} is not a subtype of ${factory.psiType} for factory $factory"
        }
    }
}

data class FirPsiSimpleDiagnostic(
    override val element: FirPsiSourceElement,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirSimpleDiagnostic(), FirPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class FirPsiDiagnosticWithParameters1<A>(
    override val element: FirPsiSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<A>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters1<A>(), FirPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}


data class FirPsiDiagnosticWithParameters2<A, B>(
    override val element: FirPsiSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<A, B>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters2<A, B>(), FirPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class FirPsiDiagnosticWithParameters3<A, B, C>(
    override val element: FirPsiSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters3<A, B, C>(), FirPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class FirPsiDiagnosticWithParameters4<A, B, C, D>(
    override val element: FirPsiSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters4<A, B, C, D>(), FirPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

// ------------------------------ light tree diagnostics ------------------------------

interface FirLightDiagnostic {
    val element: FirLightSourceElement
}

data class FirLightSimpleDiagnostic(
    override val element: FirLightSourceElement,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory0,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirSimpleDiagnostic(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters1<A>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory1<A>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters1<A>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters2<A, B>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory2<A, B>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters2<A, B>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters3<A, B, C>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters3<A, B, C>(), FirLightDiagnostic

data class FirLightDiagnosticWithParameters4<A, B, C, D>(
    override val element: FirLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: FirDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: SourceElementPositioningStrategy
) : FirDiagnosticWithParameters4<A, B, C, D>(), FirLightDiagnostic
