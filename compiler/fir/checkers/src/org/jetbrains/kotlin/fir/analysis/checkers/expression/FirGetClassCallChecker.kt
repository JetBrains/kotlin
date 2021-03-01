/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens.QUEST

object FirGetClassCallChecker : FirBasicExpressionChecker() {
    override fun check(expression: FirStatement, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression !is FirGetClassCall) return
        val source = expression.source ?: return
        if (source.kind is FirFakeSourceElementKind) return

        val argument = expression.argument as? FirResolvedQualifier ?: return
        // Note that raw FIR drops marked nullability "?" in, e.g., `A?::class`, `A<T?>::class`, or `A<T?>?::class`.
        // That is, AST structures for those expressions have token type QUEST, whereas FIR element doesn't have any information about it.
        //
        // A?::class -> CLASS_LITERAL_EXPRESSION(REFERENCE_EXPRESSION QUEST COLONCOLON "class")
        // A<T?>::class -> CLASS_LITERAL_EXPRESSION(REFERENCE_EXPRESSION TYPE_ARGUMENT_LIST COLONCOLON "class")
        // A<T?>?::class -> CLASS_LITERAL_EXPRESSION(REFERENCE_EXPRESSION TYPE_ARGUMENT_LIST QUEST COLONCOLON "class")
        //   where TYPE_ARGUMENT_LIST may have QUEST in it
        //
        // Only the 2nd example is valid, and we want to check if token type QUEST doesn't exist at the same level as COLONCOLON.
        val markedNullable = source.getChild(QUEST, depth = 1) != null
        if (argument.isNullableLHSForCallableReference || markedNullable) {
            reporter.reportOn(source, FirErrors.NULLABLE_TYPE_IN_CLASS_LITERAL_LHS, context)
            return
        }
        // TODO: differentiate RESERVED_SYNTAX_IN_CALLABLE_REFERENCE_LHS
        if (argument.typeArguments.isNotEmpty() && !argument.typeRef.coneType.isAllowedInClassLiteral(context)) {
            val typeParameters = (argument.symbol?.fir as? FirTypeParameterRefsOwner)?.typeParameters
            // Among type parameter references, only count actual type parameter while discarding [FirOuterClassTypeParameterRef]
            val expectedTypeArgumentSize = typeParameters?.filterIsInstance<FirTypeParameter>()?.size ?: 0
            if (expectedTypeArgumentSize != argument.typeArguments.size) {
                // Will be reported as WRONG_NUMBER_OF_TYPE_ARGUMENTS
                return
            }
            reporter.reportOn(source, FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS, context)
        }
    }

    private fun ConeKotlinType.isAllowedInClassLiteral(context: CheckerContext): Boolean =
        when (this) {
            is ConeClassLikeType -> {
                if (isNonPrimitiveArray) {
                    typeArguments.none { typeArgument ->
                        when (typeArgument) {
                            is ConeStarProjection -> true
                            is ConeKotlinTypeProjection -> !typeArgument.type.isAllowedInClassLiteral(context)
                        }
                    }
                } else
                    typeArguments.isEmpty()
            }
            is ConeTypeParameterType -> this.lookupTag.typeParameterSymbol.fir.isReified
            else -> false
        }
}
