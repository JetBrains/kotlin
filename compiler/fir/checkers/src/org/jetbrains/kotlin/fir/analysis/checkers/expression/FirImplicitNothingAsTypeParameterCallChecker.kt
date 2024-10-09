/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isExplicit
import org.jetbrains.kotlin.fir.analysis.checkers.typeParameterSymbols
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.toResolvedFunctionSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*

object FirImplicitNothingAsTypeParameterCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!expression.resolvedType.isNothingOrNullableNothing) return // Expression doesn't return Nothing.
        if (expression.resolvedType.expectedType?.isNothing == true) return // Expression was expected to return Nothing.
        if (expression.dispatchReceiver?.resolvedType?.isBasicFunctionType(context.session) == true) return

        val function = expression.calleeReference.toResolvedFunctionSymbol() ?: return
        when (expression.source?.kind) {
            KtFakeSourceElementKind.DelegatedPropertyAccessor -> {
                // Consider cases when the delegate accessor is resolved to the class type parameter.
                val typeRef = function.resolvedReturnTypeRef.delegatedTypeRef as? FirUserTypeRef ?: return
                if (typeRef.qualifier.size != 1) return
                val typeParameters = function.getContainingSymbol(context.session)?.typeParameterSymbols ?: return

                // Have no better way to check if the type parameter is from the class, other than to compare names.
                val name = typeRef.qualifier[0].name
                if (typeParameters.none { it.name == name }) return
            }
            else -> {
                // Ignore cases when the function return type parameter is not from the function.
                val typeParameter = (function.resolvedReturnType.toSymbol(context.session) as? FirTypeParameterSymbol) ?: return
                if (typeParameter.containingDeclarationSymbol != function) return
                // Ignore cases when the type argument is explicitly specified as Nothing.
                val typeArgument = expression.typeArguments.getOrNull(function.typeParameterSymbols.indexOf(typeParameter))
                if (typeArgument?.isExplicit == true) return
            }
        }

        // Ignore cases when a type parameter T without bounds is resolved to Nothing?.
        if (expression.resolvedType.isNullableNothing && !function.resolvedReturnType.isMarkedNullable) {
            return
        }

        /*
         * The warning isn't reported in cases where there are lambdas among the function arguments,
         * the return type of which is a type variable that was inferred to Nothing.
         * This corresponds to useful cases in which this report will not be helpful.
         *
         * E.g.:
         *
         * 1) Return if null:
         *      x?.let { return }
         *
         * 2) Implicit receiver to shorter code writing:
         *      x.run {
         *          println(inv())
         *          return inv()
         *      }
         */
        if (function.valueParameterSymbols.any {
                it.resolvedReturnType.isSomeFunctionType(context.session) &&
                        function.resolvedReturnType == it.resolvedReturnType.typeArguments.last().type
            }) return

        reporter.reportOn(expression.source, FirErrors.IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION, context)
    }
}
