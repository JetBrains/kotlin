/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.diagnostics.AbstractSourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticBaseContext
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory4
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters3
import org.jetbrains.kotlin.diagnostics.KtDiagnosticWithParameters4
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters1
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters2
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters3
import org.jetbrains.kotlin.diagnostics.KtPsiDiagnosticWithParameters4
import org.jetbrains.kotlin.diagnostics.KtPsiSimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.KtSimpleDiagnostic
import org.jetbrains.kotlin.diagnostics.PsiSourceToDiagnosticInstanceMapper
import org.jetbrains.kotlin.diagnostics.Severity

internal class PsiSourceToDiagnosticInstanceMapperComponent :
    PsiSourceToDiagnosticInstanceMapper(), KtSourceToDiagnosticInstanceMapperComponent {
    override fun createDiagnostic0(
        element: AbstractKtSourceElement,
        severity: Severity,
        factory: KtDiagnosticFactory0,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext,
    ): KtSimpleDiagnostic {
        return KtPsiSimpleDiagnostic(
            element as KtPsiSourceElement,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    override fun <A> createDiagnostic1(
        element: AbstractKtSourceElement,
        severity: Severity,
        factory: KtDiagnosticFactory1<A>,
        a: A,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters1<A> {
        return KtPsiDiagnosticWithParameters1(
            element as KtPsiSourceElement,
            a,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    override fun <A, B> createDiagnostic2(
        element: AbstractKtSourceElement,
        severity: Severity,
        factory: KtDiagnosticFactory2<A, B>,
        a: A,
        b: B,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters2<A, B> {
        return KtPsiDiagnosticWithParameters2(
            element as KtPsiSourceElement,
            a,
            b,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    override fun <A, B, C> createDiagnostic3(
        element: AbstractKtSourceElement,
        severity: Severity,
        factory: KtDiagnosticFactory3<A, B, C>,
        a: A,
        b: B,
        c: C,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters3<A, B, C> {
        return KtPsiDiagnosticWithParameters3(
            element as KtPsiSourceElement,
            a,
            b,
            c,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }

    override fun <A, B, C, D> createDiagnostic4(
        element: AbstractKtSourceElement,
        severity: Severity,
        factory: KtDiagnosticFactory4<A, B, C, D>,
        a: A,
        b: B,
        c: C,
        d: D,
        positioningStrategy: AbstractSourceElementPositioningStrategy,
        context: DiagnosticBaseContext,
    ): KtDiagnosticWithParameters4<A, B, C, D> {
        return KtPsiDiagnosticWithParameters4(
            element as KtPsiSourceElement,
            a,
            b,
            c,
            d,
            severity,
            factory,
            positioningStrategy,
            context,
        )
    }
}
