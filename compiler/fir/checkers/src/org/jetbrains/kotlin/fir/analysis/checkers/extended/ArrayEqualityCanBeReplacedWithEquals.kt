/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirBasicExpresionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS
import org.jetbrains.kotlin.fir.expressions.FirEqualityOperatorCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.symbols.StandardClassIds
import org.jetbrains.kotlin.fir.toFirPsiSourceElement
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

object ArrayEqualityCanBeReplacedWithEquals : FirBasicExpresionChecker() {
    override fun check(equality: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (equality !is FirEqualityOperatorCall) return
        if (equality.operation != FirOperation.EQ && equality.operation != FirOperation.NOT_EQ) return
        val left = equality.arguments.getOrNull(0) ?: return
        val right = equality.arguments.getOrNull(1) ?: return

        if (left.typeRef.coneType.classId != StandardClassIds.Array) return
        if (right.typeRef.coneType.classId != StandardClassIds.Array) return

        reporter.report(equality.psi?.children?.get(1)?.toFirPsiSourceElement(), ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_EQUALS)
    }
}