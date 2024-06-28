/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.AbstractFirUnnecessarySafeCallChecker
import org.jetbrains.kotlin.fir.expressions.FirSafeCallExpression
import org.jetbrains.kotlin.fir.java.enhancement.EnhancedForWarningConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.typeContext

object FirJavaUnnecessarySafeCallChecker : AbstractFirUnnecessarySafeCallChecker() {
    override fun check(expression: FirSafeCallExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val receiverType = EnhancedForWarningConeSubstitutor(context.session.typeContext)
            .substituteOrNull(expression.receiver.resolvedType)
            ?.fullyExpandedType(context.session) ?: return

        checkSafeCallReceiverType(receiverType, expression.source, context, reporter)
    }
}

