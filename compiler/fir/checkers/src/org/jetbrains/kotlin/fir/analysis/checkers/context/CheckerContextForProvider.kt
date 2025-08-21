/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirInlineDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extra.FirAnonymousUnusedParamChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.SessionAndScopeSessionHolder
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirInlineBodyResolvableExpressionChecker
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator

/**
 * This class should be invisible for checkers, but should be only used by DiagnosticCollectorVisitor who runs those checkers
 * It contains all context-modification-related methods unlike CheckerContext that is assumed to be read-only
 */
abstract class CheckerContextForProvider(
    override val sessionHolder: SessionAndScopeSessionHolder,
    override val returnTypeCalculator: ReturnTypeCalculator,
    override val allInfosSuppressed: Boolean,
    override val allWarningsSuppressed: Boolean,
    override val allErrorsSuppressed: Boolean
) : CheckerContext() {
    abstract fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): CheckerContextForProvider

    abstract fun addDeclaration(declaration: FirDeclaration): CheckerContextForProvider

    abstract fun dropDeclaration()

    abstract fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: FirStatement): CheckerContextForProvider

    abstract fun dropCallOrAssignment()

    abstract fun addGetClassCall(getClassCall: FirGetClassCall): CheckerContextForProvider

    abstract fun dropGetClassCall()

    abstract fun addAnnotationContainer(annotationContainer: FirAnnotationContainer): CheckerContextForProvider

    abstract fun dropAnnotationContainer()

    abstract fun enterContractBody(): CheckerContextForProvider

    abstract fun exitContractBody(): CheckerContextForProvider

    abstract fun setInlineFunctionBodyContext(context: FirInlineDeclarationChecker.InlineFunctionBodyContext?): CheckerContextForProvider

    abstract fun setInlinableParameterContext(context: FirInlineBodyResolvableExpressionChecker.InlinableParameterContext?): CheckerContextForProvider

    abstract fun setLambdaBodyContext(context: FirAnonymousUnusedParamChecker.LambdaBodyContext?): CheckerContextForProvider

    abstract fun enterFile(file: FirFile): CheckerContextForProvider

    abstract fun exitFile(file: FirFile): CheckerContextForProvider

    abstract fun addElement(element: FirElement): CheckerContextForProvider

    abstract fun dropElement()
}
