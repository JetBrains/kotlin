/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.ConeDynamicType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe

object FirDynamicUnsupportedChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        // It's assumed this checker is only called
        // by within the platform that disallows dynamics
        if (declaration.source?.kind !is KtRealSourceElementKind) {
            return
        }

        when (declaration) {
            is FirVariable -> checkType(declaration.returnTypeRef, context, reporter)
            is FirFunction -> {
                checkType(declaration.returnTypeRef, context, reporter)
                declaration.receiverTypeRef?.let { checkType(it, context, reporter) }
            }
            is FirClass -> declaration.superTypeRefs.forEach {
                checkType(it, context, reporter)
            }
            is FirTypeAlias -> checkType(declaration.expandedTypeRef, context, reporter)
            else -> {}
        }
    }

    private fun checkType(typeRef: FirTypeRef, context: CheckerContext, reporter: DiagnosticReporter) {
        if (typeRef.coneTypeSafe<ConeDynamicType>() != null) {
            reporter.reportOn(typeRef.source, FirErrors.UNSUPPORTED, "dynamic type", context)
        }
    }
}
