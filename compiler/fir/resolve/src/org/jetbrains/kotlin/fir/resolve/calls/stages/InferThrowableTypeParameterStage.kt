/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.isJavaOrEnhancement
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSink
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeParameterBasedTypeVariable
import org.jetbrains.kotlin.fir.scopes.impl.typeAliasConstructorInfo
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.unwrapSubstitutionOverrides
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.isSubtypeConstraintCompatible
import org.jetbrains.kotlin.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.MutableVariableWithConstraints
import org.jetbrains.kotlin.resolve.calls.inference.model.ThrowableUpperBoundAllowingToFixIntoIt
import org.jetbrains.kotlin.types.model.typeConstructor

/**
 * For Java declarations, infers Throwable-bounded type parameters that are unused in the signature
 * to their upper bound. This handles the common Java pattern where a type parameter like
 * `E extends Throwable` is only used in `throws` clauses (which Kotlin ignores).
 *
 * See KT-82961.
 */
internal object InferThrowableTypeParameterToUpperBound : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        if (LanguageFeature.InferThrowableTypeParameterToUpperBound.isDisabled()) return
        if (!candidate.symbol.isJavaOrEnhancement) {
            // Also handle typealias constructors pointing to Java classes
            val originalConstructor =
                (candidate.symbol.fir as? FirConstructor)?.typeAliasConstructorInfo?.originalConstructor?.unwrapSubstitutionOverrides()
            if (originalConstructor?.isJavaOrEnhancement != true) return
        }

        val callableSymbol = candidate.symbol as? FirCallableSymbol<*> ?: return
        val session = context.session
        val nullableThrowable = session.builtinTypes.throwableType.coneType.withNullability(nullable = true, session.typeContext)

        for ((index, variable) in candidate.freshVariables.withIndex()) {
            if (variable !is ConeTypeParameterBasedTypeVariable) continue
            val typeParameterSymbol = variable.typeParameterSymbol
            // Though there should be no reified type parameters in Java declarations, I don't feel like having an assertion yet.
            if (typeParameterSymbol.isReified) continue

            // Skip if an explicit type argument was provided for this type parameter
            if (candidate.typeArgumentMapping[index] !is FirPlaceholderProjection) continue

            val typeVariableStorage =
                candidate.system.notFixedTypeVariables[variable.typeConstructor] ?: continue

            val firstBoundSubtypeOfThrowable =
                firstBoundSubtypeOfThrowable(typeVariableStorage, candidate.system, nullableThrowable) ?: continue

            if (isTypeVariableCanBeAutoInferredToThrowable(variable, callableSymbol, candidate, session)) {
                // We add basically the same upper bound it already had, but with a different constraint position
                // which allows using it for fixation.
                candidate.system.addSubtypeConstraint(
                    variable.defaultType,
                    firstBoundSubtypeOfThrowable,
                    ThrowableUpperBoundAllowingToFixIntoIt,
                )
            }
        }
    }

    private fun firstBoundSubtypeOfThrowable(
        typeVariable: MutableVariableWithConstraints,
        csBuilder: ConstraintSystemBuilder,
        throwableType: ConeKotlinType,
    ): ConeKotlinType? {
        return typeVariable.constraints.firstOrNull { constraint ->
            if (constraint.position.from !is DeclaredUpperBoundConstraintPosition<*>) return@firstOrNull false
            if (!csBuilder.isProperType(constraint.type)) return@firstOrNull false

            csBuilder.isSubtypeConstraintCompatible(constraint.type, throwableType)
        }?.type as ConeKotlinType?
    }

    private fun isTypeVariableCanBeAutoInferredToThrowable(
        variable: ConeTypeParameterBasedTypeVariable,
        callableSymbol: FirCallableSymbol<*>,
        candidate: Candidate,
        session: FirSession,
    ): Boolean {
        val typeConstructorForVariable = variable.typeConstructor

        fun ConeKotlinType.containsGivenTypeVariable(): Boolean =
            candidate.substitutor.substituteOrSelf(this)
                .contains { it.typeConstructor(session.typeContext) == typeConstructorForVariable }

        if (callableSymbol.fir.returnTypeRef.coneType.containsGivenTypeVariable()) return false

        // Check extension receiver
        callableSymbol.fir.receiverParameter?.let { receiverParam ->
            if (receiverParam.typeRef.coneType.containsGivenTypeVariable()) return false
        }

        // Check dispatch receiver
        callableSymbol.dispatchReceiverType?.let { dispatchType ->
            if (dispatchType.containsGivenTypeVariable()) return false
        }

        // Check context parameters
        callableSymbol.contextParameterSymbols.forEach { contextParam ->
            if (contextParam.resolvedReturnType.containsGivenTypeVariable()) return false
        }

        // Check value parameter types, accounting for SAM conversions
        val function = callableSymbol.fir as? FirFunction
        var anySamConversionHidingTypeVariable = false
        if (function != null) {
            val samConversions = candidate.samConversionInfosOfArguments
            for ((atom, param) in candidate.argumentMapping) {
                val samInfo = samConversions?.get(atom.expression)
                // For SAM-converted arguments, check the functional type
                // (the declared SAM interface type contains the type variable in throws clause,
                // but the functional type won't contain it since throws is stripped)
                val originalParamType = param.returnTypeRef.coneType
                val typeToCheck = samInfo?.functionalType ?: originalParamType
                if (typeToCheck.containsGivenTypeVariable()) return false

                if (samInfo != null && originalParamType.containsGivenTypeVariable()) {
                    anySamConversionHidingTypeVariable = true
                }
            }
        }

        // There should be at least some ThrowableComputable<T, E> converted into `() -> T`
        if (!anySamConversionHidingTypeVariable) return false

        // Check bounds of other type variables
        for (otherVariable in candidate.freshVariables) {
            if (otherVariable === variable) continue
            val variableWithStorage = candidate.system.notFixedTypeVariables[otherVariable.typeConstructor] ?: continue
            for (constraint in variableWithStorage.constraints) {
                if ((constraint.type as ConeKotlinType).containsGivenTypeVariable()) return false
            }
        }

        return true
    }
}
