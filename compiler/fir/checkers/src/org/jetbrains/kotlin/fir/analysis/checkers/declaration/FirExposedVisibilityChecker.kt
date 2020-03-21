/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.onSource
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.firClassLike
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object FirExposedVisibilityChecker : FirDeclarationChecker<FirMemberDeclaration>() {
    override fun check(declaration: FirMemberDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration is FirTypeAlias) {
            checkTypeAlias(declaration, context, reporter)
        }
    }

    private fun checkTypeAlias(declaration: FirTypeAlias, context: CheckerContext, reporter: DiagnosticReporter) {
        val expandedType = declaration.expandedConeType

        val restricting = expandedType?.leastPermissiveDescriptor(declaration.session, declaration.effectiveVisibility)
        if (restricting != null) {
            reporter.reportExposure(Error.EXPOSED_TYPEALIAS_EXPANDED_TYPE, declaration.source)
        }
    }

    private enum class Error {
        EXPOSED_TYPEALIAS_EXPANDED_TYPE
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
        }
    }
}