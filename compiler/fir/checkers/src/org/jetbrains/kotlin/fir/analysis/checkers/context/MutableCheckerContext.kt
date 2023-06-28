/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirInlineDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.createInlineFunctionBodyContext
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.PersistentImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.name.Name

class MutableCheckerContext private constructor(
    override val implicitReceiverStack: PersistentImplicitReceiverStack,
    override val containingDeclarations: MutableList<FirDeclaration>,
    override val callsOrAssignments: MutableList<FirStatement>,
    override val getClassCalls: MutableList<FirGetClassCall>,
    override val annotationContainers: MutableList<FirAnnotationContainer>,
    override val containingElements: MutableList<FirElement>,
    override var isContractBody: Boolean,
    override var inlineFunctionBodyContext: FirInlineDeclarationChecker.InlineFunctionBodyContext?,
    override var containingFile: FirFile?,
    sessionHolder: SessionHolder,
    returnTypeCalculator: ReturnTypeCalculator,
    override val suppressedDiagnostics: PersistentSet<String>,
    allInfosSuppressed: Boolean,
    allWarningsSuppressed: Boolean,
    allErrorsSuppressed: Boolean
) : CheckerContextForProvider(sessionHolder, returnTypeCalculator, allInfosSuppressed, allWarningsSuppressed, allErrorsSuppressed) {
    constructor(sessionHolder: SessionHolder, returnTypeCalculator: ReturnTypeCalculator) : this(
        PersistentImplicitReceiverStack(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        isContractBody = false,
        inlineFunctionBodyContext = null,
        containingFile = null,
        sessionHolder,
        returnTypeCalculator,
        persistentSetOf(),
        allInfosSuppressed = false,
        allWarningsSuppressed = false,
        allErrorsSuppressed = false
    )

    override fun addImplicitReceiver(name: Name?, value: ImplicitReceiverValue<*>): MutableCheckerContext {
        return MutableCheckerContext(
            implicitReceiverStack.add(name, value),
            containingDeclarations,
            callsOrAssignments,
            getClassCalls,
            annotationContainers,
            containingElements,
            isContractBody,
            inlineFunctionBodyContext,
            containingFile,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    override fun addDeclaration(declaration: FirDeclaration): MutableCheckerContext {
        containingDeclarations.add(declaration)
        return this
    }

    override fun dropDeclaration() {
        containingDeclarations.removeLast()
    }

    override fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: FirStatement): MutableCheckerContext {
        callsOrAssignments.add(qualifiedAccessOrAnnotationCall)
        return this
    }

    override fun dropCallOrAssignment() {
        callsOrAssignments.removeLast()
    }

    override fun addGetClassCall(getClassCall: FirGetClassCall): MutableCheckerContext {
        getClassCalls.add(getClassCall)
        return this
    }

    override fun dropGetClassCall() {
        getClassCalls.removeLast()
    }

    override fun addAnnotationContainer(annotationContainer: FirAnnotationContainer): CheckerContextForProvider {
        annotationContainers.add(annotationContainer)
        return this
    }

    override fun dropAnnotationContainer() {
        annotationContainers.removeLast()
    }

    override fun addElement(element: FirElement): CheckerContextForProvider {
        assert(containingElements.lastOrNull() !== element)
        containingElements.add(element)
        return this
    }

    override fun dropElement() {
        containingElements.removeLast()
    }

    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): CheckerContextForProvider {
        if (diagnosticNames.isEmpty()) return this
        return MutableCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            callsOrAssignments,
            getClassCalls,
            annotationContainers,
            containingElements,
            isContractBody,
            inlineFunctionBodyContext,
            containingFile,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics.addAll(diagnosticNames),
            this.allInfosSuppressed || allInfosSuppressed,
            this.allWarningsSuppressed || allWarningsSuppressed,
            this.allErrorsSuppressed || allErrorsSuppressed
        )
    }

    override fun enterContractBody(): CheckerContextForProvider {
        check(!isContractBody)
        isContractBody = true
        return this
    }

    override fun exitContractBody(): CheckerContextForProvider {
        check(isContractBody)
        isContractBody = false
        return this
    }

    override fun setInlineFunctionBodyContext(context: FirInlineDeclarationChecker.InlineFunctionBodyContext): CheckerContextForProvider {
        inlineFunctionBodyContext = context
        return this
    }

    override fun unsetInlineFunctionBodyContext(): CheckerContextForProvider {
        inlineFunctionBodyContext = null
        return this
    }

    override fun enterFile(file: FirFile): CheckerContextForProvider {
        containingFile = file
        return this
    }

    override fun exitFile(file: FirFile): CheckerContextForProvider {
        containingFile = file
        return this
    }
}
