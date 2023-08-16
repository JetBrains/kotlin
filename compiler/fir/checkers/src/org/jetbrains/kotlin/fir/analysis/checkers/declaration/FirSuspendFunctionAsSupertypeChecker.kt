/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.functions.isBasicFunctionOrKFunction
import org.jetbrains.kotlin.builtins.functions.isSuspendOrKSuspendFunction
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.types.functionTypeKind

object FirSuspendFunctionAsSupertypeChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val supertypes = lookupSuperTypes(declaration.symbol, lookupInterfaces = true, deep = true, context.session)
            .mapNotNull { it.functionTypeKind(context.session) }

        if (
            supertypes.any { it.isSuspendOrKSuspendFunction } &&
            supertypes.any { it.isBasicFunctionOrKFunction }
        ) {
            reporter.reportOn(declaration.source, FirErrors.MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES, context)
        }
    }
}
