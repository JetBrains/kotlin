/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl

sealed class DoubleColonLHS(val type: ConeKotlinType) {
    /**
     * [isObjectQualifier] is true iff the LHS of a callable reference is a qualified expression which references a named object.
     * Note that such LHS can be treated both as a type and as an expression, so special handling may be required.
     *
     * For example, if `Obj` is an object:
     *
     *     Obj::class         // object qualifier
     *     test.Obj::class    // object qualifier
     *     (Obj)::class       // not an object qualifier (can only be treated as an expression, not as a type)
     *     { Obj }()::class   // not an object qualifier
     */
    class Expression(type: ConeKotlinType, val isObjectQualifier: Boolean) : DoubleColonLHS(type)

    class Type(type: ConeKotlinType) : DoubleColonLHS(type)
}


// Returns true if this expression has the form "A<B>" which means it's a type on the LHS of a double colon expression
internal val FirFunctionCall.hasExplicitValueArguments: Boolean
    get() = true // TODO: hasExplicitArgumentList || hasExplicitLambdaArguments

class FirDoubleColonExpressionResolver(
    private val session: FirSession
) {

    // Returns true if the expression is not a call expression without value arguments (such as "A<B>") or a qualified expression
    // which contains such call expression as one of its parts.
    // In this case it's pointless to attempt to type check an expression on the LHS in "A<B>::class", since "A<B>" certainly means a type.
    private fun FirExpression.canBeConsideredProperExpression(): Boolean {
        return when {
            this is FirQualifiedAccessExpression && explicitReceiver?.canBeConsideredProperExpression() != true -> false
            this is FirFunctionCall && !hasExplicitValueArguments -> false
            else -> true
        }
    }

    private fun FirExpression.canBeConsideredProperType(): Boolean {
        return when {
            this is FirFunctionCall &&
                    explicitReceiver?.canBeConsideredProperType() != false -> !hasExplicitValueArguments
            this is FirQualifiedAccessExpression &&
                    explicitReceiver?.canBeConsideredProperType() != false &&
                    calleeReference is FirNamedReference -> true
            this is FirResolvedQualifier -> true
            else -> false
        }
    }

    private fun shouldTryResolveLHSAsExpression(expression: FirCallableReferenceAccess): Boolean {
        val lhs = expression.explicitReceiver ?: return false
        return lhs.canBeConsideredProperExpression() /* && !expression.hasQuestionMarks */
    }

    private fun shouldTryResolveLHSAsType(expression: FirCallableReferenceAccess): Boolean {
        val lhs = expression.explicitReceiver
        return lhs != null && lhs.canBeConsideredProperType()
    }

    internal fun resolveDoubleColonLHS(doubleColonExpression: FirCallableReferenceAccess): DoubleColonLHS? {
        val resultForExpr = tryResolveLHS(doubleColonExpression, this::shouldTryResolveLHSAsExpression, this::resolveExpressionOnLHS)
        if (resultForExpr != null && !resultForExpr.isObjectQualifier) {
            return resultForExpr
        }

        val resultForType = tryResolveLHS(doubleColonExpression, this::shouldTryResolveLHSAsType) { expression ->
            resolveTypeOnLHS(expression)
        }

        if (resultForType != null) {
            if (resultForExpr != null && resultForType.type == resultForExpr.type) {
                // If we skipped an object expression result before and the type result is the same, this means that
                // there were no other classifier except that object that could win. We prefer to treat the LHS as an expression here,
                // to have a bound callable reference / class literal
                return resultForExpr
            }
            return resultForType
        }

        // If the LHS could be resolved neither as an expression nor as a type, we should still type-check it to allow all diagnostics
        // to be reported and references to be resolved. For that, we commit one of the applicable traces here, preferring the expression
        return resultForExpr
    }

    /**
     * Returns null if the LHS is definitely not an expression. Returns a non-null result if a resolution was attempted and led to
     * either a successful result or not.
     */
    private fun <T : DoubleColonLHS> tryResolveLHS(
        doubleColonExpression: FirCallableReferenceAccess,
        criterion: (FirCallableReferenceAccess) -> Boolean,
        resolve: (FirExpression) -> T?
    ): T? {
        val expression = doubleColonExpression.explicitReceiver ?: return null

        if (!criterion(doubleColonExpression)) return null

        return resolve(expression)
    }

    private fun resolveExpressionOnLHS(expression: FirExpression): DoubleColonLHS.Expression? {
        val type = (expression.typeRef as? FirResolvedTypeRef)?.type ?: return null

        if (expression is FirResolvedQualifier) {
            val firClass = session.firSymbolProvider
                .getClassLikeSymbolByFqName(expression.classId ?: return null)
                ?.fir as? FirRegularClass
                ?: return null

            if (firClass.classKind == ClassKind.OBJECT) return DoubleColonLHS.Expression(type, isObjectQualifier = true)
            return null
        }

        return DoubleColonLHS.Expression(type, isObjectQualifier = false)
    }

    private fun resolveTypeOnLHS(
        expression: FirExpression
    ): DoubleColonLHS.Type? {
        val resolvedExpression =
            expression as? FirResolvedQualifier
                ?: return null

        val firClass = session.firSymbolProvider
            .getClassLikeSymbolByFqName(resolvedExpression.classId ?: return null)
            // TODO: support type aliases
            ?.fir as? FirRegularClass
            ?: return null

        val type = ConeClassLikeTypeImpl(
            firClass.symbol.toLookupTag(),
            Array(firClass.typeParameters.size) { ConeStarProjection },
            isNullable = false // TODO: Use org.jetbrains.kotlin.psi.KtDoubleColonExpression.getHasQuestionMarks
        )

        return DoubleColonLHS.Type(type)
    }
}
