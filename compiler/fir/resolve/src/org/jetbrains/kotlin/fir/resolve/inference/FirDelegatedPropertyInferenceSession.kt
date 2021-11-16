/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.calls.*
import org.jetbrains.kotlin.fir.resolve.substitution.AbstractConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.types.model.*

class FirDelegatedPropertyInferenceSession(
    val property: FirProperty,
    resolutionContext: ResolutionContext,
    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
) : AbstractManyCandidatesInferenceSession(resolutionContext) {

    override val currentConstraintSystem: ConstraintStorage
        get() {
//            return ConstraintStorage.Empty
            val system = components.session.inferenceComponents.createConstraintSystem()

            val stubToTypeVariableSubstitutor = createToSyntheticTypeVariableSubstitutor()
            syntheticTypeVariableByTypeVariable.values.forEach {
                system.registerVariable(it)
            }
            partiallyResolvedCalls.forEach { (_, candidate) ->
                integrateConstraints(
                    system,
                    candidate.system.asReadOnlyStorage(),
                    stubToTypeVariableSubstitutor,
                    false
                )
            }

            return system.currentStorage()
        }

    private val commonSystem = components.session.inferenceComponents.createConstraintSystem()
    private val unitType: ConeKotlinType = components.session.builtinTypes.unitType.type
    private lateinit var resultingConstraintSystem: NewConstraintSystem

    private fun ConeKotlinType.containsStubType(): Boolean {
        return this.contains {
            it is ConeStubTypeForBuilderInference
        }
    }

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        partiallyResolvedCalls += call to candidate
    }

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        val callee = call.calleeReference as? FirNamedReferenceWithCandidate ?: return true

        if (callee.candidate.system.hasContradiction) return true

        val hasStubType =
            callee.candidate.extensionReceiverValue?.type?.containsStubType() ?: false
                    || callee.candidate.dispatchReceiverValue?.type?.containsStubType() ?: false

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
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode
    ): Map<ConeTypeVariableTypeConstructor, ConeKotlinType>? = null

    private fun createNonFixedTypeToVariableSubstitutor(): ConeSubstitutor {
        val typeContext = components.session.typeContext

        val bindings = mutableMapOf<TypeConstructorMarker, ConeKotlinType>()
        for ((variable, stubType) in stubTypesByTypeVariable) {
            bindings[stubType.constructor] = variable.defaultType(typeContext) as ConeKotlinType
        }

        return object : AbstractConeSubstitutor(typeContext) {
            override fun substituteType(type: ConeKotlinType): ConeKotlinType? {
                if (type !is ConeStubType) return null
                return bindings[type.constructor].updateNullabilityIfNeeded(type)
            }
        }
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


    override fun isSyntheticTypeVariable(typeVariable: TypeVariableMarker): Boolean {
        return typeVariable in syntheticTypeVariableByTypeVariable.values
    }

    fun completeCandidates(): List<FirResolvable> {
        @Suppress("UNCHECKED_CAST")
        val notCompletedCalls = partiallyResolvedCalls.mapNotNull { partiallyResolvedCall ->
            partiallyResolvedCall.first.takeIf { resolvable ->
                resolvable.candidate() != null
            }
        }

        val stubToTypeVariableSubstitutor = createNonFixedTypeToVariableSubstitutor()

        partiallyResolvedCalls.forEach { (call, candidate) ->
            integrateConstraints(
                commonSystem,
                candidate.system.asReadOnlyStorage(),
                stubToTypeVariableSubstitutor,
                call.candidate() != null
            )
        }


        resolutionContext.bodyResolveContext.withInferenceSession(DEFAULT) {
            @Suppress("UNCHECKED_CAST")
            components.callCompleter.completer.complete(
                commonSystem.asConstraintSystemCompleterContext(),
                ConstraintSystemCompletionMode.FULL,
                notCompletedCalls as List<FirStatement>,
                unitType, resolutionContext
            ) { lambdaAtom ->
                val containingCandidateForLambda = notCompletedCalls.first {
                    it.candidate.postponedAtoms.contains(lambdaAtom)
                }.candidate
                postponedArgumentsAnalyzer.analyze(
                    commonSystem.asPostponedArgumentsAnalyzerContext(),
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

        val resultSubstitutor = resultingConstraintSystem.asReadOnlyStorage()
            .buildAbstractResultingSubstitutor(components.session.typeContext) as ConeSubstitutor
        return ChainedSubstitutor(stubTypeSubstitutor, resultSubstitutor)
    }

    val stubTypesByTypeVariable: MutableMap<ConeTypeVariable, ConeStubType> = mutableMapOf()
    val stubTypeBySyntheticTypeVariable: MutableMap<ConeTypeVariable, ConeStubType> = mutableMapOf()

    private val syntheticTypeVariableByTypeVariable = mutableMapOf<TypeVariableMarker, ConeTypeVariable>()


    override fun createSyntheticStubTypes(system: NewConstraintSystemImpl): Map<TypeConstructorMarker, ConeStubType> {

        val bindings = mutableMapOf<TypeConstructorMarker, ConeStubType>()

//        val synthToOriginal = syntheticTypeVariableByTypeVariable.entries.associateBy({ it.value }, { it.key })
//
//        for (variable in system.postponedTypeVariables) {
//            variable as ConeTypeVariable
//            if (isSyntheticTypeVariable(variable)) {
//                system.fixVariable(
//                    variable,
//                    ConeStubTypeForBuilderInference(
//                        synthToOriginal[variable]!! as ConeTypeVariable,
//                        ConeNullability.create(variable.defaultType.isMarkedNullable)
//                    ),
//                    ConeFixVariableConstraintPosition(variable)
//                )
//                system.unmarkPostponedVariable(variable)
//            }
//        }

        for (variable in system.postponedTypeVariables) {
            variable as ConeTypeVariable

            val syntheticVariable = syntheticTypeVariableByTypeVariable.getOrPut(variable) {
                ConeTypeVariable("_" + variable.typeConstructor.name)
            }

//            val potentialType = resolutionContext.inferenceComponents.resultTypeResolver.findResultType(
//                system,
//                system.notFixedTypeVariables[variable.typeConstructor]!!,
//                TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
//            )

            bindings[variable.typeConstructor] = stubTypesByTypeVariable.getOrPut(variable) {
                ConeStubTypeForBuilderInference(
                    syntheticVariable,
                    ConeNullability.create(syntheticVariable.defaultType.isMarkedNullable)
                ).also {
                    stubTypeBySyntheticTypeVariable[syntheticVariable] = it
                }
            }
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
    ): Boolean {
        if (shouldIntegrateAllConstraints) {
            storage.notFixedTypeVariables.values.forEach {
                if (isSyntheticTypeVariable(it.typeVariable)) return@forEach
                if (it.typeVariable.freshTypeConstructor(commonSystem.typeSystemContext) !in commonSystem.allTypeVariables) {
                    commonSystem.registerVariable(it.typeVariable)
                }
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

        var introducedConstraint = false

        for (initialConstraint in storage.initialConstraints) {
            val lower =
                nonFixedToVariablesSubstitutor.substituteOrSelf(callSubstitutor.substituteOrSelf(initialConstraint.a as ConeKotlinType)) // TODO: SUB
            val upper =
                nonFixedToVariablesSubstitutor.substituteOrSelf(callSubstitutor.substituteOrSelf(initialConstraint.b as ConeKotlinType)) // TODO: SUB

            if (commonSystem.isProperType(lower) && (lower == upper || commonSystem.isProperType(upper))) continue

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
                if (isSyntheticTypeVariable(typeVariable)) continue

                commonSystem.registerVariable(typeVariable)
                commonSystem.addEqualityConstraint((typeVariable as ConeTypeVariable).defaultType, type, BuilderInferencePosition)
                introducedConstraint = true
            }
        }

        return introducedConstraint
    }
}
