/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
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
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFileSymbol

class PersistentCheckerContext private constructor(
    override val containingDeclarations: PersistentList<FirBasedSymbol<*>>,
    override val callsOrAssignments: PersistentList<FirStatement>,
    override val getClassCalls: PersistentList<FirGetClassCall>,
    override val annotationContainers: PersistentList<FirAnnotationContainer>,
    override val containingElements: PersistentList<FirElement>,
    override val isContractBody: Boolean,
    override val inlineFunctionBodyContext: FirInlineDeclarationChecker.InlineFunctionBodyContext?,
    override val inlinableParameterContext: FirInlineBodyResolvableExpressionChecker.InlinableParameterContext?,
    override val lambdaBodyContext: FirAnonymousUnusedParamChecker.LambdaBodyContext?,
    sessionHolder: SessionAndScopeSessionHolder,
    returnTypeCalculator: ReturnTypeCalculator,
    override val suppressedDiagnostics: PersistentSet<String>,
    allInfosSuppressed: Boolean,
    allWarningsSuppressed: Boolean,
    allErrorsSuppressed: Boolean,
    override val containingFileSymbol: FirFileSymbol?,
) : CheckerContextForProvider(sessionHolder, returnTypeCalculator, allInfosSuppressed, allWarningsSuppressed, allErrorsSuppressed) {
    constructor(sessionHolder: SessionAndScopeSessionHolder, returnTypeCalculator: ReturnTypeCalculator) : this(
        containingDeclarations = persistentListOf(),
        callsOrAssignments = persistentListOf(),
        getClassCalls = persistentListOf(),
        annotationContainers = persistentListOf(),
        containingElements = persistentListOf(),
        isContractBody = false,
        inlineFunctionBodyContext = null,
        inlinableParameterContext = null,
        lambdaBodyContext = null,
        sessionHolder,
        returnTypeCalculator,
        suppressedDiagnostics = persistentSetOf(),
        allInfosSuppressed = false,
        allWarningsSuppressed = false,
        allErrorsSuppressed = false,
        containingFileSymbol = null,
    )

    override fun addDeclaration(declaration: FirDeclaration): PersistentCheckerContext =
        copy(containingDeclarations = containingDeclarations.add(declaration.symbol))

    override fun dropDeclaration() {}

    override fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: FirStatement): PersistentCheckerContext =
        copy(
            qualifiedAccessOrAssignmentsOrAnnotationCalls =
            callsOrAssignments.add(qualifiedAccessOrAnnotationCall)
        )

    override fun dropCallOrAssignment() {}

    override fun addGetClassCall(getClassCall: FirGetClassCall): PersistentCheckerContext =
        copy(getClassCalls = getClassCalls.add(getClassCall))

    override fun dropGetClassCall() {}

    override fun addAnnotationContainer(annotationContainer: FirAnnotationContainer): PersistentCheckerContext =
        copy(annotationContainers = annotationContainers.add(annotationContainer))

    override fun dropAnnotationContainer() {}

    override fun addElement(element: FirElement): PersistentCheckerContext =
        copy(containingElements = containingElements.add(element))

    override fun dropElement() {}

    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): CheckerContextForProvider {
        if (diagnosticNames.isEmpty()) return this
        return copy(
            suppressedDiagnostics = suppressedDiagnostics.addAll(diagnosticNames),
            allInfosSuppressed = this.allInfosSuppressed || allInfosSuppressed,
            allWarningsSuppressed = this.allWarningsSuppressed || allWarningsSuppressed,
            allErrorsSuppressed = this.allErrorsSuppressed || allErrorsSuppressed
        )
    }

    private fun copy(
        qualifiedAccessOrAssignmentsOrAnnotationCalls: PersistentList<FirStatement> = this.callsOrAssignments,
        getClassCalls: PersistentList<FirGetClassCall> = this.getClassCalls,
        annotationContainers: PersistentList<FirAnnotationContainer> = this.annotationContainers,
        containingElements: PersistentList<FirElement> = this.containingElements,
        containingDeclarations: PersistentList<FirBasedSymbol<*>> = this.containingDeclarations,
        isContractBody: Boolean = this.isContractBody,
        inlineFunctionBodyContext: FirInlineDeclarationChecker.InlineFunctionBodyContext? = this.inlineFunctionBodyContext,
        inlinableParameterContext: FirInlineBodyResolvableExpressionChecker.InlinableParameterContext? = this.inlinableParameterContext,
        lambdaBodyContext: FirAnonymousUnusedParamChecker.LambdaBodyContext? = this.lambdaBodyContext,
        allInfosSuppressed: Boolean = this.allInfosSuppressed,
        allWarningsSuppressed: Boolean = this.allWarningsSuppressed,
        allErrorsSuppressed: Boolean = this.allErrorsSuppressed,
        suppressedDiagnostics: PersistentSet<String> = this.suppressedDiagnostics,
        containingFileSymbol: FirFileSymbol? = this.containingFileSymbol,
    ): PersistentCheckerContext {
        return PersistentCheckerContext(
            containingDeclarations,
            qualifiedAccessOrAssignmentsOrAnnotationCalls,
            getClassCalls,
            annotationContainers,
            containingElements,
            isContractBody,
            inlineFunctionBodyContext,
            inlinableParameterContext,
            lambdaBodyContext,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed, allWarningsSuppressed, allErrorsSuppressed, containingFileSymbol,
        )
    }

    private fun toggleContractBody(newValue: Boolean): CheckerContextForProvider {
        check(isContractBody != newValue)

        return copy(isContractBody = newValue)
    }

    override fun enterContractBody(): CheckerContextForProvider = toggleContractBody(newValue = true)

    override fun exitContractBody(): CheckerContextForProvider = toggleContractBody(newValue = false)

    override fun setInlineFunctionBodyContext(context: FirInlineDeclarationChecker.InlineFunctionBodyContext?): PersistentCheckerContext =
        copy(inlineFunctionBodyContext = context)

    override fun setInlinableParameterContext(context: FirInlineBodyResolvableExpressionChecker.InlinableParameterContext?): CheckerContextForProvider =
        copy(inlinableParameterContext = context)

    override fun setLambdaBodyContext(context: FirAnonymousUnusedParamChecker.LambdaBodyContext?): CheckerContextForProvider =
        copy(lambdaBodyContext = context)

    override fun enterFile(file: FirFile): CheckerContextForProvider = copy(containingFileSymbol = file.symbol)

    override fun exitFile(file: FirFile): CheckerContextForProvider = copy(containingFileSymbol = null)
}
