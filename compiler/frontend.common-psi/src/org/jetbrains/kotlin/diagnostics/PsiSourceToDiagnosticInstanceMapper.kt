/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement

open class PsiSourceToDiagnosticInstanceMapper : KtSourceToDiagnosticInstanceMapper {
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
