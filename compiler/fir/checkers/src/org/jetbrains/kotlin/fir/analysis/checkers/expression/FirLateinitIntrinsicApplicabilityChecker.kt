/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLateInit
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.name.StandardClassIds.Annotations

object FirLateinitIntrinsicApplicabilityChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {

        val resolvedSymbol = expression.calleeReference.toResolvedPropertySymbol() ?: return

        // An optimization
        if (resolvedSymbol.name.identifierOrNullIfSpecial != "isInitialized") return

        val receiverParameter = resolvedSymbol.receiverParameter ?: return

        if (!receiverParameter.hasAnnotation(Annotations.AccessibleLateinitPropertyLiteral, context.session)) return

        val source = expression.calleeReference.source

        val extensionReceiver = expression.extensionReceiver
        if (extensionReceiver !is FirCallableReferenceAccess) {
            reporter.reportOn(source, FirErrors.LATEINIT_INTRINSIC_CALL_ON_NON_LITERAL, context)
            return
        }

        val calleePropertySymbol = extensionReceiver.calleeReference.toResolvedPropertySymbol() ?: return

        if (!calleePropertySymbol.isLateInit) {
            reporter.reportOn(source, FirErrors.LATEINIT_INTRINSIC_CALL_ON_NON_LATEINIT, context)
            return
        }

        // property must be declared in one of the outer lexical scopes
        val containingSymbol = calleePropertySymbol.containingClassOrFile(context)
        if (context.containingDeclarations.none { it.symbol == containingSymbol }) {
            reporter.reportOn(
                source,
                FirErrors.LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY,
                calleePropertySymbol,
                context
            )
            return
        }

        val closestOwnFunction = context.containingDeclarations.lastOrNull()
        if (closestOwnFunction is FirFunction && closestOwnFunction.isInline) {
            reporter.reportOn(source, FirErrors.LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION, context)
        }
    }

    /**
     * Returns the containing class or file if the property is top-level.
     */
    private fun FirPropertySymbol.containingClassOrFile(
        context: CheckerContext
    ): FirBasedSymbol<*>? {
        return getContainingClassSymbol(context.session)
            ?: context.session.firProvider.getFirCallableContainerFile(this)?.symbol
    }
}
