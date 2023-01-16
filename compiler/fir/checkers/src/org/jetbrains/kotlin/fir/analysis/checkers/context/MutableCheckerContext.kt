/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
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
    override val qualifiedAccessOrAssignmentsOrAnnotationCalls: MutableList<FirStatement>,
    override val getClassCalls: MutableList<FirGetClassCall>,
    override val annotationContainers: MutableList<FirAnnotationContainer>,
    override var isContractBody: Boolean,
    sessionHolder: SessionHolder,
    returnTypeCalculator: ReturnTypeCalculator,
    override val suppressedDiagnostics: PersistentSet<String>,
    allInfosSuppressed: Boolean,
    allWarningsSuppressed: Boolean,
    allErrorsSuppressed: Boolean
) : AbstractCheckerContext(sessionHolder, returnTypeCalculator, allInfosSuppressed, allWarningsSuppressed, allErrorsSuppressed) {
    constructor(sessionHolder: SessionHolder, returnTypeCalculator: ReturnTypeCalculator) : this(
        PersistentImplicitReceiverStack(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        mutableListOf(),
        isContractBody = false,
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
            qualifiedAccessOrAssignmentsOrAnnotationCalls,
            getClassCalls,
            annotationContainers,
            isContractBody,
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
        containingDeclarations.removeAt(containingDeclarations.size - 1)
    }

    override fun addQualifiedAccessOrAnnotationCall(qualifiedAccessOrAnnotationCall: FirStatement): MutableCheckerContext {
        qualifiedAccessOrAssignmentsOrAnnotationCalls.add(qualifiedAccessOrAnnotationCall)
        return this
    }

    override fun dropQualifiedAccessOrAnnotationCall() {
        qualifiedAccessOrAssignmentsOrAnnotationCalls.removeAt(qualifiedAccessOrAssignmentsOrAnnotationCalls.size - 1)
    }

    override fun addGetClassCall(getClassCall: FirGetClassCall): MutableCheckerContext {
        getClassCalls.add(getClassCall)
        return this
    }

    override fun dropGetClassCall() {
        getClassCalls.removeAt(getClassCalls.size - 1)
    }

    override fun addAnnotationContainer(annotationContainer: FirAnnotationContainer): CheckerContext {
        annotationContainers.add(annotationContainer)
        return this
    }

    override fun dropAnnotationContainer() {
        annotationContainers.removeAt(annotationContainers.size - 1)
    }

    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): MutableCheckerContext {
        if (diagnosticNames.isEmpty()) return this
        return MutableCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            qualifiedAccessOrAssignmentsOrAnnotationCalls,
            getClassCalls,
            annotationContainers,
            isContractBody,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics.addAll(diagnosticNames),
            this.allInfosSuppressed || allInfosSuppressed,
            this.allWarningsSuppressed || allWarningsSuppressed,
            this.allErrorsSuppressed || allErrorsSuppressed
        )
    }

    override fun enterContractBody(): CheckerContext {
        check(!isContractBody)
        isContractBody = true
        return this
    }

    override fun exitContractBody(): CheckerContext {
        check(isContractBody)
        isContractBody = false
        return this
    }
}
