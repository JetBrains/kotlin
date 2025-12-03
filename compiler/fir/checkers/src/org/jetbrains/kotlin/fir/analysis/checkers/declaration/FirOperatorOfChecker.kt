/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.lowerBoundIfFlexible
import org.jetbrains.kotlin.util.OperatorNameConventions


@OptIn(SymbolInternals::class)
object FirOperatorOfChecker : FirRegularClassChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (LanguageFeature.CollectionLiterals.isDisabled()) return
        val companion = declaration.companionObjectSymbol?.fir ?: return

        CheckerImpl(declaration, companion).check()
    }

    private class CheckerImpl(
        val outerClass: FirRegularClass,
        val companion: FirRegularClass,
    ) {
        open inner class OfOverload(val function: FirNamedFunction) {
            context(context: CheckerContext, reporter: DiagnosticReporter)
            fun checkReturnType() {
                fun report(dueToNullability: Boolean) {
                    if (!dueToNullability) {
                        reporter.reportOn(function.source, FirErrors.RETURN_TYPE_MISMATCH_OF_OPERATOR_OF, outerClass.symbol)
                    } else {
                        reporter.reportOn(function.source, FirErrors.NULLABLE_RETURN_TYPE_OF_OPERATOR_OF)
                    }
                }

                val returnType = function.returnTypeRef.coneType.lowerBoundIfFlexible().fullyExpandedType()
                when {
                    returnType is ConeErrorType -> {
                    }
                    returnType !is ConeClassLikeType -> {
                        report(dueToNullability = false)
                    }
                    returnType.classId != outerClass.symbol.classId -> {
                        report(dueToNullability = false)
                    }
                    returnType.isMarkedNullable -> {
                        report(dueToNullability = true)
                    }
                }
            }
        }

        inner class MainOfOverload(function: FirNamedFunction, val mainParameter: FirValueParameter) : OfOverload(function)

        /**
         * @return true if exactly one main overload
         */
        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkNumberOfMainOverloads(overloads: List<OfOverload>): Boolean {
            return when (overloads.count { it is MainOfOverload }) {
                1 -> true
                0 -> {
                    overloads.forEach { overload ->
                        reporter.reportOn(overload.function.source, FirErrors.NO_VARARG_OVERLOAD_OF_OPERATOR_OF)
                    }
                    false
                }
                else -> {
                    overloads.filterIsInstance<MainOfOverload>().forEach { overload ->
                        reporter.reportOn(overload.function.source, FirErrors.MULTIPLE_VARARG_OVERLOADS_OF_OPERATOR_OF)
                    }
                    false
                }
            }
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        fun check() {
            val allOverloads = buildList {
                companion.processAllDeclarations(context.session) { functionSymbol ->
                    if (functionSymbol !is FirNamedFunctionSymbol) return@processAllDeclarations
                    if (!functionSymbol.isOperator || functionSymbol.name != OperatorNameConventions.OF) return@processAllDeclarations

                    val function = functionSymbol.fir

                    val mainParameter: FirValueParameter? = function.valueParameters.firstOrNull { it.isVararg }

                    if (mainParameter != null) {
                        add(MainOfOverload(function, mainParameter))
                    } else {
                        add(OfOverload(function))
                    }
                }
            }

            if (allOverloads.isEmpty()) return

            allOverloads.forEach {
                it.checkReturnType()
            }

            if (!checkNumberOfMainOverloads(allOverloads)) return

            val mainOverload = allOverloads.filterIsInstance<MainOfOverload>().single()
        }
    }
}