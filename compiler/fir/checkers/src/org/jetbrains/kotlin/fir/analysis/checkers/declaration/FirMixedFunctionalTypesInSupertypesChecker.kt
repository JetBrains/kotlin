/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind.Function
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind.SuspendFunction
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.lookupSuperTypes
import org.jetbrains.kotlin.fir.types.functionTypeKind

sealed class FirMixedFunctionalTypesInSupertypesChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirMixedFunctionalTypesInSupertypesChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirMixedFunctionalTypesInSupertypesChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val superKinds = lookupSuperTypes(declaration.symbol, lookupInterfaces = true, deep = true, context.session)
            .mapNotNullTo(mutableSetOf()) { it.functionTypeKind(context.session)?.nonReflectKind() }

        when {
            superKinds.size <= 1 -> {}
            superKinds == setOf(Function, SuspendFunction) -> {
                reporter.reportOn(declaration.source, FirErrors.MIXING_SUSPEND_AND_NON_SUSPEND_SUPERTYPES, context)
            }
            else -> {
                reporter.reportOn(declaration.source, FirErrors.MIXING_FUNCTIONAL_KINDS_IN_SUPERTYPES, superKinds, context)
            }
        }
    }
}
