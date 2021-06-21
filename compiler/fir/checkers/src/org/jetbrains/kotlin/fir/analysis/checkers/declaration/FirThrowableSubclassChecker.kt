/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSubtypeOfThrowable
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.types.ConeClassErrorType

object FirThrowableSubclassChecker : FirClassChecker() {
    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasThrowableSupertype(context))
            return

        if (declaration.typeParameters.isNotEmpty()) {
            reporter.reportOn(declaration.typeParameters.firstOrNull()?.source, FirErrors.GENERIC_THROWABLE_SUBCLASS, context)

            val source = when {
                (declaration as? FirRegularClass)?.isInner == true -> declaration.source
                declaration is FirAnonymousObject -> (declaration.declarations.firstOrNull())?.source
                else -> null
            }
            reporter.reportOn(source, FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS, context)
        } else if (declaration.hasGenericOuterDeclaration(context)) {
            reporter.reportOn(declaration.source, FirErrors.INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS, context)
        }
    }

    private fun FirClass<*>.hasThrowableSupertype(context: CheckerContext) =
        superConeTypes.any { it !is ConeClassErrorType && it.isSubtypeOfThrowable(context.session) }

    private fun FirClass<*>.hasGenericOuterDeclaration(context: CheckerContext) =
        classId.isLocal && context.containingDeclarations.anyIsGeneric()

    private fun Collection<FirDeclaration<*>>.anyIsGeneric() =
        any { it is FirTypeParameterRefsOwner && it.typeParameters.isNotEmpty() }
}
