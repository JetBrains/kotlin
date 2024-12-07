/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirInlineDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.extra.FirAnonymousUnusedParamChecker
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator

class PersistentCheckerContext private constructor(
    override val containingDeclarations: PersistentList<FirDeclaration>,
    override val callsOrAssignments: PersistentList<FirStatement>,
    override val getClassCalls: PersistentList<FirGetClassCall>,
    override val annotationContainers: PersistentList<FirAnnotationContainer>,
    override val containingElements: PersistentList<FirElement>,
    override val isContractBody: Boolean,
    override val inlineFunctionBodyContext: FirInlineDeclarationChecker.InlineFunctionBodyContext?,
    override val lambdaBodyContext: FirAnonymousUnusedParamChecker.LambdaBodyContext?,
    sessionHolder: SessionHolder,
    returnTypeCalculator: ReturnTypeCalculator,
    override val suppressedDiagnostics: PersistentSet<String>,
    allInfosSuppressed: Boolean,
    allWarningsSuppressed: Boolean,
    allErrorsSuppressed: Boolean,
    override val containingFile: FirFile?,
) : CheckerContextForProvider(sessionHolder, returnTypeCalculator, allInfosSuppressed, allWarningsSuppressed, allErrorsSuppressed) {
    constructor(sessionHolder: SessionHolder, returnTypeCalculator: ReturnTypeCalculator) : this(
        containingDeclarations = persistentListOf(),
        callsOrAssignments = persistentListOf(),
        getClassCalls = persistentListOf(),
        annotationContainers = persistentListOf(),
        containingElements = persistentListOf(),
        isContractBody = false,
        inlineFunctionBodyContext = null,
        lambdaBodyContext = null,
        sessionHolder,
        returnTypeCalculator,
        suppressedDiagnostics = getGloballySuppressedDiagnostics(sessionHolder.session),
        allInfosSuppressed = false,
        allWarningsSuppressed = false,
        allErrorsSuppressed = false,
        containingFile = null,
    )

    override fun addDeclaration(declaration: FirDeclaration): PersistentCheckerContext =
        copy(containingDeclarations = containingDeclarations.add(declaration))

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
        containingDeclarations: PersistentList<FirDeclaration> = this.containingDeclarations,
        isContractBody: Boolean = this.isContractBody,
        inlineFunctionBodyContext: FirInlineDeclarationChecker.InlineFunctionBodyContext? = this.inlineFunctionBodyContext,
        lambdaBodyContext: FirAnonymousUnusedParamChecker.LambdaBodyContext? = this.lambdaBodyContext,
        allInfosSuppressed: Boolean = this.allInfosSuppressed,
        allWarningsSuppressed: Boolean = this.allWarningsSuppressed,
        allErrorsSuppressed: Boolean = this.allErrorsSuppressed,
        suppressedDiagnostics: PersistentSet<String> = this.suppressedDiagnostics,
        containingFile: FirFile? = this.containingFile,
    ): PersistentCheckerContext {
        return PersistentCheckerContext(
            containingDeclarations,
            qualifiedAccessOrAssignmentsOrAnnotationCalls,
            getClassCalls,
            annotationContainers,
            containingElements,
            isContractBody,
            inlineFunctionBodyContext,
            lambdaBodyContext,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed, allWarningsSuppressed, allErrorsSuppressed, containingFile,
        )
    }

    private fun toggleContractBody(newValue: Boolean): CheckerContextForProvider {
        check(isContractBody != newValue)

        return copy(isContractBody = newValue)
    }

    override fun enterContractBody(): CheckerContextForProvider = toggleContractBody(newValue = true)

    override fun exitContractBody(): CheckerContextForProvider = toggleContractBody(newValue = false)

    override fun setInlineFunctionBodyContext(context: FirInlineDeclarationChecker.InlineFunctionBodyContext): PersistentCheckerContext =
        copy(inlineFunctionBodyContext = context)

    override fun unsetInlineFunctionBodyContext(): CheckerContextForProvider = copy(inlineFunctionBodyContext = null)

    override fun setLambdaBodyContext(context: FirAnonymousUnusedParamChecker.LambdaBodyContext): CheckerContextForProvider =
        copy(lambdaBodyContext = context)

    override fun unsetLambdaBodyContext(): CheckerContextForProvider = copy(lambdaBodyContext = null)

    override fun enterFile(file: FirFile): CheckerContextForProvider = copy(containingFile = file)

    override fun exitFile(file: FirFile): CheckerContextForProvider = copy(containingFile = null)
}
