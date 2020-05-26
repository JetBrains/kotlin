/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.collectors.AbstractDiagnosticCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.*

class DeclarationCheckersDiagnosticComponent(collector: AbstractDiagnosticCollector) : AbstractDiagnosticCollectorComponent(collector) {
    private val checkers = session.checkersComponent.declarationCheckers

    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        runCheck { checkers.memberDeclarationCheckers.check(property, data, it) }
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        runCheck { checkers.memberDeclarationCheckers.check(regularClass, data, it) }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        runCheck { checkers.memberDeclarationCheckers.check(simpleFunction, data, it) }
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: CheckerContext) {
        runCheck { checkers.memberDeclarationCheckers.check(typeAlias, data, it) }
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        runCheck { checkers.constructorCheckers.check(constructor, data, it) }
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        runCheck { checkers.declarationCheckers.check(anonymousFunction, data, it) }
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        runCheck { checkers.declarationCheckers.check(propertyAccessor, data, it) }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: CheckerContext) {
        runCheck { checkers.declarationCheckers.check(valueParameter, data, it) }
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: CheckerContext) {
        runCheck { checkers.declarationCheckers.check(typeParameter, data, it) }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: CheckerContext) {
        runCheck { checkers.declarationCheckers.check(enumEntry, data, it) }
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: CheckerContext) {
        runCheck { checkers.declarationCheckers.check(anonymousObject, data, it) }
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: CheckerContext) {
        runCheck { checkers.declarationCheckers.check(anonymousInitializer, data, it) }
    }

    private fun <D : FirDeclaration> List<FirDeclarationChecker<D>>.check(
        declaration: D,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (checker in this) {
            checker.check(declaration, context, reporter)
        }
    }
}