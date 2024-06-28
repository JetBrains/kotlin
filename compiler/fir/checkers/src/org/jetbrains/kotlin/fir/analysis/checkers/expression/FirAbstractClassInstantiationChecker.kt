/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirAbstractClassInstantiationChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val constructorSymbol = expression.calleeReference.toResolvedConstructorSymbol() ?: return
        val declarationClass = constructorSymbol.resolvedReturnTypeRef.coneType.toRegularClassSymbol(context.session) ?: return

        if (declarationClass.isAbstract && declarationClass.classKind == ClassKind.CLASS) {
            val source = when (expression) {
                is FirCallableReferenceAccess -> expression.calleeReference.source
                else -> expression.source
            }

            reporter.reportOn(source, FirErrors.CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, context)
        }
    }
}
