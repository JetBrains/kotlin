/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.AbstractKtSourceElement
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.WrappedTreeStructure
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.addValueFor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.toKtPsiSourceElement

internal class LLFirDiagnosticReporter(private val session: FirSession) : DiagnosticReporter() {
    private val pendingDiagnostics = mutableMapOf<PsiElement, MutableList<KtPsiDiagnostic>>()
    val committedDiagnostics = mutableMapOf<PsiElement, MutableList<KtPsiDiagnostic>>()

    override fun report(diagnostic: KtDiagnostic?, context: DiagnosticContext) {
        if (diagnostic == null) return
        if (context.isDiagnosticSuppressed(diagnostic)) return

        val psiDiagnostic = when (diagnostic) {
            is KtPsiDiagnostic -> diagnostic
            is KtLightDiagnostic -> diagnostic.toPsiDiagnostic(session)
            else -> error("Unknown diagnostic type ${diagnostic::class.simpleName}")
        }
        pendingDiagnostics.addValueFor(psiDiagnostic.psiElement, psiDiagnostic)
    }

    override fun checkAndCommitReportsOn(element: AbstractKtSourceElement, context: DiagnosticContext?) {
        val commitEverything = context == null
        for ((diagnosticElement, pendingList) in pendingDiagnostics) {
            val committedList = committedDiagnostics.getOrPut(diagnosticElement) { mutableListOf() }
            val iterator = pendingList.iterator()
            while (iterator.hasNext()) {
                val diagnostic = iterator.next()
                when {
                    context?.isDiagnosticSuppressed(diagnostic as KtDiagnostic) == true -> {
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

private fun KtLightDiagnostic.toPsiDiagnostic(session: FirSession): KtPsiDiagnostic {
    val psiSourceElement = element.unwrapToKtPsiSourceElement(session)
        ?: error("Diagnostic should be created from PSI in IDE")
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is KtLightSimpleDiagnostic -> KtPsiSimpleDiagnostic(
            psiSourceElement,
            severity,
            factory,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters1<*> -> KtPsiDiagnosticWithParameters1(
            psiSourceElement,
            a,
            severity,
            factory as KtDiagnosticFactory1<Any?>,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters2<*, *> -> KtPsiDiagnosticWithParameters2(
            psiSourceElement,
            a, b,
            severity,
            factory as KtDiagnosticFactory2<Any?, Any?>,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters3<*, *, *> -> KtPsiDiagnosticWithParameters3(
            psiSourceElement,
            a, b, c,
            severity,
            factory as KtDiagnosticFactory3<Any?, Any?, Any?>,
            positioningStrategy
        )

        is KtLightDiagnosticWithParameters4<*, *, *, *> -> KtPsiDiagnosticWithParameters4(
            psiSourceElement,
            a, b, c, d,
            severity,
            factory as KtDiagnosticFactory4<Any?, Any?, Any?, Any?>,
            positioningStrategy
        )
        else -> error("Unknown diagnostic type ${this::class.simpleName}")
    }
}

/**
 * We can create a [KtLightSourceElement] from a [KtPsiSourceElement] by using [KtPsiSourceElement.lighterASTNode];
 * [unwrapToKtPsiSourceElement] allows to get original [KtPsiSourceElement] in such case.
 *
 * If it is `pure` [KtLightSourceElement], i.e, compiler created it in light tree mode, then return [unwrapToKtPsiSourceElement] `null`.
 * Otherwise, return some not-null result.
 */
private fun KtLightSourceElement.unwrapToKtPsiSourceElement(session: FirSession): KtPsiSourceElement? {
    val treeStructure = treeStructure
    if (treeStructure !is WrappedTreeStructure) return null
    val node = treeStructure.unwrap(lighterASTNode)
    return node.psi?.toKtPsiSourceElement(session, kind)
}
