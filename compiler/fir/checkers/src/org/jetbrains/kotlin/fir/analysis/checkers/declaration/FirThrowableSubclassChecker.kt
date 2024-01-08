/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeOfThrowable
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.types.ConeErrorType

object FirThrowableSubclassChecker : FirClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasThrowableSupertype(context))
            return

        if (declaration.typeParameters.isNotEmpty()) {
            declaration.typeParameters.firstOrNull()?.source?.let {
                reporter.reportOn(it, FirErrors.GENERIC_THROWABLE_SUBCLASS, context)
            }

            val shouldReport = when (declaration) {
                is FirRegularClass -> declaration.isInner || declaration.isLocal
                is FirAnonymousObject -> true
                else -> false
            }

            if (shouldReport) {
                reporter.reportOn(declaration.source, FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS, context)
            }
        } else if (declaration.hasGenericOuterDeclaration(context)) {
            reporter.reportOn(declaration.source, FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS, context)
        }
    }

    private fun FirClass.hasThrowableSupertype(context: CheckerContext) =
        superConeTypes.any { it !is ConeErrorType && it.isSubtypeOfThrowable(context.session) }

    private fun FirClass.hasGenericOuterDeclaration(context: CheckerContext): Boolean {
        if (!classId.isLocal) return false
        for (containingDeclaration in context.containingDeclarations.asReversed()) {
            if (containingDeclaration is FirTypeParameterRefsOwner && containingDeclaration.typeParameters.isNotEmpty()) {
                return true
            }
            if (containingDeclaration is FirRegularClass && !containingDeclaration.isLocal && !containingDeclaration.isInner) {
                return false
            }
        }
        return false
    }
}
