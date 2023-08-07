/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.unsubstitutedScope
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.scopes.processAllCallables
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.name.StandardClassIds.Annotations.FunctionN

object FirBadInheritedJavaSignaturesChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        fun containsFunctionN(type: ConeKotlinType) = type.classId == FunctionN

        declaration.unsubstitutedScope(context).processAllCallables { symbol ->
            if (!symbol.isJavaOrEnhancement) {
                return@processAllCallables
            }

            val hasBadReturnType = symbol.resolvedReturnType.contains(::containsFunctionN)
            // NB: This case with receiver is not covered with tests
            // and was replicated, because it's present in the original
            // checker.
            val hasBadReceiverType = symbol.resolvedReceiverTypeRef?.type?.contains(::containsFunctionN) == true
            val hasBadValueParameter = symbol is FirFunctionSymbol<*> && symbol.valueParameterSymbols.any { valueParameter ->
                valueParameter.resolvedReturnType.contains(::containsFunctionN)
            }

            if (hasBadReturnType || hasBadReceiverType || hasBadValueParameter) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.UNSUPPORTED_INHERITANCE_FROM_JAVA_MEMBER_REFERENCING_KOTLIN_FUNCTION,
                    symbol, context,
                )
            }
        }
    }
}
