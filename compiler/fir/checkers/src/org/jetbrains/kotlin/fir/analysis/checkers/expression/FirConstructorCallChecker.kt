/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedConstructorSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol

object FirConstructorCallChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val constructorSymbol = expression.calleeReference.toResolvedConstructorSymbol() ?: return
        val declarationClass = constructorSymbol.resolvedReturnTypeRef.coneType.toRegularClassSymbol(context.session)
            ?: return

        if (declarationClass.classKind == ClassKind.ANNOTATION_CLASS &&
            context.callsOrAssignments.all { call ->
                call !is FirAnnotation
            } &&
            context.containingDeclarations.all { klass ->
                klass !is FirRegularClass || klass.classKind != ClassKind.ANNOTATION_CLASS
            }
        ) {
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.InstantiationOfAnnotationClasses)) reporter.reportOn(
                expression.source,
                FirErrors.ANNOTATION_CLASS_CONSTRUCTOR_CALL,
                context
            )
        }

        if (declarationClass.classKind == ClassKind.ENUM_CLASS) {
            reporter.reportOn(
                expression.source,
                FirErrors.ENUM_CLASS_CONSTRUCTOR_CALL,
                context
            )
        }
    }
}
