/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.FE10LikeConeSubstitutor
import org.jetbrains.kotlin.fir.analysis.checkers.checkUpperBoundViolated
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.toTypeArgumentsWithSourceInfo
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.constructType

object FirTypeArgumentsOfQualifierOfCallableReferenceChecker : FirCallableReferenceAccessChecker() {
    override fun check(expression: FirCallableReferenceAccess, context: CheckerContext, reporter: DiagnosticReporter) {
        val lhs = expression.explicitReceiver as? FirResolvedQualifier ?: return
        val correspondingDeclaration = lhs.symbol ?: return

        var typeArgumentsWithSourceInfo = lhs.typeArguments.toTypeArgumentsWithSourceInfo()
        var typeParameterSymbols = correspondingDeclaration.typeParameterSymbols
        if (typeParameterSymbols.size != typeArgumentsWithSourceInfo.size) {
            reporter.reportOn(
                lhs.source,
                FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS,
                correspondingDeclaration.typeParameterSymbols.size,
                correspondingDeclaration,
                context
            )
            return
        }

        if (correspondingDeclaration is FirTypeAliasSymbol) {
            val qualifierType = correspondingDeclaration.constructType(typeArgumentsWithSourceInfo.toTypedArray(), isNullable = false)
            val expandedLhsType = qualifierType.fullyExpandedType(context.session)
            typeArgumentsWithSourceInfo = expandedLhsType.typeArguments.toList()

            val expandedClassSymbol = correspondingDeclaration.resolvedExpandedTypeRef.toRegularClassSymbol(context.session) ?: return
            typeParameterSymbols = expandedClassSymbol.typeParameterSymbols
        }

        val substitutor = FE10LikeConeSubstitutor(typeParameterSymbols, typeArgumentsWithSourceInfo, context.session)
        checkUpperBoundViolated(
            context,
            reporter,
            typeParameterSymbols,
            typeArgumentsWithSourceInfo,
            substitutor,
        )
    }
}
