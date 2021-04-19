/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClass
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.isAbstract
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.types.coneType

object FirConstructorCallChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val constructorSymbol =
            (expression.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirConstructorSymbol ?: return
        val declarationClass = constructorSymbol.fir.returnTypeRef.coneType.toRegularClass(context.session)

        if (declarationClass != null && declarationClass.isAbstract && declarationClass.classKind == ClassKind.CLASS) {
            reporter.reportOn(expression.source, FirErrors.CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS, context)
        }
    }
}