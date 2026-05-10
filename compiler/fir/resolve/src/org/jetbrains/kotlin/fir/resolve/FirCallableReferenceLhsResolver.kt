/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.expandedConeType
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.runUnless

/**
 * [type] is always non-error type, errors/deprecation errors are stored in [diagnostic].
 */
class CallableReferenceLhsAsType(
    val type: ConeKotlinType,
    val diagnostic: ConeDiagnostic?,
    val hasNullableMark: Boolean,
    val hasExplicitTypeArguments: Boolean,
) {
    /**
     * `true` if the corresponding [FirResolvedQualifier] was written without:
     * - a nullable mark (`?`), and
     * - any explicit type arguments.
     *
     * In other words, the LHS is a plain classifier reference such as `NG::foo`,
     * as opposed to `NG?::foo`, `G<*>::foo`, or `G<*>?::foo`.
     */
    val isProperStaticReceiver: Boolean
        get() = !hasNullableMark && !hasExplicitTypeArguments
}

class FirCallableReferenceLhsResolver(
    private val components: BodyResolveComponents,
    private val context: BodyResolveContext,
) {
    private val session = components.session

    /**
     * Returns not-`null` iff LHS of callable reference _can be_ non-expression, i.e., resolved qualifier to a non-object.
     * Note, however, that this does not necessarily mean that the reference will be unbound because it can refer to
     * a member / extension of a companion object.
     */
    fun resolveLhsAsType(doubleColonExpression: FirCallableReferenceAccess): CallableReferenceLhsAsType? {
        val lhsExpression = doubleColonExpression.explicitReceiver ?: return null

        val resultForExpr = runUnless(doubleColonExpression.hasQuestionMarkAtLhs) {
            resolveExpressionOnLhs(lhsExpression)
        }

        if (resultForExpr != null && !resultForExpr.isObjectQualifier) {
            return null
        }

        val resultForType = resolveTypeOnLhs(lhsExpression)

        return resultForType?.takeUnless {
            // If we skipped an object expression result before and the type result is the same, this means that
            // there was no other classifier except that object that could win.
            // We prefer to treat the LHS as an expression here, to have a bound callable reference / class literal
            // TODO: KT-84336
            //  Qualifier with type arguments always has type `Unit`, but that might be generic typealias to `Unit` in which case
            //  `resultForType` should be preferred.
            resultForExpr != null && resultForType.type.equalTypes(resultForExpr.type, session)
        }
    }

    private fun FirResolvedQualifier.expandedRegularClassIfAny(): FirRegularClass? {
        var fir = symbol?.fir ?: return null
        while (fir is FirTypeAlias) {
            fir = fir.expandedConeType?.lookupTag?.toSymbol(session)?.fir ?: return null
        }
        return fir as? FirRegularClass
    }

    /**
     * [isObjectQualifier] is true iff the LHS of a callable reference is a qualified expression that references a named object.
     * Such expressions are always treated as objects. Hence, the callable reference is always bound.
     *
     * Note that this type is marked private: it is only used internally by [FirCallableReferenceLhsResolver].
     * Outside of [FirCallableReferenceLhsResolver], only [CallableReferenceLhsAsType] (or `null`) is possible.
     */
    private class ExpressionCallableReferenceLhs(
        val type: ConeKotlinType,
        val isObjectQualifier: Boolean,
    )

    private fun resolveExpressionOnLhs(expression: FirExpression): ExpressionCallableReferenceLhs? {
        val type = expression.resolvedType

        val expressionWithoutSmartCast = expression.unwrapSmartcastExpression()
        if (expressionWithoutSmartCast is FirResolvedQualifier) {
            val firClass = expressionWithoutSmartCast.expandedRegularClassIfAny() ?: return null
            if (firClass.classKind == ClassKind.OBJECT) {
                return ExpressionCallableReferenceLhs(type, isObjectQualifier = true)
            }
            return null
        }

        return ExpressionCallableReferenceLhs(type, isObjectQualifier = false)
    }

    private fun resolveTypeOnLhs(
        expression: FirExpression
    ): CallableReferenceLhsAsType? {
        val resolvedExpression = expression.unwrapSmartcastExpression() as? FirResolvedQualifier
            ?: return null

        return session.typeResolver.resolveTypeOnDoubleColonLhs(
            resolvedExpression,
            TypeResolutionConfiguration(
                components.createCurrentScopeList(),
                context.containingClassDeclarations,
                context.file,
                context.topContainerForTypeResolution,
            )
        )
    }
}
