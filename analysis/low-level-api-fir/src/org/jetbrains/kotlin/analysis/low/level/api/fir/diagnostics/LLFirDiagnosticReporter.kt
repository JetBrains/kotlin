/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLDiagnostic
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.psi.psiUtil.isAncestor

/**
 * Collects diagnostics reported by compiler checkers.
 *
 * Suppressed diagnostics are **not** dropped: they are collected as well and marked with [LLDiagnostic.isSuppressed]. This way a single
 * collection pass serves both clients which need the diagnostics the compiler would report, and clients which analyze suppressions
 * themselves.
 */
internal class LLFirDiagnosticReporter : PendingDiagnosticReporter() {
    private val pendingDiagnostics = mutableMapOf<PsiElement, MutableList<PendingDiagnostic>>()
    private val _committedDiagnostics = mutableMapOf<PsiElement, MutableList<LLDiagnostic>>()

    val committedDiagnostics: Map<PsiElement, List<LLDiagnostic>> get() = _committedDiagnostics.ifEmpty { emptyMap() }

    override val hasErrors: Boolean
        get() = committedDiagnostics.any { [_, diagnostics] -> diagnostics.any { !it.isSuppressed && it.diagnostic.severity.isError } }

    override val hasWarningsForWError: Boolean
        get() = committedDiagnostics.any { [_, diagnostics] ->
            diagnostics.any { !it.isSuppressed && it.diagnostic.severity.isErrorWhenWError }
        }

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return

        // Implicit imports for scripts are currently implemented via FIR-tree mutation (they do not exist in default importing scopes).
        // So as a temporary solution we filter out related diagnostics here.
        if (diagnostic.isAboutImplicitImport()) return

        val psiDiagnostic = when (diagnostic) {
            is KtPsiDiagnostic -> diagnostic
            is KtLightDiagnostic -> diagnostic.toPsiDiagnostic()
            else -> error("Unknown diagnostic type ${diagnostic::class.simpleName}")
        }

        val pendingDiagnostic = PendingDiagnostic(psiDiagnostic, isSuppressed = context.isDiagnosticSuppressed(diagnostic))
        pendingDiagnostics.addValueFor(psiDiagnostic.psiElement, pendingDiagnostic)
    }

    override fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext, commitEverything: Boolean) {
        for ([diagnosticElement, pendingList] in pendingDiagnostics) {
            val committedList = _committedDiagnostics.getOrPut(diagnosticElement) { mutableListOf() }
            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val pendingDiagnostic = iterator.next()
                val diagnostic = pendingDiagnostic.diagnostic

                // The committing context knows about suppressions which were not yet visible at the reporting site,
                // but it can only judge diagnostics reported inside the committed element.
                if (!pendingDiagnostic.isSuppressed && diagnostic.isInside(element)) {
                    pendingDiagnostic.isSuppressed = context.isDiagnosticSuppressed(diagnostic as KtDiagnostic)
                }

                if (diagnostic.element == element || commitEverything) {
                    iterator.remove()
                    committedList += LLDiagnostic(diagnostic, pendingDiagnostic.isSuppressed)
                }
            }
        }
    }

    private class PendingDiagnostic(val diagnostic: KtPsiDiagnostic, var isSuppressed: Boolean)
}

/**
 * PSI ancestry is checked instead of text range containment, as walking the parent chain is cheaper than computing text ranges:
 * [PsiElement.getTextRange] has to traverse preceding siblings to compute the start offset.
 */
private fun KtPsiDiagnostic.isInside(element: AbstractKtSourceElement): Boolean {
    if (this.element == element) return true

    val elementPsi = (element as? KtPsiSourceElement)?.psi
        ?: return this.element.startOffset >= element.startOffset && this.element.endOffset <= element.endOffset

    return elementPsi.isAncestor(psiElement, strict = false)
}

@OptIn(SuspiciousFakeSourceCheck::class)
private fun KtDiagnostic.isAboutImplicitImport(): Boolean {
    if (this !is KtPsiDiagnostic) return false
    return (element is KtFakePsiSourceElement && (element as KtFakePsiSourceElement).kind == KtFakeSourceElementKind.ImplicitImport)
}


private fun KtLightDiagnostic.toPsiDiagnostic(): KtPsiDiagnostic {
    val psiSourceElement = element.unwrapToKtPsiSourceElement()
        ?: error("Diagnostic should be created from PSI in IDE")
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is KtLightSimpleDiagnostic -> KtPsiSimpleDiagnostic(
            psiSourceElement,
            severity,
            factory,
            positioningStrategy,
            context,
        )

        is KtLightDiagnosticWithParameters1<*> -> KtPsiDiagnosticWithParameters1(
            psiSourceElement,
            a,
            severity,
            factory as KtDiagnosticFactory1<Any?>,
            positioningStrategy,
            context,
        )

        is KtLightDiagnosticWithParameters2<*, *> -> KtPsiDiagnosticWithParameters2(
            psiSourceElement,
            a, b,
            severity,
            factory as KtDiagnosticFactory2<Any?, Any?>,
            positioningStrategy,
            context,
        )

        is KtLightDiagnosticWithParameters3<*, *, *> -> KtPsiDiagnosticWithParameters3(
            psiSourceElement,
            a, b, c,
            severity,
            factory as KtDiagnosticFactory3<Any?, Any?, Any?>,
            positioningStrategy,
            context,
        )

        is KtLightDiagnosticWithParameters4<*, *, *, *> -> KtPsiDiagnosticWithParameters4(
            psiSourceElement,
            a, b, c, d,
            severity,
            factory as KtDiagnosticFactory4<Any?, Any?, Any?, Any?>,
            positioningStrategy,
            context,
        )
        else -> error("Unknown diagnostic type ${this::class.simpleName}")
    }
}
