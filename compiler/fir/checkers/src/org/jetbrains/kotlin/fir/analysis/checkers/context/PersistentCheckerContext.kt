/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.context

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.PersistentImplicitReceiverStack
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.calls.ImplicitReceiverValue
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.name.Name

class PersistentCheckerContext private constructor(
    override val implicitReceiverStack: PersistentImplicitReceiverStack,
    override val containingDeclarations: PersistentList<FirDeclaration<*>>,
    override val qualifiedAccessOrAnnotationCalls: PersistentList<FirStatement>,
    override val getClassCalls: PersistentList<FirGetClassCall>,
    override val sessionHolder: SessionHolder,
    override val returnTypeCalculator: ReturnTypeCalculator,
    override val suppressedDiagnostics: PersistentSet<String>,
    override val allInfosSuppressed: Boolean,
    override val allWarningsSuppressed: Boolean,
    override val allErrorsSuppressed: Boolean
) : CheckerContext() {
    constructor(sessionHolder: SessionHolder, returnTypeCalculator: ReturnTypeCalculator) : this(
        PersistentImplicitReceiverStack(),
        persistentListOf(),
        persistentListOf(),
        persistentListOf(),
        sessionHolder,
        returnTypeCalculator,
        persistentSetOf(),
        allInfosSuppressed = false,
        allWarningsSuppressed = false,
        allErrorsSuppressed = false
    )

    fun addImplicitReceiver(name: Name?, value: ImplicitReceiverValue<*>): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack.add(name, value),
            containingDeclarations,
            qualifiedAccessOrAnnotationCalls,
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    fun addDeclaration(declaration: FirDeclaration<*>): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations.add(declaration),
            qualifiedAccessOrAnnotationCalls,
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    fun addQualifiedAccessOrAnnotationCall(qualifiedAccessOrAnnotationCall: FirStatement): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            this.qualifiedAccessOrAnnotationCalls.add(qualifiedAccessOrAnnotationCall),
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    fun addGetClassCall(getClassCall: FirGetClassCall): PersistentCheckerContext {
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            qualifiedAccessOrAnnotationCalls,
            getClassCalls.add(getClassCall),
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics,
            allInfosSuppressed,
            allWarningsSuppressed,
            allErrorsSuppressed
        )
    }

    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): PersistentCheckerContext {
        if (diagnosticNames.isEmpty()) return this
        return PersistentCheckerContext(
            implicitReceiverStack,
            containingDeclarations,
            qualifiedAccessOrAnnotationCalls,
            getClassCalls,
            sessionHolder,
            returnTypeCalculator,
            suppressedDiagnostics.addAll(diagnosticNames),
            this.allInfosSuppressed || allInfosSuppressed,
            this.allWarningsSuppressed || allWarningsSuppressed,
            this.allErrorsSuppressed || allErrorsSuppressed
        )
    }
}
