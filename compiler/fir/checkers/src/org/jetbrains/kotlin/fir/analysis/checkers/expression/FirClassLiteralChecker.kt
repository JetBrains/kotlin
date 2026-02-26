/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes.TYPE_ARGUMENT_LIST
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.ownTypeArguments
import org.jetbrains.kotlin.fir.references.toResolvedTypeParameterSymbol
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toTypeParameterSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.lexer.KtTokens.QUEST
import org.jetbrains.kotlin.resolve.checkers.OptInNames

object FirClassLiteralChecker : FirGetClassCallChecker(MppCheckerKind.Common) {

    context(context: CheckerContext)
    private val isGenericArrayAllowed: Boolean
        get() = context.session.firGenericArrayClassLiteralSupport.isEnabled

    context(context: CheckerContext)
    private val areUselessTypeArgumentsForbidden: Boolean
        get() = LanguageFeature.ForbidUselessTypeArgumentsIn25.isEnabled()

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirGetClassCall) {
        val source = expression.source ?: return
        if (source.kind is KtFakeSourceElementKind) return
        val argument = expression.argument
        if (argument is FirResolvedQualifier) {
            val classId = argument.classId
            if (classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID || classId == OptInNames.OPT_IN_CLASS_ID) {
                reporter.reportOn(argument.source, FirErrors.OPT_IN_CAN_ONLY_BE_USED_AS_ANNOTATION)
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
        val resolvedFullyExpandedType = argument.resolvedType.fullyExpandedType()
        val isNullable = markedNullable ||
                (argument as? FirResolvedQualifier)?.isNullableLHSForCallableReference == true ||
                resolvedFullyExpandedType.isMarkedNullable ||
                resolvedFullyExpandedType.isNullableTypeParameter(isExpression = !argument.canBeDoubleColonLHSAsType)
        if (isNullable) {
            if (argument.canBeDoubleColonLHSAsType) {
                reporter.reportOn(source, FirErrors.NULLABLE_TYPE_IN_CLASS_LITERAL_LHS)
            } else {
                reporter.reportOn(
                    argument.source,
                    FirErrors.EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS,
                    argument.resolvedType
                )
            }
            return
        }

        if (!argument.canBeDoubleColonLHSAsType &&
            !LanguageFeature.ForbidClassLiteralWithPotentiallyNullableReifiedLhs.isEnabled() &&
            resolvedFullyExpandedType.toTypeParameterSymbol()?.isReified == true &&
            !resolvedFullyExpandedType.isMarkedNullable &&
            resolvedFullyExpandedType.canBeNull(context.session)
        ) {
            reporter.reportOn(
                argument.source,
                FirErrors.EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS_WARNING,
                argument.resolvedType
            )
        }

        argument.safeAsTypeParameterSymbol?.let {
            if (!it.isReified) {
                // E.g., fun <T: Any> foo(): Any = T::class
                reporter.reportOn(source, FirErrors.TYPE_PARAMETER_AS_REIFIED, it)
            }
        }

        reportTypeArguments(argument, resolvedFullyExpandedType, source)
    }

    context(context: CheckerContext)
    private fun ConeKotlinType.isNullableTypeParameter(isExpression: Boolean): Boolean {
        if (this !is ConeTypeParameterType) return false
        val typeParameter = lookupTag.typeParameterSymbol
        // E.g., fun <T> f2(t: T): Any = t::class
        return canBeNull(context.session) &&
                (!typeParameter.isReified || isExpression && LanguageFeature.ForbidClassLiteralWithPotentiallyNullableReifiedLhs.isEnabled())
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

    /**
     * Type arguments are only allowed in `Array` / typealiases to `Array` on JVM.
     *
     * Without [LanguageFeature.ForbidUselessTypeArgumentsIn25], the following cases must produce deprecation warnings:
     *  1. typealiases to non-generic classes with arbitrary non-zero number of type arguments;
     *  2. typealiases to `Array` with incorrect non-zero number of type arguments on JVM;
     *  3. reified type parameters with non-zero number of type arguments.
     *
     * Cases like `Array<Int, Int>::class` do not require deprecation since they produced internal error in backend.
     *
     * TODO: KT-84254. Here, just remove deprecation logic.
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportTypeArguments(
        argument: FirExpression,
        fullyExpandedType: ConeKotlinType,
        getClassSource: KtSourceElement,
    ) {
        if (argument is FirResolvedReifiedParameterReference) {
            val argumentList = getClassSource.getChild(TYPE_ARGUMENT_LIST) ?: return
            val diagnostic = if (!areUselessTypeArgumentsForbidden) {
                FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED_WARNING
            } else {
                FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED
            }
            reporter.reportOn(argumentList, diagnostic, "for type parameters")
        } else if (argument is FirResolvedQualifier) {
            if (argument.typeArguments.isEmpty()) return
            val symbol = argument.symbol ?: return

            val isAllowedGenericArray = fullyExpandedType.isAllowedGenericArrayTypeInClassLiteral()
            val isTypeAliasToAllowedArray = symbol is FirTypeAliasSymbol && isAllowedGenericArray
            val isDeprecationCase = symbol.isTypeAliasToNonGeneric || isTypeAliasToAllowedArray

            if (!reportWrongNumberOfTypeArguments(argument, isDeprecationCase) && (!isAllowedGenericArray || isTypeAliasToAllowedArray)) {
                val diagnostic = if (isDeprecationCase && !areUselessTypeArgumentsForbidden) {
                    FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS_WARNING
                } else {
                    FirErrors.CLASS_LITERAL_LHS_NOT_A_CLASS
                }
                reporter.reportOn(getClassSource, diagnostic)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private tailrec fun reportWrongNumberOfTypeArguments(
        qualifier: FirResolvedQualifier?,
        deprecationCase: Boolean,
    ): Boolean {
        val symbol = qualifier?.symbol ?: return false
        if (symbol.ownTypeParameterSymbols.size != qualifier.ownTypeArguments.size) {
            val diagnostic = if (deprecationCase && !areUselessTypeArgumentsForbidden) {
                FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS_IN_GET_CLASS_WARNING
            } else {
                FirErrors.WRONG_NUMBER_OF_TYPE_ARGUMENTS
            }
            reporter.reportOn(
                qualifier.source,
                diagnostic,
                symbol.ownTypeParameterSymbols.size,
                symbol,
                positioningStrategy = SourceElementPositioningStrategies.TYPE_ARGUMENT_LIST_OR_WITHOUT_RECEIVER
            )
            return true
        }
        if (!symbol.isInner) return false
        return reportWrongNumberOfTypeArguments(qualifier.explicitParent, deprecationCase)
    }

    context(checkerContext: CheckerContext)
    private val FirClassLikeSymbol<*>.isTypeAliasToNonGeneric: Boolean
        get() = this is FirTypeAliasSymbol && resolvedExpandedTypeRef.coneType.fullyExpandedType().typeArguments.isEmpty()

    context(context: CheckerContext)
    private fun ConeKotlinType.isAllowedGenericArrayTypeInClassLiteral(): Boolean =
        when (this) {
            is ConeClassLikeType if (isNonPrimitiveArray && isGenericArrayAllowed) -> {
                typeArguments.all { typeArgument ->
                    when (typeArgument) {
                        is ConeStarProjection -> false
                        is ConeKotlinTypeProjection ->
                            typeArgument.type.isAllowedTypeArgumentInClassLiteral()
                    }
                }
            }
            else -> false
        }

    context(context: CheckerContext)
    private fun ConeKotlinType.isAllowedTypeArgumentInClassLiteral(): Boolean {
        return (this is ConeClassLikeType && typeArguments.isEmpty())
                || (this is ConeTypeParameterType && lookupTag.typeParameterSymbol.isReified)
                || isAllowedGenericArrayTypeInClassLiteral()
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
