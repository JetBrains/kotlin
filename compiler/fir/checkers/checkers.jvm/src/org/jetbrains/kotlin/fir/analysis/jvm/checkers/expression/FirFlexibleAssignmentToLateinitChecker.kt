/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirVariableAssignmentChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.hasFlexibleMarkedNullability
import org.jetbrains.kotlin.fir.types.resolvedType

object FirFlexibleAssignmentToLateinitChecker : FirVariableAssignmentChecker(MppCheckerKind.Platform) {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!expression.rValue.resolvedType.hasFlexibleMarkedNullability) return
        val symbol = expression.lValue.toResolvedCallableSymbol(context.session) ?: return
        if (!symbol.isLateInit) return
        reporter.reportOn(expression.source, FirJvmErrors.PLATFORM_TYPE_ASSIGNMENT_TO_LATEINIT, expression.rValue.resolvedType, symbol, context)
    }

}