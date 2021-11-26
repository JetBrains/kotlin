/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirEnumEntry
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.utils.addToStdlib.lastIsInstanceOrNull

object FirEnumCompanionInEnumConstructorCallChecker : FirEnumEntryChecker() {
    override fun check(declaration: FirEnumEntry, context: CheckerContext, reporter: DiagnosticReporter) {
        val enumClass = context.containingDeclarations.lastIsInstanceOrNull<FirRegularClass>() ?: return
        if (enumClass.classKind != ClassKind.ENUM_CLASS) return
        val companionOfEnumSymbol = enumClass.companionObjectSymbol ?: return
        val initializerObject = (declaration.initializer as? FirAnonymousObjectExpression)?.anonymousObject ?: return
        val delegatingConstructorCall = initializerObject.primaryConstructorIfAny(context.session)?.resolvedDelegatedConstructorCall ?: return
        val visitor = Visitor(context, reporter, companionOfEnumSymbol)
        delegatingConstructorCall.argumentList.acceptChildren(visitor)
    }

    private class Visitor(
        val context: CheckerContext,
        val reporter: DiagnosticReporter,
        val companionSymbol: FirRegularClassSymbol
    ) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            val needVisitReceiver = checkQualifiedAccess(functionCall)
            functionCall.argumentList.acceptChildren(this)
            if (needVisitReceiver) {
                functionCall.explicitReceiver?.accept(this)
            }
        }

        override fun visitPropertyAccessExpression(propertyAccessExpression: FirPropertyAccessExpression) {
            val needVisitReceiver = checkQualifiedAccess(propertyAccessExpression)
            if (needVisitReceiver) {
                propertyAccessExpression.explicitReceiver?.accept(this)
            }
        }


        private fun checkQualifiedAccess(expression: FirQualifiedAccessExpression): Boolean {
            val extensionReceiver = expression.extensionReceiver
            if (extensionReceiver !is FirResolvedQualifier && extensionReceiver !is FirThisReceiverExpression) return true

            val receiverSymbol = expression.toResolvedCallableSymbol()
                ?.resolvedReceiverTypeRef
                ?.toRegularClassSymbol(context.session)
                ?: return true

            if (receiverSymbol == companionSymbol) {
                val source = expression.extensionReceiver.source ?: expression.source
                reporter.reportOn(source, FirErrors.UNINITIALIZED_ENUM_COMPANION, companionSymbol, context)
                return false
            }

            return true
        }
    }
}
