/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.analysis.CheckersComponent
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.collectors.components.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.checkers.*

internal abstract class AbstractFirIdeDiagnosticsCollector(
    session: FirSession,
    useExtendedCheckers: Boolean,
) : AbstractDiagnosticCollector(
    session
) {
    init {
        val declarationCheckers = CheckersFactory.createDeclarationCheckers(useExtendedCheckers)
        val expressionCheckers = CheckersFactory.createExpressionCheckers(useExtendedCheckers)
        val typeCheckers = CheckersFactory.createTypeCheckers(useExtendedCheckers)

        @Suppress("LeakingThis")
        initializeComponents(
            DeclarationCheckersDiagnosticComponent(this, declarationCheckers),
            ExpressionCheckersDiagnosticComponent(this, expressionCheckers),
            TypeCheckersDiagnosticComponent(this, typeCheckers),
            ErrorNodeDiagnosticCollectorComponent(this),
            ControlFlowAnalysisDiagnosticComponent(this, declarationCheckers),
        )
    }

    protected abstract fun onDiagnostic(diagnostic: FirPsiDiagnostic<*>)


    private inner class Reporter : DiagnosticReporter() {
        override fun report(diagnostic: FirDiagnostic<*>?, context: CheckerContext) {
            if (diagnostic == null) return
            if (context.isDiagnosticSuppressed(diagnostic)) return

            val psiDiagnostic = when (diagnostic) {
                is FirPsiDiagnostic<*> -> diagnostic
                is FirLightDiagnostic -> diagnostic.toPsiDiagnostic()
                else -> error("Unknown diagnostic type ${diagnostic::class.simpleName}")
            }

            onDiagnostic(psiDiagnostic)
        }
    }

    override var reporter: DiagnosticReporter = Reporter()

    override fun initializeCollector() {
        reporter = Reporter()
    }


    override fun getCollectedDiagnostics(): List<FirDiagnostic<*>> {
        // Not necessary in IDE
        return emptyList()
    }
}

private fun FirLightDiagnostic.toPsiDiagnostic(): FirPsiDiagnostic<*> {
    val psiSourceElement = element.unwrapToFirPsiSourceElement()
        ?: error("Diagnostic should be created from PSI in IDE")
    @Suppress("UNCHECKED_CAST")
    return when (this) {
        is FirLightSimpleDiagnostic -> FirPsiSimpleDiagnostic(
            psiSourceElement,
            severity,
            factory as FirDiagnosticFactory0<PsiElement>
        )

        is FirLightDiagnosticWithParameters1<*> -> FirPsiDiagnosticWithParameters1(
            psiSourceElement,
            a,
            severity,
            factory as FirDiagnosticFactory1<PsiElement, Any>
        )

        is FirLightDiagnosticWithParameters2<*, *> -> FirPsiDiagnosticWithParameters2(
            psiSourceElement,
            a, b,
            severity,
            factory as FirDiagnosticFactory2<PsiElement, Any, Any>
        )

        is FirLightDiagnosticWithParameters3<*, *, *> -> FirPsiDiagnosticWithParameters3(
            psiSourceElement,
            a, b, c,
            severity,
            factory as FirDiagnosticFactory3<PsiElement, Any, Any, Any>
        )
        else -> error("Unknown diagnostic type ${this::class.simpleName}")
    }
}

private object CheckersFactory {
    private val extendedDeclarationCheckers = createDeclarationCheckers(ExtendedDeclarationCheckers)
    private val commonDeclarationCheckers = createDeclarationCheckers(CommonDeclarationCheckers)

    fun createDeclarationCheckers(useExtendedCheckers: Boolean): DeclarationCheckers =
        if (useExtendedCheckers) extendedDeclarationCheckers else commonDeclarationCheckers

    fun createExpressionCheckers(useExtendedCheckers: Boolean): ExpressionCheckers =
        if (useExtendedCheckers) ExtendedExpressionCheckers else CommonExpressionCheckers

    fun createTypeCheckers(useExtendedCheckers: Boolean): TypeCheckers = CommonTypeCheckers

    // TODO hack to have all checkers present in DeclarationCheckers.memberDeclarationCheckers and similar
    // If use ExtendedDeclarationCheckers directly when DeclarationCheckers.memberDeclarationCheckers will not contain basicDeclarationCheckers
    @OptIn(SessionConfiguration::class)
    private fun createDeclarationCheckers(declarationCheckers: DeclarationCheckers): DeclarationCheckers =
        CheckersComponent().apply { register(declarationCheckers) }.declarationCheckers

}
