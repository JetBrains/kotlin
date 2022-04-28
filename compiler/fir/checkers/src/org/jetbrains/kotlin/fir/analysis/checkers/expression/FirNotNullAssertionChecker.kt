/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirCheckNotNullCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

object FirNotNullAssertionChecker : FirCheckNotNullCallChecker() {
    override fun check(expression: FirCheckNotNullCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val argument = expression.argumentList.arguments.singleOrNull() ?: return
        if (argument is FirAnonymousFunctionExpression && argument.anonymousFunction.isLambda) {
            reporter.reportOn(expression.source, FirErrors.NOT_NULL_ASSERTION_ON_LAMBDA_EXPRESSION, context)
            return
        }
        if (argument is FirCallableReferenceAccess) {
            reporter.reportOn(expression.source, FirErrors.NOT_NULL_ASSERTION_ON_CALLABLE_REFERENCE, context)
            return
        }
        // TODO: use of Unit is subject to change.
        //  See BodyResolveComponents.typeForQualifier in ResolveUtils.kt which returns Unit for no value type.
        if (argument is FirResolvedQualifier && argument.typeRef.isUnit) {
            // Would be reported as NO_COMPANION_OBJECT
            return
        }

        val type = argument.typeRef.coneType.fullyExpandedType(context.session)

        if (!type.canBeNull && context.languageVersionSettings.supportsFeature(LanguageFeature.EnableDfaWarningsInK2)) {
            reporter.reportOn(expression.source, FirErrors.UNNECESSARY_NOT_NULL_ASSERTION, type, context)
        }
    }
}
