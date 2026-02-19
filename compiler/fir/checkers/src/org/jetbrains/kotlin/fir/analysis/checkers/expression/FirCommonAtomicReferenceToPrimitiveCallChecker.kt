/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkAtomicCallReceiverForStableIdentity
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.hasStableIdentityForAtomicOperations
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.withClassId

abstract class AbstractAtomicReferenceToPrimitiveCallChecker(
    val appropriateCandidatesForArgument: Map<ClassId, ClassId>,
    mppKind: MppCheckerKind,
    firstProblematicCallableId: CallableId,
    vararg remainingProblematicCallableIds: CallableId,
) : FirFunctionCallChecker(mppKind) {
    val problematicCallableIds: Set<CallableId> = setOf(firstProblematicCallableId, *remainingProblematicCallableIds)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        val callable = expression.calleeReference.resolved?.resolvedSymbol as? FirFunctionSymbol<*> ?: return
        val receiverType = expression.dispatchReceiver?.resolvedType?.fullyExpandedType() ?: return
        val atomicReferenceClassId = receiverType.classId ?: return
        val fullyExpandedCallableId = callable.callableId.withClassId(atomicReferenceClassId)

        if (fullyExpandedCallableId in problematicCallableIds) {
            checkAtomicCallReceiverForStableIdentity(
                receiverType, expression.source,
                atomicReferenceClassId, appropriateCandidatesForArgument
            )

            for ((argument, parameter) in expression.arguments.zip(callable.valueParameterSymbols)) {
                if (
                    !argument.resolvedType.hasStableIdentityForAtomicOperations &&
                    isDangerousAtomicCallParameterNameWithin(callable, parameter.name)
                ) {
                    reporter.reportOn(
                        source = argument.source,
                        factory = FirErrors.ATOMIC_REF_CALL_ARGUMENT_WITHOUT_CONSISTENT_IDENTITY,
                        argument.resolvedType,
                    )
                }
            }
        }
    }

    protected open fun isDangerousAtomicCallParameterNameWithin(function: FirFunctionSymbol<*>, name: Name): Boolean =
        name == Name.identifier("expectedValue")
                || name == Name.identifier("expected")
                || name == Name.identifier("newValue")
                || name == Name.identifier("expect")
                || name == Name.identifier("update")
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
