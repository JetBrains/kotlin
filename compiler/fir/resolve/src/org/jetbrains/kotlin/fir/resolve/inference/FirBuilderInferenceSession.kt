/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.CoroutinePosition
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

class FirBuilderInferenceSession(
    resolutionContext: ResolutionContext,
    private val stubsForPostponedVariables: Map<ConeTypeVariable, ConeStubType>,
) : AbstractManyCandidatesInferenceSession(resolutionContext) {
    private val commonCalls: MutableList<Pair<FirStatement, Candidate>> = mutableListOf()

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        val candidate = call.candidate
        val system = candidate.system

        if (system.hasContradiction) return true

        val storage = system.getBuilder().currentStorage()

        return !storage.notFixedTypeVariables.keys.any {
            val variable = storage.allTypeVariables[it]
            val isPostponed = variable != null && variable in storage.postponedTypeVariables
            !isPostponed && !components.callCompleter.completer.variableFixationFinder.isTypeVariableHasProperConstraint(system, it)
        } || call.hasPostponed()
    }

    private fun FirStatement.hasPostponed(): Boolean {
        var result = false
        processAllContainingCallCandidates(processBlocks = false) {
            result = result || it.hasPostponed()
        }
        return result
    }

    private fun Candidate.hasPostponed(): Boolean {
        return postponedAtoms.any { !it.analyzed }
    }

    override fun <T> addCompetedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        if (skipCall(call)) return
        commonCalls += call to candidate
    }

    override fun <T> writeOnlyStubs(call: T): Boolean where T : FirResolvable, T : FirStatement {
        return !skipCall(call)
    }

    private fun <T> skipCall(call: T): Boolean where T : FirResolvable, T : FirStatement {
        // TODO: what is FIR analog?
        // if (descriptor is FakeCallableDescriptorForObject) return true
        // if (!DescriptorUtils.isObject(descriptor) && isInLHSOfDoubleColonExpression(callInfo)) return true

        return false
    }

    override val currentConstraintSystem: ConstraintStorage
        get() = ConstraintStorage.Empty

    override fun <T> shouldCompleteResolvedSubAtomsOf(call: T): Boolean where T : FirResolvable, T : FirStatement {
        return true
    }

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? {
        val (commonSystem, effectivelyEmptyConstraintSystem) = buildCommonSystem(initialStorage)
        if (effectivelyEmptyConstraintSystem) {
            updateCalls(commonSystem)
            return null
        }

        val context = commonSystem.asConstraintSystemCompleterContext()
        @Suppress("UNCHECKED_CAST")
        components.callCompleter.completer.complete(
            context,
            ConstraintSystemCompletionMode.FULL,
            partiallyResolvedCalls.map { it.first as FirStatement },
            components.session.builtinTypes.unitType.type, resolutionContext,
            collectVariablesFromContext = true
        ) {
            error("Shouldn't be called in complete constraint system mode")
        }

        updateCalls(commonSystem)

        @Suppress("UNCHECKED_CAST")
        return commonSystem.fixedTypeVariables as Map<ConeTypeVariableTypeConstructor, ConeKotlinType>
    }

    private fun buildCommonSystem(initialStorage: ConstraintStorage): Pair<NewConstraintSystemImpl, Boolean> {
        val commonSystem = components.session.inferenceComponents.createConstraintSystem()
        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

        integrateConstraints(commonSystem, initialStorage, nonFixedToVariablesSubstitutor, false)

        var effectivelyEmptyCommonSystem = true

        for ((_, candidate) in commonCalls) {
            val hasConstraints =
                integrateConstraints(commonSystem, candidate.system.asReadOnlyStorage(), nonFixedToVariablesSubstitutor, false)
            if (hasConstraints) effectivelyEmptyCommonSystem = false
        }
        for ((_, candidate) in partiallyResolvedCalls) {
            val hasConstraints =
                integrateConstraints(commonSystem, candidate.system.asReadOnlyStorage(), nonFixedToVariablesSubstitutor, true)
            if (hasConstraints) effectivelyEmptyCommonSystem = false
        }

        // TODO: add diagnostics holder
//        for (diagnostic in diagnostics) {
//            commonSystem.addError(diagnostic)
//        }

        return commonSystem to effectivelyEmptyCommonSystem
    }

    private fun createNonFixedTypeToVariableSubstitutor(): ConeSubstitutor {
        val ctx = components.session.inferenceComponents.ctx

        val bindings = mutableMapOf<TypeConstructorMarker, ConeKotlinType>()
        for ((variable, nonFixedType) in stubsForPostponedVariables) {
            bindings[nonFixedType.variable.typeConstructor] = variable.defaultType
        }

        return ctx.typeSubstitutorByTypeConstructor(bindings)
    }

    private fun integrateConstraints(
        commonSystem: NewConstraintSystemImpl,
        storage: ConstraintStorage,
        nonFixedToVariablesSubstitutor: ConeSubstitutor,
        shouldIntegrateAllConstraints: Boolean
    ): Boolean {
        storage.notFixedTypeVariables.values.forEach { commonSystem.registerVariable(it.typeVariable) }

        /*
        * storage can contain the following substitutions:
        *  TypeVariable(A) -> ProperType
        *  TypeVariable(B) -> Special-Non-Fixed-Type
        *
        * while substitutor from parameter map non-fixed types to the original type variable
        * */
        val callSubstitutor = storage.buildAbstractResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false) as ConeSubstitutor

        var introducedConstraint = false

        for (initialConstraint in storage.initialConstraints) {
            val lower = nonFixedToVariablesSubstitutor.substituteOrSelf(callSubstitutor.substituteOrSelf(initialConstraint.a as ConeKotlinType)) // TODO: SUB
            val upper = nonFixedToVariablesSubstitutor.substituteOrSelf(callSubstitutor.substituteOrSelf(initialConstraint.b as ConeKotlinType)) // TODO: SUB

            if (commonSystem.isProperType(lower) && commonSystem.isProperType(upper)) continue

            introducedConstraint = true

            when (initialConstraint.constraintKind) {
                ConstraintKind.LOWER -> error("LOWER constraint shouldn't be used, please use UPPER")

                ConstraintKind.UPPER -> commonSystem.addSubtypeConstraint(lower, upper, initialConstraint.position)

                ConstraintKind.EQUALITY ->
                    with(commonSystem) {
                        addSubtypeConstraint(lower, upper, initialConstraint.position)
                        addSubtypeConstraint(upper, lower, initialConstraint.position)
                    }
            }
        }

        if (shouldIntegrateAllConstraints) {
            for ((variableConstructor, type) in storage.fixedTypeVariables) {
                val typeVariable = storage.allTypeVariables.getValue(variableConstructor)
                commonSystem.registerVariable(typeVariable)
                commonSystem.addEqualityConstraint((typeVariable as ConeTypeVariable).defaultType, type, CoroutinePosition)
                introducedConstraint = true
            }
        }

        return introducedConstraint
    }

    // TODO: besides calls, perhaps use the stub type substitutor for all top-level expressions inside the lambda
    private fun updateCalls(commonSystem: NewConstraintSystemImpl) {
        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()
        val commonSystemSubstitutor = commonSystem.buildCurrentSubstitutor() as ConeSubstitutor
        val nonFixedTypesToResultSubstitutor = ConeComposedSubstitutor(commonSystemSubstitutor, nonFixedToVariablesSubstitutor)
        val completionResultsWriter = components.callCompleter.createCompletionResultsWriter(nonFixedTypesToResultSubstitutor)

        val stubTypeSubstitutor = FirStubTypeTransformer(nonFixedTypesToResultSubstitutor)
        for ((completedCall, _) in commonCalls) {
            completedCall.transformSingle(stubTypeSubstitutor, null)
            // TODO: support diagnostics, see [CoroutineInferenceSession#updateCalls]
        }

        for ((call, _) in partiallyResolvedCalls) {
            call.transformSingle(completionResultsWriter, null)
            // TODO: support diagnostics, see [CoroutineInferenceSession#updateCalls]
        }
    }
}

class ConeComposedSubstitutor(val left: ConeSubstitutor, val right: ConeSubstitutor) : ConeSubstitutor() {
    override fun substituteOrNull(type: ConeKotlinType): ConeKotlinType? {
        val rightSubstitution = right.substituteOrNull(type)
        return left.substituteOrNull(rightSubstitution ?: type)
    }
}

class FirStubTypeTransformer(
    private val substitutor: ConeSubstitutor
) : FirDefaultTransformer<Nothing?>() {

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E).compose()
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): CompositeTransformResult<FirTypeRef> =
        substitutor.substituteOrNull(resolvedTypeRef.type)?.let {
            resolvedTypeRef.withReplacedConeType(it).compose()
        } ?: resolvedTypeRef.compose()

    override fun transformArgumentList(argumentList: FirArgumentList, data: Nothing?): CompositeTransformResult<FirArgumentList> =
        argumentList.transformArguments(this, data).compose()
}
