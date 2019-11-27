/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.MissingSupertypesResolver
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.components.CompletedCallInfo
import org.jetbrains.kotlin.resolve.calls.components.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.types.StubType
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.utils.addToStdlib.cast

class CoroutineInferenceSession(
    psiCallResolver: PSICallResolver,
    postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    callComponents: KotlinCallComponents,
    builtIns: KotlinBuiltIns,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val stubsForPostponedVariables: Map<NewTypeVariable, StubType>,
    private val trace: BindingTrace,
    private val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
    private val expressionTypingServices: ExpressionTypingServices,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver
) : ManyCandidatesResolver<CallableDescriptor>(
    psiCallResolver, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter, callComponents, builtIns
) {
    private val commonCalls = arrayListOf<PSICompletedCallInfo>()
    private val diagnostics = arrayListOf<KotlinCallDiagnostic>()

    override fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean = true

    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {
        require(callInfo is PSICompletedCallInfo) { "Wrong instance of callInfo: $callInfo" }

        if (skipCall(callInfo.callResolutionResult)) return

        commonCalls.add(callInfo)

        val isApplicableCall =
            callComponents.statelessCallbacks.isApplicableCallForBuilderInference(
                callInfo.resolvedCall.resultingDescriptor,
                callComponents.languageVersionSettings
            )

        if (!isApplicableCall) {
            diagnostics.add(NonApplicableCallForBuilderInferenceDiagnostic(callInfo.callResolutionResult.resultCallAtom.atom))
        }
    }

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean {
        return !skipCall(callInfo)
    }

    private fun skipCall(callInfo: SingleCallResolutionResult): Boolean {
        // FakeCallableDescriptorForObject can't introduce new information for inference, so it's safe to complete it fully
        if (callInfo.resultCallAtom.candidateDescriptor is FakeCallableDescriptorForObject) return true

        return false
    }

    override fun currentConstraintSystem(): ConstraintStorage {
        return ConstraintStorage.Empty
    }

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage
    ): Map<TypeConstructor, UnwrappedType> {
        val commonSystem = buildCommonSystem(initialStorage)

        val context = commonSystem.asConstraintSystemCompleterContext()
        kotlinConstraintSystemCompleter.completeConstraintSystem(context, builtIns.unitType)

        updateCalls(lambda, commonSystem)

        return commonSystem.fixedTypeVariables.cast() // TODO: SUB
    }

    private fun createNonFixedTypeToVariableSubstitutor(): NewTypeSubstitutorByConstructorMap {
        val bindings = hashMapOf<TypeConstructor, UnwrappedType>()
        for ((variable, nonFixedType) in stubsForPostponedVariables) {
            bindings[nonFixedType.constructor] = variable.defaultType
        }

        return NewTypeSubstitutorByConstructorMap(bindings)
    }

    private fun integrateConstraints(
        commonSystem: NewConstraintSystemImpl,
        storage: ConstraintStorage,
        nonFixedToVariablesSubstitutor: NewTypeSubstitutor
    ) {
        storage.notFixedTypeVariables.values.forEach { commonSystem.registerVariable(it.typeVariable) }

        /*
        * storage can contain the following substitutions:
        *  TypeVariable(A) -> ProperType
        *  TypeVariable(B) -> Special-Non-Fixed-Type
        *
        * while substitutor from parameter map non-fixed types to the original type variable
        * */
        val callSubstitutor = storage.buildResultingSubstitutor(commonSystem) // substitutor only for fixed variables

        for (initialConstraint in storage.initialConstraints) {
            val lower = nonFixedToVariablesSubstitutor.safeSubstitute(callSubstitutor.safeSubstitute(initialConstraint.a as UnwrappedType)) // TODO: SUB
            val upper = nonFixedToVariablesSubstitutor.safeSubstitute(callSubstitutor.safeSubstitute(initialConstraint.b as UnwrappedType)) // TODO: SUB

            if (commonSystem.isProperType(lower) && commonSystem.isProperType(upper)) continue

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
    }

    private fun buildCommonSystem(initialStorage: ConstraintStorage): NewConstraintSystemImpl {
        val commonSystem = NewConstraintSystemImpl(callComponents.constraintInjector, builtIns)

        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

        integrateConstraints(commonSystem, initialStorage, nonFixedToVariablesSubstitutor)

        for (call in commonCalls) {
            integrateConstraints(commonSystem, call.callResolutionResult.constraintSystem, nonFixedToVariablesSubstitutor)
        }
        for (diagnostic in diagnostics) {
            commonSystem.addError(diagnostic)
        }

        return commonSystem
    }

    private fun updateCalls(lambda: ResolvedLambdaAtom, commonSystem: NewConstraintSystemImpl) {
        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()
        val commonSystemSubstitutor = commonSystem.buildCurrentSubstitutor() as NewTypeSubstitutor

        val nonFixedTypesToResult = nonFixedToVariablesSubstitutor.map.mapValues { commonSystemSubstitutor.safeSubstitute(it.value) }

        val nonFixedTypesToResultSubstitutor = ComposedSubstitutor(commonSystemSubstitutor, nonFixedToVariablesSubstitutor)

        for (completedCall in commonCalls) {
            updateCall(completedCall, nonFixedTypesToResultSubstitutor, nonFixedTypesToResult)

            kotlinToResolvedCallTransformer.reportCallDiagnostic(
                completedCall.context,
                trace,
                completedCall.callResolutionResult.resultCallAtom,
                completedCall.resolvedCall.resultingDescriptor,
                commonSystem.diagnostics
            )
        }

        val lambdaAtomCompleter = createResolvedAtomCompleter(nonFixedTypesToResultSubstitutor, topLevelCallContext)
        lambdaAtomCompleter.completeAll(lambda)
    }

    private fun updateCall(
        completedCall: PSICompletedCallInfo,
        nonFixedTypesToResultSubstitutor: NewTypeSubstitutor,
        nonFixedTypesToResult: Map<TypeConstructor, UnwrappedType>
    ) {
        val resultingCallSubstitutor = completedCall.callResolutionResult.constraintSystem.fixedTypeVariables.entries
            .associate { it.key to nonFixedTypesToResultSubstitutor.safeSubstitute(it.value as UnwrappedType) } // TODO: SUB

        val resultingSubstitutor = NewTypeSubstitutorByConstructorMap((resultingCallSubstitutor + nonFixedTypesToResult).cast()) // TODO: SUB

        val atomCompleter = createResolvedAtomCompleter(resultingSubstitutor, completedCall.context)
        val resultCallAtom = completedCall.callResolutionResult.resultCallAtom

        for (subResolvedAtom in resultCallAtom.subResolvedAtoms) {
            atomCompleter.completeAll(subResolvedAtom)
        }
        atomCompleter.completeResolvedCall(resultCallAtom, completedCall.callResolutionResult.diagnostics)

        val callTrace = completedCall.context.trace
        if (callTrace is TemporaryBindingTrace) {
            callTrace.commit()
        }
    }

    private fun createResolvedAtomCompleter(
        resultSubstitutor: NewTypeSubstitutor,
        context: BasicCallResolutionContext
    ): ResolvedAtomCompleter {
        return ResolvedAtomCompleter(
            resultSubstitutor, context, kotlinToResolvedCallTransformer,
            expressionTypingServices, argumentTypeResolver, doubleColonExpressionResolver, builtIns,
            deprecationResolver, moduleDescriptor, context.dataFlowValueFactory, typeApproximator, missingSupertypesResolver
        )
    }
}

class ComposedSubstitutor(val left: NewTypeSubstitutor, val right: NewTypeSubstitutor) : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
        return left.substituteNotNullTypeWithConstructor(
            right.substituteNotNullTypeWithConstructor(constructor)?.constructor ?: constructor
        )
    }
}
