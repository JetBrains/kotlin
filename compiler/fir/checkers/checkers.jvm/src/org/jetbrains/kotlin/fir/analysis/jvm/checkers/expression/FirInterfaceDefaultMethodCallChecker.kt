/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.jvmDefaultMode
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.context.findClosest
import org.jetbrains.kotlin.fir.analysis.checkers.explicitReceiverIsNotSuperReference
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.isCompiledToJvmDefault
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.SpecialNames.ANONYMOUS_FQ_NAME

object FirInterfaceDefaultMethodCallChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (LanguageFeature.AllowSuperCallToJavaInterface.isEnabled()) return

        val symbol = expression.calleeReference.toResolvedCallableSymbol()
        val typeSymbol = symbol?.containingClassLookupTag()?.toRegularClassSymbol() ?: return
        if (typeSymbol.isLocal) return

        if (expression.explicitReceiverIsNotSuperReference()) return

        val containingDeclaration = context.findClosest<FirRegularClassSymbol>() ?: return

        val session = context.session

        val jvmDefaultMode = session.languageVersionSettings.jvmDefaultMode
        if (typeSymbol.isInterface &&
            (typeSymbol.origin is FirDeclarationOrigin.Java || symbol.isCompiledToJvmDefault(session, jvmDefaultMode))
        ) {
            if (containingDeclaration.isInterface) {
                val containingMember = context.findContainingMember()
                if (containingMember?.isCompiledToJvmDefault(session, jvmDefaultMode) == false) {
                    reporter.reportOn(expression.source, FirJvmErrors.INTERFACE_CANT_CALL_DEFAULT_METHOD_VIA_SUPER)
                    return
                }
            }
        }
    }

    private fun CheckerContext.findContainingMember(): FirCallableSymbol<*>? {
        return findClosest {
            (it is FirNamedFunctionSymbol && it.callableId.classId?.relativeClassName != ANONYMOUS_FQ_NAME) || it is FirPropertySymbol
        }
    }
}
