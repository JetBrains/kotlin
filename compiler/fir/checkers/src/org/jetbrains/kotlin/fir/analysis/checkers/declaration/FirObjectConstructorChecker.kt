/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitor

object FirObjectConstructorChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.session.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitSelfCallsInNestedObjects))
            return

        if (declaration.classKind != ClassKind.OBJECT)
            return

        val objectSymbol = declaration.symbol
        // Temporary restriction till KT-66754 will be accepted
        if (objectSymbol.getContainingClassSymbol(context.session) != objectSymbol.getSuperClassSymbolOrAny(context.session))
            return

        objectSymbol.primaryConstructorSymbol(context.session)?.resolvedDelegatedConstructorCall
            ?.accept(objectRefVisitor, Data(objectSymbol, context, reporter))
    }

    private val objectRefVisitor: FirVisitor<Unit, Data> = object : FirVisitor<Unit, Data>() {
        override fun visitElement(element: FirElement, data: Data) {
            element.acceptChildren(this, data)
        }

        override fun visitThisReceiverExpression(thisReceiverExpression: FirThisReceiverExpression, data: Data) {
            if (thisReceiverExpression.calleeReference.boundSymbol == data.objectSymbol) {
                data.reporter.reportOn(thisReceiverExpression.source, SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR, data.context)
            }
        }

        override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: Data) {
            if (resolvedQualifier.symbol == data.objectSymbol) {
                data.reporter.reportOn(resolvedQualifier.source, SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR, data.context)
            } else if (resolvedQualifier.resolvedToCompanionObject) {
                val companionSymbol = resolvedQualifier.symbol?.fullyExpandedClass(data.context.session)?.companionObjectSymbol
                if (companionSymbol == data.objectSymbol) {
                    data.reporter.reportOn(resolvedQualifier.source, SELF_CALL_IN_NESTED_OBJECT_CONSTRUCTOR_ERROR, data.context)
                }
            }
        }

        override fun visitAnonymousObjectExpression(anonymousObjectExpression: FirAnonymousObjectExpression, data: Data) {
            // Temporary restriction till KT-66754 will be accepted.
            // After that, the potential object expression passed as an argument has to be checked in the similar manner.
            // However, we have to check if it doesn't use the initial reference [data.objectSymbol],
            // or the current self-reference and so on in the primary constructor.
            return
        }

        override fun visitGetClassCall(getClassCall: FirGetClassCall, data: Data) {
            // We allow referring the type of the object (`Bar.Companion::class.java`)
            return
        }

        override fun visitCallableReferenceAccess(callableReferenceAccess: FirCallableReferenceAccess, data: Data) {
            // We allow referring the function reference (`Bar.Companion::foo`)
            return
        }
    }

    private class Data(val objectSymbol: FirRegularClassSymbol, val context: CheckerContext, val reporter: DiagnosticReporter)
}