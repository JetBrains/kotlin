/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.containingClassLookupTag
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.toClassSymbol
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.EnrichedProjectionKind
import org.jetbrains.kotlin.types.Variance

/**
 * Reports INVISIBLE_REFERENCE with PrivateToThis visibility for callable references that try to leak
 * members effectively having "private to this" visibility (see K1 behavior), e.g. A<S>::x inside class A<in T>.
 *
 * This complements FirPrivateToThisAccessChecker, which covers qualified access expressions but may
 * miss callable references with class qualifiers (unbound references) where dispatchReceiver is absent.
 */
object FirPrivateToThisCallableReferenceChecker : FirCallableReferenceAccessChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirCallableReferenceAccess) {
        val reference = expression.calleeReference.toResolvedCallableSymbol() ?: return
        val symbol = reference
        if (symbol.visibility != Visibilities.Private) return

        val containingClassSymbol = symbol.containingClassLookupTag()?.toClassSymbol()
        if (!isPrivateToThis(symbol.unwrapFakeOverrides(), containingClassSymbol, context.session)) return

        // For callable references, only this::member (bound to the current instance) is allowed
        // when the member is effectively private-to-this. Any other explicit receiver leaks it.
        val receiverRef = expression.explicitReceiver?.toReference(context.session)
        val isVisible = receiverRef is org.jetbrains.kotlin.fir.references.FirThisReference &&
                receiverRef.boundSymbol == containingClassSymbol

        if (!isVisible) {
            reporter.reportOn(
                expression.source,
                FirErrors.INVISIBLE_REFERENCE,
                symbol,
                Visibilities.PrivateToThis,
                symbol.callableId!!.classId
            )
        }
    }

    private fun isPrivateToThis(
        symbol: FirCallableSymbol<*>,
        containingClassSymbol: FirClassSymbol<*>?,
        session: FirSession
    ): Boolean {
        if (containingClassSymbol == null) return false
        if (symbol is FirConstructorSymbol) return false
        if (containingClassSymbol.typeParameterSymbols.all { it.variance == Variance.INVARIANT }) return false
        // KT-68636 data class generated copy method can never have privateToThis visibility
        // We have to explicitly exclude data class copy because a general case isn't yet supported KT-35396
        if (symbol.isDataClassCopy(containingClassSymbol, session)) return false

        if (symbol.resolvedReceiverType?.contradictsWith(Variance.IN_VARIANCE, session) == true) {
            return true
        }
        if (symbol.resolvedReturnType.contradictsWith(
                if (symbol is FirPropertySymbol && symbol.isVar) Variance.INVARIANT
                else Variance.OUT_VARIANCE,
                session
            )
        ) {
            return true
        }
        if (symbol is FirFunctionSymbol<*>) {
            for (parameter in symbol.valueParameterSymbols) {
                if (parameter.resolvedReturnType.contradictsWith(Variance.IN_VARIANCE, session)) {
                    return true
                }
            }
        }
        for (parameter in symbol.contextParameterSymbols) {
            if (parameter.resolvedReturnType.contradictsWith(Variance.IN_VARIANCE, session)) {
                return true
            }
        }
        return false
    }

    private fun ConeKotlinType.contradictsWith(requiredVariance: Variance, session: FirSession): Boolean {
        when (this) {
            is ConeLookupTagBasedType -> {
                if (this is ConeTypeParameterType) {
                    return !this.lookupTag.typeParameterSymbol.variance.allowsPosition(requiredVariance)
                }
                if (this is ConeClassLikeType) {
                    // It's safe to access fir here, because later we access only variance of type parameters of the class
                    // And variance can not be changed after raw fir stage
                    @OptIn(SymbolInternals::class)
                    val classLike = this.lookupTag.toSymbol(session)?.fir ?: return false
                    for ((index, argument) in this.typeArguments.withIndex()) {
                        val typeParameterRef = classLike.typeParameters.getOrNull(index)
                        if (typeParameterRef !is FirTypeParameter) continue
                        val requiredVarianceForArgument = when (
                            EnrichedProjectionKind.getEffectiveProjectionKind(typeParameterRef.variance, argument.variance)
                        ) {
                            EnrichedProjectionKind.OUT -> requiredVariance
                            EnrichedProjectionKind.IN -> requiredVariance.opposite()
                            EnrichedProjectionKind.INV -> Variance.INVARIANT
                            EnrichedProjectionKind.STAR -> continue // CONFLICTING_PROJECTION error was reported
                        }
                        val argType = argument.type ?: continue
                        if (argType.contradictsWith(requiredVarianceForArgument, session)) {
                            return true
                        }
                    }
                }
            }
            is ConeFlexibleType -> {
                return lowerBound.contradictsWith(requiredVariance, session)
            }
            is ConeDefinitelyNotNullType -> {
                return original.contradictsWith(requiredVariance, session)
            }
            is ConeIntersectionType -> {
                return this.intersectedTypes.any { it.contradictsWith(requiredVariance, session) }
            }
            is ConeCapturedType -> {
                // Looks like not possible here
                return false
            }
            is ConeIntegerConstantOperatorType,
            is ConeIntegerLiteralConstantType,
            is ConeStubTypeForTypeVariableInSubtyping,
            is ConeTypeVariableType,
            -> return false
        }
        return false
    }
}
