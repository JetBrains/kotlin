/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.type

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@OptIn(CheckersComponentInternal::class)
class TypeCheckersDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    private val checkers: TypeCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: FirSession, reporter: DiagnosticReporter, mppKind: MppCheckerKind) : this(
        session,
        reporter,
        when (mppKind) {
            MppCheckerKind.Common -> session.checkersComponent.commonTypeCheckers
            MppCheckerKind.Platform -> session.checkersComponent.platformTypeCheckers
        }
    )

    override fun visitElement(element: FirElement, data: CheckerContext) {
        if (element is FirTypeRef) {
            error("${element::class.simpleName} should call parent checkers inside ${this::class.simpleName}")
        }
    }

    override fun visitTypeRef(typeRef: FirTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(typeRef, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: CheckerContext) {
        checkers.allResolvedTypeRefCheckers.check(resolvedTypeRef, data)
    }

    override fun visitFunctionTypeRef(functionTypeRef: FirFunctionTypeRef, data: CheckerContext) {
        checkers.allFunctionTypeRefCheckers.check(functionTypeRef, data)
    }

    override fun visitIntersectionTypeRef(intersectionTypeRef: FirIntersectionTypeRef, data: CheckerContext) {
        checkers.allIntersectionTypeRefCheckers.check(intersectionTypeRef, data)
    }

    override fun visitImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(implicitTypeRef, data)
    }

    override fun visitUnresolvedTypeRef(unresolvedTypeRef: FirUnresolvedTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(unresolvedTypeRef, data)
    }

    override fun visitUserTypeRef(userTypeRef: FirUserTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(userTypeRef, data)
    }

    override fun visitDynamicTypeRef(dynamicTypeRef: FirDynamicTypeRef, data: CheckerContext) {
        checkers.allTypeRefCheckers.check(dynamicTypeRef, data)
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: CheckerContext) {
        checkers.allResolvedTypeRefCheckers.check(errorTypeRef, data)
    }

    private inline fun <reified E : FirTypeRef> Array<FirTypeChecker<E>>.check(
        element: E,
        context: CheckerContext
    ) {
        for (checker in this) {
            try {
                checker.check(element, context, reporter)
            } catch (e: Exception) {
                rethrowExceptionWithDetails("Exception in type checkers", e) {
                    withFirEntry("element", element)
                    context.containingFilePath?.let { withEntry("file", it) }
                }
            }
        }
    }
}
