/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtFakePsiSourceElement
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.SuspiciousFakeSourceCheck
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.diagnostics.*

internal class LLFirDiagnosticReporter : PendingDiagnosticReporter() {
    private val pendingDiagnostics = mutableMapOf<PsiElement, MutableList<KtPsiDiagnostic>>()
    private val _committedDiagnostics = mutableMapOf<PsiElement, MutableList<KtPsiDiagnostic>>()

    val committedDiagnostics get() = _committedDiagnostics.ifEmpty { emptyMap() }
    override val hasErrors: Boolean
        get() = committedDiagnostics.any { (_, diagnostics) -> diagnostics.any { it.severity.isError } }

    override val hasWarningsForWError: Boolean
        get() = committedDiagnostics.any { (_, diagnostics) -> diagnostics.any { it.severity.isErrorWhenWError } }

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        if (context.isDiagnosticSuppressed(diagnostic)) return

        // Implicit imports for scripts are currently implemented via FIR-tree mutation (they do not exist in default importing scopes).
        // So as a temporary solution we filter out related diagnostics here.
        if (diagnostic.isAboutImplicitImport()) return

        val psiDiagnostic = when (diagnostic) {
            is KtPsiDiagnostic -> diagnostic
            is KtLightDiagnostic -> diagnostic.toPsiDiagnostic()
            else -> error("Unknown diagnostic type ${diagnostic::class.simpleName}")
        }
        pendingDiagnostics.addValueFor(psiDiagnostic.psiElement, psiDiagnostic)
    }

    override fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext, commitEverything: Boolean) {
        for ((diagnosticElement, pendingList) in pendingDiagnostics) {
            val committedList = _committedDiagnostics.getOrPut(diagnosticElement) { mutableListOf() }
            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val diagnostic = iterator.next()
                when {
                    context.isDiagnosticSuppressed(diagnostic as KtDiagnostic) -> {
                        if (diagnostic.element == element ||
                            diagnostic.element.startOffset >= element.startOffset && diagnostic.element.endOffset <= element.endOffset
                        ) {
                            iterator.remove()
                        }
                    }
                    diagnostic.element == element || commitEverything -> {
                        iterator.remove()
                        committedList += diagnostic
                    }
                }
            }
        }
    }
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
