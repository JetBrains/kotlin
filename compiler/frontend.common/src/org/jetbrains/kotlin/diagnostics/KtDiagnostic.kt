/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtMissingSourceElement
import org.jetbrains.kotlin.KtOffsetsOnlySourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation

// ------------------------------ diagnostics ------------------------------

sealed class KtDiagnostic {
    abstract val severity: Severity
    abstract val factory: AbstractKtDiagnosticFactory
    abstract val isValid: Boolean
    abstract val firstRange: TextRange
    abstract val context: DiagnosticContext

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
    override val context: DiagnosticContext,
) : KtDiagnostic() {
    override val isValid: Boolean
        get() = true

    override val firstRange: TextRange
        get() = TextRange.EMPTY_RANGE
}

sealed class KtDiagnosticWithSource : KtDiagnostic() {
    abstract val element: AbstractKtSourceElement
    abstract override val factory: KtDiagnosticFactoryN
    abstract val positioningStrategyKind: PositioningStrategyKind
    abstract override val severity: Severity

    companion object {
        private val improperTextRanges = listOf(PossiblyImproperTextRange(-1, -1))
    }

    val textRanges: List<TextRange>
        get() {
            return when (element) {
                is KtMissingSourceElement -> improperTextRanges
                is KtOffsetsOnlySourceElement -> listOf(PossiblyImproperTextRange(element.startOffset, element.endOffset))
                is KtSourceElement -> {
                    with(context.parseTreeKind) {
                        positioningStrategyKind.positioningStrategy.markDiagnostic(this@KtDiagnosticWithSource)
                    }
                }
            }
        }

    final override val isValid: Boolean
        get() {
            return when (val element = element) {
                is KtMissingSourceElement -> true
                is KtOffsetsOnlySourceElement -> true
                is KtSourceElement -> {
                    with(context.parseTreeKind) {
                        @OptIn(DiagnosticLossRisk::class)
                        positioningStrategyKind.positioningStrategy.isValid(element)
                    }
                }
            }
        }

    final override val firstRange: TextRange
        get() = DiagnosticRangeUtils.firstRange(textRanges)
}

data class KtSimpleDiagnostic(
    override val element: AbstractKtSourceElement,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory0,
    override val positioningStrategyKind: PositioningStrategyKind,
    override val context: DiagnosticContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters1<A>(
    override val element: AbstractKtSourceElement,
    val a: A,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory1<A>,
    override val positioningStrategyKind: PositioningStrategyKind,
    override val context: DiagnosticContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters2<A, B>(
    override val element: AbstractKtSourceElement,
    val a: A,
    val b: B,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory2<A, B>,
    override val positioningStrategyKind: PositioningStrategyKind,
    override val context: DiagnosticContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters3<A, B, C>(
    override val element: AbstractKtSourceElement,
    val a: A,
    val b: B,
    val c: C,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory3<A, B, C>,
    override val positioningStrategyKind: PositioningStrategyKind,
    override val context: DiagnosticContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters4<A, B, C, D>(
    override val element: AbstractKtSourceElement,
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory4<A, B, C, D>,
    override val positioningStrategyKind: PositioningStrategyKind,
    override val context: DiagnosticContext,
) : KtDiagnosticWithSource()

data class KtDiagnosticWithParameters5<A, B, C, D, E>(
    override val element: AbstractKtSourceElement,
    val a: A,
    val b: B,
    val c: C,
    val d: D,
    val e: E,
    override val severity: Severity,
    override val factory: KtDiagnosticFactory5<A, B, C, D, E>,
    override val positioningStrategyKind: PositioningStrategyKind,
    override val context: DiagnosticContext,
) : KtDiagnosticWithSource()
