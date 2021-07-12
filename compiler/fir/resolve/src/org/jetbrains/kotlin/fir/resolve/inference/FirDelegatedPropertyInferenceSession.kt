/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeVariableTypeConstructor
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.DelegatedPropertyConstraintPosition
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class FirDelegatedPropertyInferenceSession(
    val property: FirProperty,
    initialCall: FirExpression,
    resolutionContext: ResolutionContext,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
) : AbstractManyCandidatesInferenceSession(resolutionContext) {
    init {
        val initialCandidate = (initialCall as? FirResolvable)
            ?.calleeReference
            ?.safeAs<FirNamedReferenceWithCandidate>()
            ?.candidate
        if (initialCandidate != null) {
            addPartiallyResolvedCall(initialCall)
        }
    }

    val expectedType: ConeKotlinType? by lazy { property.returnTypeRef.coneTypeSafe() }
    private val unitType: ConeKotlinType = components.session.builtinTypes.unitType.type
    private lateinit var resultingConstraintSystem: NewConstraintSystem

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = false

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? = null

    override fun <T> shouldCompleteResolvedSubAtomsOf(call: T): Boolean where T : FirResolvable, T : FirStatement = true

    fun completeCandidates(): List<FirResolvable> {
        @Suppress("UNCHECKED_CAST")
        val resolvedCalls = partiallyResolvedCalls.map { it.first }
        val commonSystem = components.session.inferenceComponents.createConstraintSystem().apply {
            addOtherSystem(currentConstraintSystem)
        }
        prepareForCompletion(commonSystem, resolvedCalls)
        resolutionContext.bodyResolveContext.withInferenceSession(DEFAULT) {
            @Suppress("UNCHECKED_CAST")
            components.callCompleter.completer.complete(
                commonSystem.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.FULL,
                resolvedCalls as List<FirStatement>,
                unitType, resolutionContext
            ) {
                postponedArgumentsAnalyzer.analyze(
                    commonSystem.asPostponedArgumentsAnalyzerContext(),
                    it,
                    resolvedCalls.first().candidate,
                    ConstraintSystemCompletionMode.FULL,
                )
            }
        }

        for ((_, candidate) in partiallyResolvedCalls) {
            for (error in commonSystem.errors) {
                candidate.system.addError(error)
            }
        }

        resultingConstraintSystem = commonSystem
        return resolvedCalls
    }

    private fun prepareForCompletion(commonSystem: NewConstraintSystem, partiallyResolvedCalls: List<FirResolvable>) {
        val csBuilder = commonSystem.getBuilder()
        for (call in partiallyResolvedCalls) {
            val candidate = call.candidate
            when ((call.calleeReference as FirNamedReference).name) {
                OperatorNameConventions.GET_VALUE -> candidate.addConstraintsForGetValueMethod(csBuilder)
                OperatorNameConventions.SET_VALUE -> candidate.addConstraintsForSetValueMethod(csBuilder)
            }
        }
    }

    fun createFinalSubstitutor(): ConeSubstitutor {
        return resultingConstraintSystem.asReadOnlyStorage()
            .buildAbstractResultingSubstitutor(components.session.inferenceComponents.ctx) as ConeSubstitutor
    }

    private fun Candidate.addConstraintsForGetValueMethod(commonSystem: ConstraintSystemBuilder) {
        if (expectedType != null) {
            val accessor = symbol.fir as? FirSimpleFunction ?: return
            val unsubstitutedReturnType = accessor.returnTypeRef.coneType

            val substitutedReturnType = substitutor.substituteOrSelf(unsubstitutedReturnType)
            commonSystem.addSubtypeConstraint(substitutedReturnType, expectedType!!, DelegatedPropertyConstraintPosition(callInfo.callSite))
        }

        addConstraintForThis(commonSystem)
    }

    private fun Candidate.addConstraintsForSetValueMethod(commonSystem: ConstraintSystemBuilder) {
        if (expectedType != null) {
            val accessor = symbol.fir as? FirSimpleFunction ?: return
            val unsubstitutedParameterType = accessor.valueParameters.getOrNull(2)?.returnTypeRef?.coneType ?: return

            val substitutedReturnType = substitutor.substituteOrSelf(unsubstitutedParameterType)
            commonSystem.addSubtypeConstraint(expectedType!!, substitutedReturnType, DelegatedPropertyConstraintPosition(callInfo.callSite))
        }

        addConstraintForThis(commonSystem)
    }

    private fun Candidate.addConstraintForThis(commonSystem: ConstraintSystemBuilder) {
        val typeOfThis: ConeKotlinType = property.receiverTypeRef?.coneType
            ?: when (val container = components.container) {
                is FirRegularClass -> container.defaultType()
                is FirAnonymousObject -> container.defaultType()
                is FirCallableDeclaration -> container.dispatchReceiverType
                else -> null
            } ?: components.session.builtinTypes.nullableNothingType.type
        val valueParameterForThis = (symbol as? FirFunctionSymbol<*>)?.fir?.valueParameters?.firstOrNull() ?: return
        val substitutedType = substitutor.substituteOrSelf(valueParameterForThis.returnTypeRef.coneType)
        commonSystem.addSubtypeConstraint(typeOfThis, substitutedType, DelegatedPropertyConstraintPosition(callInfo.callSite))
    }

    override fun <T> writeOnlyStubs(call: T): Boolean where T : FirResolvable, T : FirStatement = false
}
