/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.CheckersComponentInternal
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkersComponent
import org.jetbrains.kotlin.fir.analysis.collectors.components.AbstractDiagnosticCollectorComponent
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.utils.exceptions.rethrowExceptionWithDetails

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

@OptIn(CheckersComponentInternal::class)
class DeclarationCheckersDiagnosticComponent(
    session: FirSession,
    reporter: DiagnosticReporter,
    private val checkers: DeclarationCheckers,
) : AbstractDiagnosticCollectorComponent(session, reporter) {
    constructor(session: FirSession, reporter: DiagnosticReporter, mppKind: MppCheckerKind) : this(
        session,
        reporter,
        when (mppKind) {
            MppCheckerKind.Common -> session.checkersComponent.commonDeclarationCheckers
            MppCheckerKind.Platform -> session.checkersComponent.platformDeclarationCheckers
        }
    )

    override fun visitElement(element: FirElement, data: CheckerContext) {
        if (element is FirDeclaration) {
            error("${element::class.simpleName} should call parent checkers inside ${this::class.simpleName}")
        }
    }

    override fun visitDeclaration(declaration: FirDeclaration, data: CheckerContext) {
        checkers.allBasicDeclarationCheckers.check(declaration, data)
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: CheckerContext) {
        checkers.allSimpleFunctionCheckers.check(simpleFunction, data)
    }

    override fun visitProperty(property: FirProperty, data: CheckerContext) {
        checkers.allPropertyCheckers.check(property, data)
    }

    override fun visitClass(klass: FirClass, data: CheckerContext) {
        checkers.allClassCheckers.check(klass, data)
    }

    override fun visitRegularClass(regularClass: FirRegularClass, data: CheckerContext) {
        checkers.allRegularClassCheckers.check(regularClass, data)
    }

    override fun visitConstructor(constructor: FirConstructor, data: CheckerContext) {
        checkers.allConstructorCheckers.check(constructor, data)
    }

    override fun visitFile(file: FirFile, data: CheckerContext) {
        checkers.allFileCheckers.check(file, data)
    }

    override fun visitScript(script: FirScript, data: CheckerContext) {
        checkers.allScriptCheckers.check(script, data)
    }

    override fun visitReplSnippet(replSnippet: FirReplSnippet, data: CheckerContext) {
        checkers.allReplSnippetCheckers.check(replSnippet, data)
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: CheckerContext) {
        checkers.allTypeParameterCheckers.check(typeParameter, data)
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: CheckerContext) {
        checkers.allTypeAliasCheckers.check(typeAlias, data)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: CheckerContext) {
        checkers.allAnonymousFunctionCheckers.check(anonymousFunction, data)
    }

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: CheckerContext) {
        checkers.allPropertyAccessorCheckers.check(propertyAccessor, data)
    }

    override fun visitBackingField(backingField: FirBackingField, data: CheckerContext) {
        checkers.allBackingFieldCheckers.check(backingField, data)
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: CheckerContext) {
        checkers.allValueParameterCheckers.check(valueParameter, data)
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: CheckerContext) {
        checkers.allEnumEntryCheckers.check(enumEntry, data)
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: CheckerContext) {
        checkers.allAnonymousObjectCheckers.check(anonymousObject, data)
    }

    override fun visitAnonymousInitializer(anonymousInitializer: FirAnonymousInitializer, data: CheckerContext) {
        checkers.allAnonymousInitializerCheckers.check(anonymousInitializer, data)
    }

    override fun visitReceiverParameter(receiverParameter: FirReceiverParameter, data: CheckerContext) {
        checkers.allReceiverParameterCheckers.check(receiverParameter, data)
    }

    override fun visitDanglingModifierList(danglingModifierList: FirDanglingModifierList, data: CheckerContext) {
        checkers.allBasicDeclarationCheckers.check(danglingModifierList, data)
    }

    override fun visitCodeFragment(codeFragment: FirCodeFragment, data: CheckerContext) {
        checkers.allBasicDeclarationCheckers.check(codeFragment, data)
    }

    override fun visitField(field: FirField, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(field, data)
    }

    override fun visitErrorProperty(errorProperty: FirErrorProperty, data: CheckerContext) {
        checkers.allCallableDeclarationCheckers.check(errorProperty, data)
    }

    override fun visitErrorPrimaryConstructor(errorPrimaryConstructor: FirErrorPrimaryConstructor, data: CheckerContext) {
        checkers.allConstructorCheckers.check(errorPrimaryConstructor, data)
    }

    private inline fun <reified E : FirDeclaration> Array<FirDeclarationChecker<E>>.check(
        element: E,
        context: CheckerContext
    ) {
        for (checker in this) {
            try {
                checker.check(element, context, reporter)
            } catch (e: Exception) {
                rethrowExceptionWithDetails("Exception in declaration checkers", e) {
                    withFirEntry("element", element)
                    context.containingFilePath?.let { withEntry("file", it) }
                }
            }
        }
    }
}
