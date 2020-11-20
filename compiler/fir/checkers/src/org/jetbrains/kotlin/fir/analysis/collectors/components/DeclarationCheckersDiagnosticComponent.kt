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

class DeclarationCheckersDiagnosticComponent(
    collector: AbstractDiagnosticCollector
) : AbstractDiagnosticCollectorComponent(collector) {
    private val checkers = session.checkersComponent.declarationCheckers

    override fun visitFile(file: FirFile, data: CheckerContext) {
        checkers.fileCheckers.check(file, data, reporter)
    }

    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        checkers.memberDeclarationCheckers.check(property, data, reporter)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        checkers.regularClassCheckers.check(regularClass, data, reporter)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        checkers.memberDeclarationCheckers.check(simpleFunction, data, reporter)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: CheckerContext) {
        checkers.memberDeclarationCheckers.check(typeAlias, data, reporter)
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        checkers.constructorCheckers.check(constructor, data, reporter)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        checkers.basicDeclarationCheckers.check(anonymousFunction, data, reporter)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        checkers.basicDeclarationCheckers.check(propertyAccessor, data, reporter)
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: CheckerContext) {
        checkers.basicDeclarationCheckers.check(valueParameter, data, reporter)
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: CheckerContext) {
        checkers.basicDeclarationCheckers.check(typeParameter, data, reporter)
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: CheckerContext) {
        checkers.basicDeclarationCheckers.check(enumEntry, data, reporter)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: CheckerContext) {
        checkers.basicDeclarationCheckers.check(anonymousObject, data, reporter)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: CheckerContext) {
        checkers.basicDeclarationCheckers.check(anonymousInitializer, data, reporter)
    }

    private fun <D : FirDeclaration> Collection<FirDeclarationChecker<D>>.check(
        declaration: D,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (checker in this) {
            checker.check(declaration, context, reporter)
        }
    }
}
