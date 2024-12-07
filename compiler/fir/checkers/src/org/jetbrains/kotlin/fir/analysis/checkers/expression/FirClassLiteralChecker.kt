/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedTypeParameterSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens.QUEST
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirClassLiteralChecker : FirGetClassCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirGetClassCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val source = expression.source ?: return
        if (source.kind is KtFakeSourceElementKind) return
        val argument = expression.argument
        if (argument is FirResolvedQualifier) {
            val classId = argument.classId
            if (classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID || classId == OptInNames.OPT_IN_CLASS_ID) {
                reporter.reportOn(argument.source, FirErrors.OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION, context)
            }
        }

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
        val resolvedFullyExpandedType = argument.resolvedType.fullyExpandedType(context.session)
        val isNullable = markedNullable ||
                (argument as? FirResolvedQualifier)?.isNullableLHSForCallableReference == true ||
                resolvedFullyExpandedType.isMarkedNullable ||
                resolvedFullyExpandedType.isNullableTypeParameter(context.session.typeContext)
        if (isNullable) {
            if (argument.canBeDoubleColonLHSAsType) {
                reporter.reportOn(source, FirErrors.NULLABLE_TYPE_IN_CLASS_LITERAL_LHS, context)
            } else {
                reporter.reportOn(
                    argument.source,
                    FirErrors.EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS,
                    argument.resolvedType,
                    context
                )
            }
            return
        }

        argument.safeAsTypeParameterSymbol?.let {
            if (!it.isReified) {
                // E.g., fun <T: Any> foo(): Any = T::class
                reporter.reportOn(source, FirErrors.TYPE_PARAMETER_AS_REIFIED, it, context)
            }
        }

        if (argument !is FirResolvedQualifier) return
        if (argument.typeArguments.isNotEmpty() && !resolvedFullyExpandedType.isAllowedInClassLiteral(context)) {
            val symbol = argument.symbol
            symbol?.lazyResolveToPhase(FirResolvePhase.TYPES)
            // Among type parameter references, only count actual type parameter while discarding [FirOuterClassTypeParameterRef]
            val expectedTypeArgumentSize = symbol?.ownTypeParameterSymbols?.size ?: 0
            if (expectedTypeArgumentSize != argument.typeArguments.size) {
                if (symbol != null) {
                    reporter.reportOn(argument.source, FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS, expectedTypeArgumentSize, symbol, context)
                }
                return
            }
            reporter.reportOn(source, FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS, context)
        }
    }

    private fun ConeKotlinType.isNullableTypeParameter(context: ConeInferenceContext): Boolean {
        if (this !is ConeTypeParameterType) return false
        val typeParameter = lookupTag.typeParameterSymbol
        with(context) {
            return !typeParameter.isReified &&
                    // E.g., fun <T> f2(t: T): Any = t::class
                    typeParameter.toConeType().isNullableType()
        }
    }

    private val FirExpression.canBeDoubleColonLHSAsType: Boolean
        get() {
            return this is FirResolvedQualifier ||
                    this is FirResolvedReifiedParameterReference ||
                    safeAsTypeParameterSymbol != null
        }

    private val FirExpression.safeAsTypeParameterSymbol: FirTypeParameterSymbol?
        get() {
            return (this as? FirQualifiedAccessExpression)?.calleeReference?.toResolvedTypeParameterSymbol()
        }

    private fun ConeKotlinType.isAllowedInClassLiteral(context: CheckerContext): Boolean =
        when (this) {
            is ConeClassLikeType -> {
                val isPlatformThatAllowsNonPrimitiveArrays = context.session.firGenericArrayClassLiteralSupport.isEnabled
                val isOldVersionThatAllowsNonPrimitiveArrays =
                    !context.languageVersionSettings.supportsFeature(LanguageFeature.ProhibitGenericArrayClassLiteral)
                if (isNonPrimitiveArray && (isPlatformThatAllowsNonPrimitiveArrays || isOldVersionThatAllowsNonPrimitiveArrays)) {
                    typeArguments.none { typeArgument ->
                        when (typeArgument) {
                            is ConeStarProjection -> true
                            is ConeKotlinTypeProjection -> !typeArgument.type.isAllowedInClassLiteral(context)
                        }
                    }
                } else
                    typeArguments.isEmpty()
            }
            is ConeTypeParameterType -> this.lookupTag.typeParameterSymbol.isReified
            else -> false
        }
}

interface FirGenericArrayClassLiteralSupport : FirSessionComponent {
    val isEnabled: Boolean

    object Enabled : FirGenericArrayClassLiteralSupport {
        override val isEnabled: Boolean = true
    }

    object Disabled : FirGenericArrayClassLiteralSupport {
        override val isEnabled: Boolean = false
    }
}

val FirSession.firGenericArrayClassLiteralSupport: FirGenericArrayClassLiteralSupport by FirSession.sessionComponentAccessor()
