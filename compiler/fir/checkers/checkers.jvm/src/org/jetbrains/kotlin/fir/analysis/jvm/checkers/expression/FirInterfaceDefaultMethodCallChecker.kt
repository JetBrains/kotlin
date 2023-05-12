/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.explicitReceiverIsNotSuperReference
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isCompiledToJvmDefault
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.java.jvmDefaultModeState
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.ANONYMOUS_CLASS_ID
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol

object FirInterfaceDefaultMethodCallChecker : FirQualifiedAccessExpressionChecker() {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val symbol = expression.calleeReference.toResolvedCallableSymbol()
        val classId = symbol?.callableId?.classId ?: return
        if (classId.isLocal) return

        if (expression.explicitReceiverIsNotSuperReference()) return

        val containingDeclaration = context.findClosest<FirRegularClass>() ?: return

        val session = context.session
        val typeSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol ?: return

        val jvmDefaultMode = session.jvmDefaultModeState
        if (typeSymbol.isInterface &&
            (typeSymbol.origin is FirDeclarationOrigin.Java || symbol.isCompiledToJvmDefault(session, jvmDefaultMode))
        ) {
            if (containingDeclaration.isInterface) {
                val containingMember = context.findContainingMember()?.symbol
                if (containingMember?.isCompiledToJvmDefault(session, jvmDefaultMode) == false) {
                    reporter.reportOn(expression.source, FirJvmErrors.INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER, context)
                    return
                }
            }
        }
    }

    private fun CheckerContext.findContainingMember(): FirCallableDeclaration? {
        return findClosest {
            (it is FirSimpleFunction && it.symbol.callableId.classId != ANONYMOUS_CLASS_ID) || it is FirProperty
        }
    }
}
