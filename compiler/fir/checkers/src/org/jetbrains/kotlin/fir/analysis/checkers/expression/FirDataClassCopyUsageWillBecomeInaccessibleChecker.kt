/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.isClass
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.primaryConstructorSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.fir.visibilityChecker
import org.jetbrains.kotlin.resolve.DataClassResolver

object FirDataClassCopyUsageWillBecomeInaccessibleChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirFunctionCall && expression !is FirCallableReferenceAccess) return
        val copyFunction = (expression.calleeReference.symbol as? FirCallableSymbol)?.unwrapSubstitutionOverrides() ?: return
        val dataClass = copyFunction.dispatchReceiverType?.toRegularClassSymbol(context.session) ?: return
        if (copyFunction.isDataClassCopy(dataClass, context.session)) {
            val dataClassConstructor = dataClass.primaryConstructorSymbol(context.session) ?: return

            @OptIn(SymbolInternals::class)
            val hasCopyAlreadyBecameInaccessible = !context.session.visibilityChecker.isVisible(
                copyFunction.fir as? FirMemberDeclaration ?: return,
                context.session,
                context.containingFile ?: return,
                context.containingDeclarations,
                dispatchReceiver = null
            )
            if (hasCopyAlreadyBecameInaccessible) {
                return
            }

            @OptIn(SymbolInternals::class)
            val isConstructorVisible = context.session.visibilityChecker.isVisible(
                dataClassConstructor.fir,
                context.session,
                context.containingFile ?: return,
                context.containingDeclarations,
                dispatchReceiver = null
            )
            // We don't check the presence of @ExposedCopyVisibility annotations on purpose.
            // Even if the 'copy' is exposed, call-sites need to migrate.
            if (!isConstructorVisible) {
                reporter.reportOn(expression.calleeReference.source, FirErrors.DATA_CLASS_INVISIBLE_COPY_USAGE, context)
            }
        }
    }
}

private fun FirCallableSymbol<*>.isDataClassCopy(dataClass: FirRegularClassSymbol, session: FirSession): Boolean {
    if (!DataClassResolver.isCopy(name)) return false
    val constructor = dataClass.primaryConstructorSymbol(session)
    return this is FirNamedFunctionSymbol &&
            dataClass.isData &&
            dataClass.classKind.isClass &&
            resolvedReturnType.classId == dataClass.classId &&
            constructor != null &&
            resolvedContextReceivers.isEmpty() &&
            typeParameterSymbols.isEmpty() &&
            receiverParameter == null &&
            valueParameterSymbols.map { it.isVararg to it.resolvedReturnType } == constructor.valueParameterSymbols.map { it.isVararg to it.resolvedReturnType }
}
