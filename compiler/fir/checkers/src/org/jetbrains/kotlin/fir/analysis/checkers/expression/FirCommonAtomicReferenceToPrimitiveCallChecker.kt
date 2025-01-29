/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.reportAtomicToPrimitiveProblematicAccess
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.withClassId

abstract class AbstractAtomicReferenceToPrimitiveCallChecker(
    val appropriateCandidatesForArgument: Map<ClassId, ClassId>,
    mppKind: MppCheckerKind,
    firstProblematicCallableId: CallableId,
    vararg remainingProblematicCallableIds: CallableId,
) : FirFunctionCallChecker(mppKind) {
    val problematicCallableIds: Set<CallableId> = setOf(firstProblematicCallableId, *remainingProblematicCallableIds)

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val callable = expression.calleeReference.resolved?.resolvedSymbol as? FirFunctionSymbol<*> ?: return
        val receiverType = expression.dispatchReceiver?.resolvedType?.fullyExpandedType(context.session) ?: return
        val atomicReferenceClassId = receiverType.classId ?: return
        val fullyExpandedCallableId = callable.callableId.withClassId(atomicReferenceClassId)

        if (fullyExpandedCallableId in problematicCallableIds) {
            reportAtomicToPrimitiveProblematicAccess(
                receiverType, expression.source,
                atomicReferenceClassId, appropriateCandidatesForArgument,
                context, reporter
            )
        }
    }
}

object FirCommonAtomicReferenceToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(
        StandardClassIds.atomicByPrimitive,
        MppCheckerKind.Platform,
        StandardClassIds.Callables.atomicReferenceCompareAndSet,
        StandardClassIds.Callables.atomicReferenceCompareAndExchange,
    )

object FirCommonAtomicArrayToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(
        StandardClassIds.atomicArrayByPrimitive,
        MppCheckerKind.Platform,
        StandardClassIds.Callables.atomicArrayCompareAndSetAt,
        StandardClassIds.Callables.atomicArrayCompareAndExchangeAt,
    )
