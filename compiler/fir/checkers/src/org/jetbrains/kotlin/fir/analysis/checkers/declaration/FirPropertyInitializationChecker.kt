/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

object FirPropertyInitializationChecker : FirClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        val declaredLater = mutableSetOf<FirPropertySymbol>()
        val visitor = object : FirVisitorVoid() {
            override fun visitElement(element: FirElement) = element.acceptChildren(this)

            override fun visitConstructor(constructor: FirConstructor) {}

            override fun visitNamedFunction(namedFunction: FirNamedFunction) {}

            override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {}

            override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
                // TODO? if (anonymousFunction.invocationKind.isInPlace) visitElement(anonymousFunction)
            }

            override fun visitRegularClass(regularClass: FirRegularClass) {}

            override fun visitEnumEntry(enumEntry: FirEnumEntry) {}

            override fun visitVariableAssignment(variableAssignment: FirVariableAssignment) {
                variableAssignment.acceptChildren(this)
                val propertySymbol = variableAssignment.calleeReference?.toResolvedCallableSymbol() as? FirPropertySymbol ?: return
                if (propertySymbol !in declaredLater) return
                if (declaration is FirAnonymousObject &&
                    !LanguageFeature.ForbidInitializationBeforeDeclarationInAnonymous.isEnabled()
                ) {
                    reporter.reportOn(variableAssignment.lValue.source, FirErrors.INITIALIZATION_BEFORE_DECLARATION_WARNING, propertySymbol)
                } else {
                    reporter.reportOn(variableAssignment.lValue.source, FirErrors.INITIALIZATION_BEFORE_DECLARATION, propertySymbol)
                }
            }
        }

        @OptIn(DirectDeclarationsAccess::class)
        for (member in declaration.declarations.asReversed()) {
            if (declaredLater.isNotEmpty()) {
                member.accept(visitor)
            }
            if (member is FirProperty) {
                declaredLater.add(member.symbol)
            }
        }
    }
}
