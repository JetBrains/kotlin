/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.*

class DeclarationCheckersDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    override fun visitProperty(property: FirProperty) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(property, it) }
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(regularClass, it) }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(simpleFunction, it) }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(typeAlias, it) }
    }

    override fun visitConstructor(constructor: FirConstructor) {
        runCheck { DeclarationCheckers.MEMBER_DECLARATIONS.check(constructor, it) }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(anonymousFunction, it) }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(propertyAccessor, it) }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(valueParameter, it) }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry) {
        runCheck { DeclarationCheckers.DECLARATIONS.check(enumEntry, it) }
    }

    private fun <D : FirDeclaration> List<FirDeclarationChecker<D>>.check(declaration: D, reporter: DiagnosticReporter) {
        for (checker in this) {
            checker.check(declaration, reporter)
        }
    }
}