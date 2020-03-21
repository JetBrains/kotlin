/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.onSource
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object FirExposedVisibilityChecker : FirDeclarationChecker<FirMemberDeclaration>() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        when (declaration) {
            is FirTypeAlias -> checkTypeAlias(declaration, context, reporter)
            is FirProperty -> checkProperty(declaration, context, reporter)
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        val expandedType = declaration.expandedConeType

        val restricting = expandedType?.leastPermissiveDescriptor(declaration.session, declaration.effectiveVisibility)
        if (restricting != null) {
            reporter.reportExposure(Error.EXPOSED_TYPEALIAS_EXPANDED_TYPE, declaration.source)
        }
    }

    private fun checkProperty(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val propertyVisibility = declaration.effectiveVisibility
        val restricting =
            declaration.returnTypeRef.coneTypeSafe<ConeClassLikeType>()?.leastPermissiveDescriptor(declaration.session, propertyVisibility)
        if (restricting != null) {
            reporter.reportExposure(Error.EXPOSED_PROPERTY_TYPE, declaration.source)
        }
        checkMemberReceiver(declaration.receiverTypeRef?.coneTypeSafe(), declaration, reporter)
    }

    private fun checkMemberReceiver(
        typeRef: ConeClassLikeType?,
        memberDeclaration: FirCallableMemberDeclaration<*>, reporter: DiagnosticReporter
    ) {
        if (typeRef == null) return
        val receiverParameterType = memberDeclaration.receiverTypeRef?.coneTypeSafe<ConeClassLikeType>()
        val memberVisibility = memberDeclaration.effectiveVisibility
        val restricting = receiverParameterType?.leastPermissiveDescriptor(memberDeclaration.session, memberVisibility)
        if (restricting != null) {
            reporter.reportExposure(Error.EXPOSED_RECEIVER_TYPE, memberDeclaration.source)
        }
    }

    private enum class Error {
        EXPOSED_TYPEALIAS_EXPANDED_TYPE,
        EXPOSED_PROPERTY_TYPE,
        EXPOSED_RECEIVER_TYPE,
    }

    private fun DiagnosticReporter.reportExposure(
        error: Error,
        source: FirSourceElement?
    ) {
        when (error) {
            Error.EXPOSED_TYPEALIAS_EXPANDED_TYPE -> {
                source?.let {
                    report(Errors.FIR_EXPOSED_TYPEALIAS_EXPANDED_TYPE.onSource(it))
                }
            }
            Error.EXPOSED_PROPERTY_TYPE -> {
                source?.let {
                    report(Errors.FIR_EXPOSED_TYPEALIAS_EXPANDED_TYPE.onSource(it))
                }
            }
            Error.EXPOSED_RECEIVER_TYPE -> {
                source?.let {
                    report(Errors.FIR_EXPOSED_RECEIVER_TYPE.onSource(it))
                }
            }
        }
    }
}