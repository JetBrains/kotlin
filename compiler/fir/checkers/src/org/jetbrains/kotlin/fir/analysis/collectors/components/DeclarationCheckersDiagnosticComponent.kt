/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.*

class DeclarationCheckersDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(property, data, it) }
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(regularClass, data, it) }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(simpleFunction, data, it) }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: CheckerContext) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(typeAlias, data, it) }
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(constructor, data, it) }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(anonymousFunction, data, it) }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(propertyAccessor, data, it) }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: CheckerContext) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(valueParameter, data, it) }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: CheckerContext) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(enumEntry, data, it) }
    }

    private fun <D : FirDeclaration> List<FirDeclarationChecker<D>>.check(declaration: D, context: CheckerContext, reporter: DiagnosticReporter) {
        for (checker in this) {
            checker.check(declaration, context, reporter)
        }
    }
}