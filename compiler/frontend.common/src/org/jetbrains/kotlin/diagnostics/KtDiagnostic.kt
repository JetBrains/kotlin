/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

// ------------------------------ diagnostics ------------------------------

sealed class KtDiagnostic {
    abstract val severity: Severity
    abstract val factory: AbstractKtDiagnosticFactory
    abstract val isValid: Boolean
    abstract val firstRange: TextRange
    abstract val context: DiagnosticBaseContext

    val factoryName: String
        get() = factory.name

    fun renderMessage(): String {
        return factory.ktRenderer.render(this)
    }
}

class KtDiagnosticWithoutSource(
    val message: String,
    val location: CompilerMessageSourceLocation?,
    override val severity: Severity,
    override val factory: KtSourcelessDiagnosticFactory,
    override val context: DiagnosticBaseContext,
) : KtDiagnostic() {
    override val isValid: Boolean
        get() = true

    override val firstRange: TextRange
        get() = TextRange.EMPTY_RANGE
}

sealed class KtDiagnosticWithSource : KtDiagnostic(), DiagnosticMarker {
    abstract val element: AbstractKtSourceElement
    abstract override val factory: KtDiagnosticFactoryN
    abstract val positioningStrategy: AbstractSourceElementPositioningStrategy
    abstract override val severity: Severity

    final override val textRanges: List<TextRange>
        get() = positioningStrategy.markDiagnostic(this)

    final override val isValid: Boolean
        get() = positioningStrategy.isValid(element)

    final override val firstRange: TextRange
        get() = DiagnosticRangeUtils.firstRange(textRanges)

    @K1Deprecation
    final override val psiElement: PsiElement
        get() = (element as KtPsiSourceElement).psi
}

data class KtSimpleDiagnostic(
    override val element: AbstractKtSourceElement,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory0,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters1<A>(
    override val element: AbstractKtSourceElement,
    val a: A,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory1<A>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters2<A, B>(
    override val element: AbstractKtSourceElement,
    val a: A,
    val b: B,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory2<A, B>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters3<A, B, C>(
    override val element: AbstractKtSourceElement,
    val a: A,
    val b: B,
    val c: C,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory3<A, B, C>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters4<A, B, C, D>(
    override val element: AbstractKtSourceElement,
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategy: AbstractSourceElementPositioningStrategy,
    override val context: DiagnosticBaseContext,
) : KtDiagnosticWithSource()
