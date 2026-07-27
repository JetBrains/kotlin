/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.utils.exceptions.requireWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

// ------------------------------ psi diagnostics ------------------------------

interface KtPsiDiagnostic : DiagnosticMarker {
    val factory: KtDiagnosticFactoryN
    val element: KtPsiSourceElement
    override val textRanges: List<TextRange>
    override val severity: Severity

    override val psiElement: PsiElement
        get() = element.psi

    val psiFile: PsiFile
        get() = psiElement.containingFile
}

private const val CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS = true

private fun KtPsiDiagnostic.checkPsiTypeConsistency() {
    if (CHECK_PSI_CONSISTENCY_IN_DIAGNOSTICS) {
        requireWithAttachment(
            factory.psiType.isInstance(psiElement),
            { "${psiElement::class} is not a subtype of ${factory.psiType} for factory $factory" }
        ) {
            withPsiEntry("psi", psiElement)
            withPsiEntry("file", psiFile)
        }
    }
}

data class KtPsiSimpleDiagnostic(
    override val element: KtPsiSourceElement,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
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
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
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
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
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
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
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
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : KtDiagnosticWithParameters4<A, B, C, D>(), KtPsiDiagnostic {
    init {
        checkPsiTypeConsistency()
    }
}
