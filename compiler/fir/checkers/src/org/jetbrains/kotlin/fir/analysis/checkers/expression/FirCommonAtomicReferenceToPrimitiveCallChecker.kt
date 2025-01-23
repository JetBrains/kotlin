/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkAtomicReferenceAccess
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.resolved
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

abstract class AbstractAtomicReferenceToPrimitiveCallChecker(
    val atomicReferenceClassId: ClassId,
    mppKind: MppCheckerKind,
) : FirFunctionCallChecker(mppKind) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val callable = expression.calleeReference.resolved?.resolvedSymbol

        if (callable is FirConstructorSymbol) {
            checkAtomicReferenceAccess(expression.resolvedType, expression.source, atomicReferenceClassId, context, reporter)
        }
    }
}

object FirCommonAtomicReferenceToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(StandardClassIds.AtomicReference, MppCheckerKind.Platform)

object FirCommonAtomicArrayToPrimitiveCallChecker :
    AbstractAtomicReferenceToPrimitiveCallChecker(StandardClassIds.AtomicArray, MppCheckerKind.Platform)
