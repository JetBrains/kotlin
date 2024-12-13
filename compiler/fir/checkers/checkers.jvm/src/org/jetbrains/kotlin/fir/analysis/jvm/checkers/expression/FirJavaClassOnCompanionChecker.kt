/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirPropertyAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.declarations.utils.isCompanion
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjection
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.JvmStandardClassIds

object FirJavaClassOnCompanionChecker : FirPropertyAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirPropertyAccessExpression,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val reference = expression.calleeReference as? FirResolvedNamedReference ?: return
        if ((reference.symbol as? FirCallableSymbol)?.callableId != JvmStandardClassIds.Callables.JavaClass) return

        val actualType = expression.resolvedType as? ConeClassLikeType ?: return
        val projectionType = (actualType.typeArguments.singleOrNull() as? ConeKotlinTypeProjection)?.type ?: return
        val projectionClassSymbol = projectionType.toRegularClassSymbol(context.session)
        if (projectionClassSymbol?.isCompanion != true) return

        val containingClassSymbol = projectionClassSymbol.getContainingClassSymbol() ?: return
        val expectedType = actualType.lookupTag.constructClassType(
            arrayOf(containingClassSymbol.defaultType()), isMarkedNullable = actualType.isMarkedNullable
        )

        reporter.reportOn(expression.source, FirJvmErrors.JAVA_CLASS_ON_COMPANION, actualType, expectedType, context)
    }
}
