/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.collectors.components

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.*

@OptIn(CheckersComponentInternal::class)
class DeclarationCheckersDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    private val checkers: DeclarationCheckers = session.checkersComponent.declarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {

    override fun visitFile(file: FirFile, data: CheckerContext) {
        checkers.allFileCheckers.check(file, data, reporter)
    }

    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        checkers.allPropertyCheckers.check(property, data, reporter)
    }

    override fun <F : FirClass<F>> visitClass(klass: FirClass<F>, data: CheckerContext) {
        checkers.allClassCheckers.check(klass, data, reporter)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        checkers.allRegularClassCheckers.check(regularClass, data, reporter)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        checkers.allSimpleFunctionCheckers.check(simpleFunction, data, reporter)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: CheckerContext) {
        checkers.allMemberDeclarationCheckers.check(typeAlias, data, reporter)
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        checkers.allConstructorCheckers.check(constructor, data, reporter)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        checkers.allAnnotatedDeclarationCheckers.check(anonymousFunction, data, reporter)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        checkers.allAnnotatedDeclarationCheckers.check(propertyAccessor, data, reporter)
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: CheckerContext) {
        checkers.allAnnotatedDeclarationCheckers.check(valueParameter, data, reporter)
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: CheckerContext) {
        checkers.allTypeParameterCheckers.check(typeParameter, data, reporter)
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: CheckerContext) {
        checkers.allBasicDeclarationCheckers.check(enumEntry, data, reporter)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: CheckerContext) {
        checkers.allClassCheckers.check(anonymousObject, data, reporter)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: CheckerContext) {
        checkers.allBasicDeclarationCheckers.check(anonymousInitializer, data, reporter)
    }

    private fun <D : FirDeclaration<*>> Collection<FirDeclarationChecker<D>>.check(
        declaration: D,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        for (checker in this) {
            checker.check(declaration, context, reporter)
        }
    }
}
