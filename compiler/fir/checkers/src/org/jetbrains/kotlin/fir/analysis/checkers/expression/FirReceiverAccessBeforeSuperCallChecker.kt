/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirImplicitInvokeCall
import org.jetbrains.kotlin.fir.expressions.FirInaccessibleReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.render

object FirReceiverAccessBeforeSuperCallChecker : FirInaccessibleReceiverChecker(MppCheckerKind.Common) {
    override fun check(expression: FirInaccessibleReceiverExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingCall = context.callsOrAssignments.last() as FirQualifiedAccessExpression
        containingCall.run {
            require(
                expression == dispatchReceiver ||
                        expression == extensionReceiver ||
                        expression in contextReceiverArguments ||
                        // Receiver can migrate here into an argument, see AbstractRawFirBuilder.convertFirSelector
                        this is FirImplicitInvokeCall && expression == arguments.first()
            ) {
                "Inaccessible receiver ${expression.render()} isn't found in receivers of a call/access ${this.render()}"
            }
        }
        reporter.reportOn(containingCall.calleeReference.source, FirErrors.INSTANCE_ACCESS_BEFORE_SUPER_CALL, "<this>", context)
    }
}
