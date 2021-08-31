/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirAnnotatedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirArgumentList
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.fir.visitors.transformSingle
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.BuilderInferencePosition
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.BUILDER_INFERENCE_ANNOTATION_FQ_NAME
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

class FirBuilderInferenceSession(
    private val lambda: FirAnonymousFunction,
    resolutionContext: ResolutionContext,
    private val stubsForPostponedVariables: Map<ConeTypeVariable, ConeStubType>,
) : AbstractManyCandidatesInferenceSession(resolutionContext) {
    private val commonCalls: MutableList<Pair<FirStatement, Candidate>> = mutableListOf()

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        val candidate = call.candidate
        val system = candidate.system

        if (system.hasContradiction) return true
        if (!candidate.isSuitableForBuilderInference()) return true


        val storage = system.getBuilder().currentStorage()

        if (call.hasPostponed()) return true

        return storage.notFixedTypeVariables.keys.all {
            val variable = storage.allTypeVariables[it]
            val isPostponed = variable != null && variable in storage.postponedTypeVariables
            isPostponed || components.callCompleter.completer.variableFixationFinder.isTypeVariableHasProperConstraint(system, it)
        }
    }

    private fun Candidate.isSuitableForBuilderInference(): Boolean {
        val extensionReceiver = extensionReceiverValue
        val dispatchReceiver = dispatchReceiverValue
        return when {
            extensionReceiver == null && dispatchReceiver == null -> false
            dispatchReceiver?.type?.containsStubType() == true -> true
            extensionReceiver?.type?.containsStubType() == true -> symbol.fir.hasBuilderInferenceAnnotation()
            else -> false
        }
    }

    private fun ConeKotlinType.containsStubType(): Boolean {
        return this.contains {
            it is ConeStubTypeForBuilderInference
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

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        if (skipCall(call)) return
        commonCalls += call to candidate
    }

    override fun <T> writeOnlyStubs(call: T): Boolean where T : FirResolvable, T : FirStatement {
        return !skipCall(call)
    }

    @Suppress("UNUSED_PARAMETER")
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
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode
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
            completionMode,
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
                commonSystem.addEqualityConstraint((typeVariable as ConeTypeVariable).defaultType, type, BuilderInferencePosition)
                introducedConstraint = true
            }
        }

        return introducedConstraint
    }

    private fun updateCalls(commonSystem: NewConstraintSystemImpl) {
        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()
        val commonSystemSubstitutor = commonSystem.buildCurrentSubstitutor() as ConeSubstitutor
        val nonFixedTypesToResultSubstitutor = ConeComposedSubstitutor(commonSystemSubstitutor, nonFixedToVariablesSubstitutor)

        val stubTypeSubstitutor = FirStubTypeTransformer(nonFixedTypesToResultSubstitutor)
        lambda.transformSingle(stubTypeSubstitutor, null)
        // TODO: support diagnostics, see [CoroutineInferenceSession#updateCalls]

        val completionResultsWriter = components.callCompleter.createCompletionResultsWriter(nonFixedTypesToResultSubstitutor)
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

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): FirTypeRef =
        substitutor.substituteOrNull(resolvedTypeRef.type)?.let {
            resolvedTypeRef.withReplacedConeType(it)
        } ?: resolvedTypeRef

    override fun transformArgumentList(argumentList: FirArgumentList, data: Nothing?): FirArgumentList =
        argumentList.transformArguments(this, data)
}

private val BUILDER_INFERENCE_ANNOTATION_CLASS_ID = ClassId.topLevel(BUILDER_INFERENCE_ANNOTATION_FQ_NAME)

fun FirElement.hasBuilderInferenceAnnotation(): Boolean =
    (this as? FirAnnotatedDeclaration)?.hasAnnotation(BUILDER_INFERENCE_ANNOTATION_CLASS_ID) == true
