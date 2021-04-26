/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.type.FirTypeChecker
import org.jetbrains.kotlin.fir.analysis.checkers.type.TypeCheckers
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.types.*

@OptIn(CheckersComponentInternal::class)
class TypeCheckersDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    private val checkers: TypeCheckers = session.checkersComponent.typeCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(dynamicTypeRef, data, reporter)
    }

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(functionTypeRef, data, reporter)
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(userTypeRef, data, reporter)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(resolvedTypeRef, data, reporter)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(errorTypeRef, data, reporter)
    }

    override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(typeRefWithNullability, data, reporter)
    }

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(implicitTypeRef, data, reporter)
    }

    override fun visitTypeRef(typeRef: FirTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(typeRef, data, reporter)
    }

    private fun <T : FirTypeRef> Collection<FirTypeChecker<T>>.check(
        typeRef: T,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (checker in this) {
            checker.check(typeRef, context, reporter)
        }
    }
}
