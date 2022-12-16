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
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.declarations.*

@OptIn(CheckersComponentInternal::class)
class DeclarationCheckersDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    checkers: DeclarationCheckers = session.checkersComponent.declarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    private val allFileCheckers = checkers.allFileCheckers.toList()
    private val allPropertyCheckers = checkers.allPropertyCheckers.toList()
    private val allClassCheckers = checkers.allClassCheckers.toList()
    private val allRegularClassCheckers = checkers.allRegularClassCheckers.toList()
    private val allSimpleFunctionCheckers = checkers.allSimpleFunctionCheckers.toList()
    private val allTypeAliasCheckers = checkers.allTypeAliasCheckers.toList()
    private val allConstructorCheckers = checkers.allConstructorCheckers.toList()
    private val allAnonymousFunctionCheckers = checkers.allAnonymousFunctionCheckers.toList()
    private val allPropertyAccessorCheckers = checkers.allPropertyAccessorCheckers.toList()
    private val allBackingFieldCheckers = checkers.allBackingFieldCheckers.toList()
    private val allValueParameterCheckers = checkers.allValueParameterCheckers.toList()
    private val allTypeParameterCheckers = checkers.allTypeParameterCheckers.toList()
    private val allEnumEntryCheckers = checkers.allEnumEntryCheckers.toList()
    private val allAnonymousObjectCheckers = checkers.allAnonymousObjectCheckers.toList()
    private val allAnonymousInitializerCheckers = checkers.allAnonymousInitializerCheckers.toList()

    override fun visitFile(file: FirFile, data: CheckerContext) {
        allFileCheckers.check(file, data)
    }

    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        allPropertyCheckers.check(property, data)
    }

    override fun visitClass(klass: FirClass, data: CheckerContext) {
        allClassCheckers.check(klass, data)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        allRegularClassCheckers.check(regularClass, data)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        allSimpleFunctionCheckers.check(simpleFunction, data)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: CheckerContext) {
        allTypeAliasCheckers.check(typeAlias, data)
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        allConstructorCheckers.check(constructor, data)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        allAnonymousFunctionCheckers.check(anonymousFunction, data)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        allPropertyAccessorCheckers.check(propertyAccessor, data)
    }

    override fun visitBackingField(backingField: FirBackingField, data: CheckerContext) {
        allBackingFieldCheckers.check(backingField, data)
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: CheckerContext) {
        allValueParameterCheckers.check(valueParameter, data)
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: CheckerContext) {
        allTypeParameterCheckers.check(typeParameter, data)
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: CheckerContext) {
        allEnumEntryCheckers.check(enumEntry, data)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: CheckerContext) {
        allAnonymousObjectCheckers.check(anonymousObject, data)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: CheckerContext) {
        allAnonymousInitializerCheckers.check(anonymousInitializer, data)
    }

    private fun <D : FirDeclaration> Collection<FirDeclarationChecker<D>>.check(
        declaration: D,
        context: CheckerContext
    ) {
        for (checker in this) {
            checker.check(declaration, context, reporter)
        }
    }
}
