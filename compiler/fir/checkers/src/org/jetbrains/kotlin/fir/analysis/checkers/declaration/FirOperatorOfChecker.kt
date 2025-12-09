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
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.impl.FirStandardOverrideChecker
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.arrayElementType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.util.OperatorNameConventions


@OptIn(SymbolInternals::class)
object FirOperatorOfChecker : FirRegularClassChecker(MppCheckerKind.Common) {

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        if (LanguageFeature.CollectionLiterals.isDisabled()) return
        val companion = declaration.companionObjectSymbol?.fir ?: return

        CheckerImpl(companion).check()
    }

    private class CheckerImpl(
        val companion: FirRegularClass,
    ) {
        open class OfOverload(val function: FirNamedFunction)

        class MainOfOverload(function: FirNamedFunction, val mainParameter: FirValueParameter) : OfOverload(function) {
            val mainParameterElementType: ConeKotlinType?
                get() = mainParameter.returnTypeRef.coneType.arrayElementType()
        }

        context(overrideChecker: FirStandardOverrideChecker)
        private fun MainOfOverload.isMatchingParameter(
            valueParameter: FirValueParameter,
            substitutor: ConeSubstitutor,
        ): Boolean {
            if (mainParameter === valueParameter) return true

            val mainParameterElementTypeRef = mainParameterElementType?.toFirResolvedTypeRef() ?: return true
            return overrideChecker.isEqualTypes(
                mainParameterElementTypeRef,
                valueParameter.returnTypeRef,
                substitutor,
            )
        }

        context(overrideChecker: FirStandardOverrideChecker, context: CheckerContext, reporter: DiagnosticReporter)
        private fun MainOfOverload.checkOverload(overload: FirNamedFunction) {
            // mainOverload -> overload because order is not important for checks while we can get better error messages with this direction
            val substitutor = overrideChecker.buildTypeParametersSubstitutorIfCompatible(function, overload, checkReifiednessIsSame = true)

            if (substitutor == null) {
                reporter.reportOn(overload.source, FirErrors.INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS, function.symbol)
                return
            }

            for (valueParameter in overload.valueParameters) {
                if (!isMatchingParameter(valueParameter, substitutor)) {
                    reporter.reportOn(
                        valueParameter.source,
                        FirErrors.INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS,
                        substitutor.substituteOrSelf(mainParameterElementType!!.fullyExpandedType()),
                    )
                }
            }

            if (function === overload) return

            if (!overrideChecker.isEqualTypes(function.returnTypeRef, overload.returnTypeRef, substitutor)) {
                reporter.reportOn(
                    overload.source,
                    FirErrors.INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS,
                    substitutor.substituteOrSelf(function.returnTypeRef.coneType.fullyExpandedType()),
                )
            }

            checkStatus(function, overload)
        }

        context(context: CheckerContext, reporter: DiagnosticReporter)
        private fun checkStatus(main: FirNamedFunction, overload: FirNamedFunction) {
            if (main.visibility != overload.visibility) {
                reporter.reportOn(overload.source, FirErrors.INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS, main.visibility)
            }
        }

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

            if (!checkNumberOfMainOverloads(allOverloads)) return

            val mainOverload = allOverloads.filterIsInstance<MainOfOverload>().single()

            context(FirStandardOverrideChecker(context.session)) {
                for (overload in allOverloads)
                    mainOverload.checkOverload(overload.function)
            }
        }
    }
}