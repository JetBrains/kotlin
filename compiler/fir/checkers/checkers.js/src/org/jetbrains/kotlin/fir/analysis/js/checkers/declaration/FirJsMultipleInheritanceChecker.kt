/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.overriddenFunctions
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.isSubtypeOf
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions

sealed class FirJsMultipleInheritanceChecker(mppKind: MppCheckerKind) : FirClassChecker(mppKind) {
    object Regular : FirJsMultipleInheritanceChecker(MppCheckerKind.Platform) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    object ForExpectClass : FirJsMultipleInheritanceChecker(MppCheckerKind.Common) {
        override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
            if (!declaration.isExpect) return
            super.check(declaration, context, reporter)
        }
    }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.checkFunctionIfSubtypeOf(
            functionToCheck = OperatorNameConventions.GET,
            supertype = context.session.builtinTypes.charSequenceType.type,
            context, reporter,
        )

        declaration.checkFunctionIfSubtypeOf(
            functionToCheck = StandardNames.NEXT_CHAR,
            supertype = context.session.builtinTypes.charIteratorType.type,
            context, reporter,
        )
    }

    private fun FirClass.checkFunctionIfSubtypeOf(
        functionToCheck: Name,
        supertype: ConeKotlinType,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!defaultType().isSubtypeOf(supertype, context.session)) {
            return
        }

        val scope = unsubstitutedScope(context)
        val overridesWithSameName = scope.getFunctions(functionToCheck)

        for (function in overridesWithSameName) {
            val overridden = function.overriddenFunctions(symbol, context)
            if (
                overridden.size > 1 &&
                overridden.any { it.callableId.classId == supertype.classId }
            ) {
                reporter.reportOn(source, FirJsErrors.WRONG_MULTIPLE_INHERITANCE, function, context)
            }
        }
    }
}
