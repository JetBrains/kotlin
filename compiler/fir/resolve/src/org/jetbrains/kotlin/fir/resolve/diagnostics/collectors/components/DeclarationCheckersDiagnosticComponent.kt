/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.components

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.resolve.diagnostics.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.resolve.diagnostics.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.resolve.diagnostics.collectors.AbstractDiagnosticCollector

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

    private fun <D : FirDeclaration> List<FirDeclarationChecker<D>>.check(declaration: D, reporter: DiagnosticReporter) {
        for (checker in this) {
            checker.check(declaration, reporter)
        }
    }
}