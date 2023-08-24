/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.NotFixedTypeToVariableSubstitutorForDelegateInference
import org.jetbrains.kotlin.fir.resolve.substitution.replaceStubsAndTypeVariablesToErrors
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.ConstraintSystemBuilder
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.BuilderInferencePosition
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.registerTypeVariableIfNotPresent
import org.jetbrains.kotlin.types.model.*

class FirDelegatedPropertyInferenceSession(
    val property: FirProperty,
    resolutionContext: ResolutionContext,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
) : FirInferenceSessionForChainedResolve(resolutionContext) {

    private val currentConstraintSystem = components.session.inferenceComponents.createConstraintSystem()
    override val currentConstraintStorage: ConstraintStorage get() = currentConstraintSystem.currentStorage()

    private val unitType: ConeClassLikeType = components.session.builtinTypes.unitType.type
    private lateinit var resultingConstraintSystem: NewConstraintSystem

    private fun ConeKotlinType.containsStubType(): Boolean {
        return this.contains {
            it is ConeStubTypeForChainInference
        }
    }

    private fun integrateResolvedCall(storage: ConstraintStorage) {
        registerSyntheticVariables(storage)
        val stubToTypeVariableSubstitutor = createToSyntheticTypeVariableSubstitutor()
        integrateConstraints(
            currentConstraintSystem,
            storage,
            stubToTypeVariableSubstitutor,
            false
        )
    }

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        partiallyResolvedCalls += call to candidate
        if (candidate.isSuccessful) {
            integrateResolvedCall(candidate.system.asReadOnlyStorage())
        }
    }

    override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {
        super.addPartiallyResolvedCall(call)
        if (call.candidate.isSuccessful) {
            integrateResolvedCall(call.candidate.system.currentStorage())
        }
    }

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        val callee = call.calleeReference as? FirNamedReferenceWithCandidate ?: return true

        if (callee.candidate.system.hasContradiction) return true

        val hasStubType =
            callee.candidate.chosenExtensionReceiver?.resolvedType?.containsStubType() ?: false
                    || callee.candidate.dispatchReceiver?.resolvedType?.containsStubType() ?: false

        if (!hasStubType) {
            return true
        }

        val system = call.candidate.system

        val storage = system.getBuilder().currentStorage()

        if (call.hasPostponed()) return true

        return storage.notFixedTypeVariables.keys.all {
            val variable = storage.allTypeVariables[it]
            val isPostponed = variable != null && variable in storage.postponedTypeVariables
            isPostponed || isSyntheticTypeVariable(variable!!) ||
                    components.callCompleter.completer.variableFixationFinder.isTypeVariableHasProperConstraint(system, it)
        }
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

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        constraintSystemBuilder: ConstraintSystemBuilder,
        completionMode: ConstraintSystemCompletionMode
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? = null

    private fun createNonFixedTypeToVariableSubstitutor(): NotFixedTypeToVariableSubstitutorForDelegateInference {
        val typeContext = components.session.typeContext

        val bindings = mutableMapOf<TypeVariableMarker, ConeKotlinType>()
        for ((variable, synthetic) in syntheticTypeVariableByTypeVariable) {
            bindings[synthetic] = variable.defaultType(typeContext) as ConeKotlinType
        }

        return NotFixedTypeToVariableSubstitutorForDelegateInference(bindings, typeContext)
    }


    /*
     * This creates Stub-preserving substitution to synthetic type variables
     * Stub(R) => Stub(_R)
     * R => _R
     */
    private fun createToSyntheticTypeVariableSubstitutor(): ConeSubstitutor {

        val typeContext = components.session.typeContext
        val bindings = mutableMapOf<TypeConstructorMarker, ConeKotlinType>()
        for ((variable, syntheticVariable) in syntheticTypeVariableByTypeVariable) {
            bindings[variable.freshTypeConstructor(typeContext)] = syntheticVariable.defaultType
        }

        return typeContext.typeSubstitutorByTypeConstructor(bindings)
    }

    override fun hasSyntheticTypeVariables(): Boolean {
        return syntheticTypeVariableByTypeVariable.isNotEmpty()
    }

    override fun isSyntheticTypeVariable(typeVariable: TypeVariableMarker): Boolean {
        return typeVariable in syntheticTypeVariableByTypeVariable.values
    }

    override fun fixSyntheticTypeVariableWithNotEnoughInformation(
        typeVariable: TypeVariableMarker,
        completionContext: ConstraintSystemCompletionContext
    ) {
        typeVariable as ConeTypeVariable
        completionContext.fixVariable(
            typeVariable,
            ConeStubTypeForSyntheticFixation(
                ConeStubTypeConstructor(typeVariable, isTypeVariableInSubtyping = false, isForFixation = true),
                ConeNullability.create(typeVariable.defaultType.isMarkedNullable)
            ),
            ConeFixVariableConstraintPosition(typeVariable)
        )
    }

    fun completeCandidates(): List<FirResolvable> {
        val commonSystem = components.session.inferenceComponents.createConstraintSystem()

        val notCompletedCalls = partiallyResolvedCalls.mapNotNull { partiallyResolvedCall ->
            partiallyResolvedCall.first.takeIf { resolvable ->
                resolvable.candidate() != null
            }
        }

        val stubToTypeVariableSubstitutor = createNonFixedTypeToVariableSubstitutor()

        for ((call, candidate) in partiallyResolvedCalls) {
            if (candidate.isSuccessful) {
                integrateConstraints(
                    commonSystem,
                    candidate.system.asReadOnlyStorage(),
                    stubToTypeVariableSubstitutor,
                    call.candidate() != null
                )
            }
        }


        resolutionContext.bodyResolveContext.withInferenceSession(DEFAULT) {
            @Suppress("UNCHECKED_CAST")
            components.callCompleter.completer.complete(
                commonSystem.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.FULL,
                notCompletedCalls as List<FirStatement>,
                unitType, resolutionContext
            ) { lambdaAtom ->
                // Reversed here bc we want top-most call to avoid exponential visit
                val containingCandidateForLambda = notCompletedCalls.asReversed().first {
                    var found = false
                    it.processAllContainingCallCandidates(processBlocks = true) { subCandidate ->
                        if (subCandidate.postponedAtoms.contains(lambdaAtom)) {
                            found = true
                        }
                    }
                    found
                }.candidate
                postponedArgumentsAnalyzer.analyze(
                    commonSystem,
                    lambdaAtom,
                    containingCandidateForLambda,
                    ConstraintSystemCompletionMode.FULL,
                )
            }
        }

        for ((_, candidate) in partiallyResolvedCalls) {
            for (error in commonSystem.errors) {
                candidate.addDiagnostic(InferenceError(error))
            }
        }

        resultingConstraintSystem = commonSystem
        return notCompletedCalls
    }

    fun createFinalSubstitutor(): ConeSubstitutor {
        val stubTypeSubstitutor = createNonFixedTypeToVariableSubstitutor()

        val typeContext = components.session.typeContext
        val resultSubstitutor = resultingConstraintSystem.asReadOnlyStorage()
            .buildAbstractResultingSubstitutor(typeContext) as ConeSubstitutor
        return ChainedSubstitutor(stubTypeSubstitutor, resultSubstitutor)
            .replaceStubsAndTypeVariablesToErrors(typeContext, stubTypesByTypeVariable.values.map { it.constructor })
    }

    private val stubTypesByTypeVariable: MutableMap<ConeTypeVariable, ConeStubType> = mutableMapOf()
    private val syntheticTypeVariableByTypeVariable = mutableMapOf<TypeVariableMarker, ConeTypeVariable>()

    private fun registerSyntheticVariables(storage: ConstraintStorage) {
        for (variableWithConstraints in storage.notFixedTypeVariables.values) {
            val variable = variableWithConstraints.typeVariable as ConeTypeVariable

            val syntheticVariable = syntheticTypeVariableByTypeVariable.getOrPut(variable) {
                ConeTypeVariable("+" + variable.typeConstructor.name).also {
                    currentConstraintSystem.registerVariable(it)
                }
            }

            stubTypesByTypeVariable.getOrPut(variable) {
                ConeStubTypeForChainInference(
                    syntheticVariable,
                    ConeNullability.create(syntheticVariable.defaultType.isMarkedNullable)
                )
            }
        }
    }

    override fun createSyntheticStubTypes(system: NewConstraintSystemImpl): Map<TypeConstructorMarker, ConeStubType> {

        val bindings = mutableMapOf<TypeConstructorMarker, ConeStubType>()
        registerSyntheticVariables(system.currentStorage())
        for (variable in system.postponedTypeVariables) {
            variable as ConeTypeVariable

            bindings[variable.typeConstructor] = stubTypesByTypeVariable[variable]!!
        }

        return bindings
    }

    override fun registerStubTypes(map: Map<TypeVariableMarker, StubTypeMarker>) {
//        @Suppress("UNCHECKED_CAST")
//        stubTypesByTypeVariable.putAll(map as Map<ConeTypeVariable, ConeStubType>)
    }


    private fun integrateConstraints(
        commonSystem: NewConstraintSystemImpl,
        storage: ConstraintStorage,
        nonFixedToVariablesSubstitutor: ConeSubstitutor,
        shouldIntegrateAllConstraints: Boolean
    ) {
        if (shouldIntegrateAllConstraints) {
            storage.notFixedTypeVariables.values.forEach {
                if (isSyntheticTypeVariable(it.typeVariable)) return@forEach
                commonSystem.registerTypeVariableIfNotPresent(it.typeVariable)
            }
        }
        /*
        * storage can contain the following substitutions:
        *  TypeVariable(A) -> ProperType
        *  TypeVariable(B) -> Special-Non-Fixed-Type
        *
        * while substitutor from parameter map non-fixed types to the original type variable
        * */
        val callSubstitutor =
            storage.buildAbstractResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false) as ConeSubstitutor

        for (initialConstraint in storage.initialConstraints) {
            integrateConstraintToSystem(
                commonSystem, initialConstraint, callSubstitutor, nonFixedToVariablesSubstitutor, storage.fixedTypeVariables
            )
        }

        if (shouldIntegrateAllConstraints) {
            for ((variableConstructor, type) in storage.fixedTypeVariables) {
                val typeVariable = storage.allTypeVariables.getValue(variableConstructor)
                if (isSyntheticTypeVariable(typeVariable)) continue

                commonSystem.registerTypeVariableIfNotPresent(typeVariable)
                commonSystem.addEqualityConstraint((typeVariable as ConeTypeVariable).defaultType, type, BuilderInferencePosition)
            }
        }
    }
}
