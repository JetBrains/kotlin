/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement

// ------------------------------ diagnostics ------------------------------

sealed class KtDiagnostic : DiagnosticMarker {
    abstract val element: AbstractKtSourceElement
    abstract val severity: Severity
    abstract val factory: AbstractKtDiagnosticFactory
    abstract val positioningStrategy: AbstractSourceElementPositioningStrategy

    val textRanges: List<TextRange>
        get() = positioningStrategy.markDiagnostic(this)

    val isValid: Boolean
        get() = positioningStrategy.isValid(element)

    override val factoryName: String
        get() = factory.name
}

sealed class KtSimpleDiagnostic : KtDiagnostic() {
    abstract override val factory: KtDiagnosticFactory0
}

sealed class KtDiagnosticWithParameters1<A> : KtDiagnostic(), DiagnosticWithParameters1Marker<A> {
    abstract override val a: A
    abstract override val factory: KtDiagnosticFactory1<A>
}

sealed class KtDiagnosticWithParameters2<A, B> : KtDiagnostic(), DiagnosticWithParameters2Marker<A, B> {
    abstract override val a: A
    abstract override val b: B
    abstract override val factory: KtDiagnosticFactory2<A, B>
}

sealed class KtDiagnosticWithParameters3<A, B, C> : KtDiagnostic(), DiagnosticWithParameters3Marker<A, B, C> {
    abstract override val a: A
    abstract override val b: B
    abstract override val c: C
    abstract override val factory: KtDiagnosticFactory3<A, B, C>
}

sealed class KtDiagnosticWithParameters4<A, B, C, D> : KtDiagnostic(), DiagnosticWithParameters4Marker<A, B, C, D> {
    abstract override val a: A
    abstract override val b: B
    abstract override val c: C
    abstract override val d: D
    abstract override val factory: KtDiagnosticFactory4<A, B, C, D>
}

// ------------------------------ psi diagnostics ------------------------------

interface KtPsiDiagnostic : DiagnosticMarker {
    val factory: AbstractKtDiagnosticFactory
    val element: KtPsiSourceElement
    val textRanges: List<TextRange>
    val severity: Severity

    override val psiElement: PsiElement
        get() = element.psi

    val psiFile: PsiFile
        get() = psiElement.containingFile
}

private const val CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS = true

private fun KtPsiDiagnostic.checkPsiTypeConsistency() {
    if (CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS) {
        require(factory.psiType.isInstance(element.psi)) {
            "${element.psi::class} is not a subtype of ${factory.psiType} for factory $factory"
        }
    }
}

data class KtPsiSimpleDiagnostic(
    override val element: KtPsiSourceElement,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtSimpleDiagnostic(), KtPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class KtPsiDiagnosticWithParameters1<A>(
    override val element: KtPsiSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory1<A>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters1<A>(), KtPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}


data class KtPsiDiagnosticWithParameters2<A, B>(
    override val element: KtPsiSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory2<A, B>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters2<A, B>(), KtPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class KtPsiDiagnosticWithParameters3<A, B, C>(
    override val element: KtPsiSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters3<A, B, C>(), KtPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

data class KtPsiDiagnosticWithParameters4<A, B, C, D>(
    override val element: KtPsiSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters4<A, B, C, D>(), KtPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}

// ------------------------------ light tree diagnostics ------------------------------

interface KtLightDiagnostic : DiagnosticMarker {
    val element: KtLightSourceElement

    @Deprecated("Should not be called", level = DeprecationLevel.HIDDEN)
    override val psiElement: PsiElement
        get() = error("psiElement should not be called on KtLightDiagnostic")
}

data class KtLightSimpleDiagnostic(
    override val element: KtLightSourceElement,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtSimpleDiagnostic(), KtLightDiagnostic

data class KtLightDiagnosticWithParameters1<A>(
    override val element: KtLightSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory1<A>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters1<A>(), KtLightDiagnostic

data class KtLightDiagnosticWithParameters2<A, B>(
    override val element: KtLightSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory2<A, B>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters2<A, B>(), KtLightDiagnostic

data class KtLightDiagnosticWithParameters3<A, B, C>(
    override val element: KtLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters3<A, B, C>(), KtLightDiagnostic

data class KtLightDiagnosticWithParameters4<A, B, C, D>(
    override val element: KtLightSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters4<A, B, C, D>(), KtLightDiagnostic

// ------------------------------ light tree diagnostics ------------------------------

interface KtOffsetsOnlyDiagnostic : DiagnosticMarker {
    val element: AbstractKtSourceElement

    @Deprecated("Should not be called", level = DeprecationLevel.HIDDEN)
    override val psiElement: PsiElement
        get() = error("psiElement should not be called on KtOffsetsOnlyDiagnostic")
}

data class KtOffsetsOnlySimpleDiagnostic(
    override val element: AbstractKtSourceElement,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtSimpleDiagnostic(), KtOffsetsOnlyDiagnostic

data class KtOffsetsOnlyDiagnosticWithParameters1<A>(
    override val element: AbstractKtSourceElement,
    override val a: A,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory1<A>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters1<A>(), KtOffsetsOnlyDiagnostic

data class KtOffsetsOnlyDiagnosticWithParameters2<A, B>(
    override val element: AbstractKtSourceElement,
    override val a: A,
    override val b: B,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory2<A, B>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters2<A, B>(), KtOffsetsOnlyDiagnostic

data class KtOffsetsOnlyDiagnosticWithParameters3<A, B, C>(
    override val element: AbstractKtSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters3<A, B, C>(), KtOffsetsOnlyDiagnostic

data class KtOffsetsOnlyDiagnosticWithParameters4<A, B, C, D>(
    override val element: AbstractKtSourceElement,
    override val a: A,
    override val b: B,
    override val c: C,
    override val d: D,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy
) : KtDiagnosticWithParameters4<A, B, C, D>(), KtOffsetsOnlyDiagnostic
