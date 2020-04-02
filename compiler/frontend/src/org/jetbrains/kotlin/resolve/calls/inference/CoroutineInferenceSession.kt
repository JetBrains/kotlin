/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtDoubleColonExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorUtils
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
import org.jetbrains.kotlin.resolve.calls.inference.model.*
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
import org.jetbrains.kotlin.types.typeUtil.contains
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
    private var hasInapplicableCall = false

    override fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean {
        val system = candidate.getSystem() as NewConstraintSystemImpl

        if (system.hasContradiction) return true

        val storage = system.getBuilder().currentStorage()
        fun ResolvedAtom.hasPostponed(): Boolean {
            if (this is PostponedResolvedAtom && !analyzed) return true
            return subResolvedAtoms?.any { it.hasPostponed() } == true
        }

        return !storage.notFixedTypeVariables.keys.any {
            val variable = storage.allTypeVariables[it]
            val isPostponed = variable != null && variable in storage.postponedTypeVariables
            !isPostponed && !kotlinConstraintSystemCompleter.variableFixationFinder.isTypeVariableHasProperConstraint(system, it)
        } || candidate.getSubResolvedAtoms().any { it.hasPostponed() }
    }

    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {
        require(callInfo is PSICompletedCallInfo) { "Wrong instance of callInfo: $callInfo" }

        if (skipCall(callInfo.callResolutionResult)) return

        commonCalls.add(callInfo)

        val resultingDescriptor = callInfo.resolvedCall.resultingDescriptor

        // This check is similar to one for old inference, see getCoroutineInferenceData() function
        val checkCall = resultingDescriptor is LocalVariableDescriptor || anyReceiverContainStubType(resultingDescriptor)

        if (!checkCall) return

        val isApplicableCall =
            callComponents.statelessCallbacks.isApplicableCallForBuilderInference(
                resultingDescriptor,
                callComponents.languageVersionSettings
            )

        if (!isApplicableCall) {
            hasInapplicableCall = true
        }
    }

    private fun anyReceiverContainStubType(descriptor: CallableDescriptor): Boolean {
        return descriptor.dispatchReceiverParameter?.type?.contains { it is StubType } == true ||
                descriptor.extensionReceiverParameter?.type?.contains { it is StubType } == true
    }

    fun hasInapplicableCall(): Boolean = hasInapplicableCall

    override fun writeOnlyStubs(callInfo: SingleCallResolutionResult): Boolean {
        return !skipCall(callInfo)
    }

    private fun skipCall(callInfo: SingleCallResolutionResult): Boolean {
        val descriptor = callInfo.resultCallAtom.candidateDescriptor

        // FakeCallableDescriptorForObject can't introduce new information for inference,
        // so it's safe to complete it fully
        if (descriptor is FakeCallableDescriptorForObject) return true

        // In this case temporary trace isn't committed during resolve of expressions like A::class, see resolveDoubleColonLHS
        if (!DescriptorUtils.isObject(descriptor) && isInLHSOfDoubleColonExpression(callInfo)) return true

        return false
    }

    private fun isInLHSOfDoubleColonExpression(callInfo: SingleCallResolutionResult): Boolean {
        val callElement = callInfo.resultCallAtom.atom.psiKotlinCall.psiCall.callElement
        val lhs = callElement.getParentOfType<KtDoubleColonExpression>(strict = false)?.lhs
        if (lhs !is KtReferenceExpression && lhs !is KtDotQualifiedExpression) return false

        return lhs.isAncestor(callElement)
    }

    override fun currentConstraintSystem(): ConstraintStorage {
        return ConstraintStorage.Empty
    }

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        diagnosticsHolder: KotlinDiagnosticsHolder
    ): Map<TypeConstructor, UnwrappedType>? {
        val (commonSystem, effectivelyEmptyConstraintSystem) = buildCommonSystem(initialStorage)
        if (effectivelyEmptyConstraintSystem) {
            updateCalls(lambda, commonSystem)
            return null
        }

        val context = commonSystem.asConstraintSystemCompleterContext()
        kotlinConstraintSystemCompleter.completeConstraintSystem(
            context,
            builtIns.unitType,
            partiallyResolvedCallsInfo.map { it.callResolutionResult.resultCallAtom },
            diagnosticsHolder
        )

        updateCalls(lambda, commonSystem)

        return commonSystem.fixedTypeVariables.cast() // TODO: SUB
    }

    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true

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
        nonFixedToVariablesSubstitutor: NewTypeSubstitutor,
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
        val callSubstitutor = storage.buildResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false)

        var introducedConstraint = false

        for (initialConstraint in storage.initialConstraints) {
            val lower = nonFixedToVariablesSubstitutor.safeSubstitute(callSubstitutor.safeSubstitute(initialConstraint.a as UnwrappedType)) // TODO: SUB
            val upper = nonFixedToVariablesSubstitutor.safeSubstitute(callSubstitutor.safeSubstitute(initialConstraint.b as UnwrappedType)) // TODO: SUB

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
                commonSystem.addEqualityConstraint((typeVariable as NewTypeVariable).defaultType, type, CoroutinePosition())
                introducedConstraint = true
            }
        }

        return introducedConstraint
    }

    private fun buildCommonSystem(initialStorage: ConstraintStorage): Pair<NewConstraintSystemImpl, Boolean> {
        val commonSystem = NewConstraintSystemImpl(callComponents.constraintInjector, builtIns)

        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

        integrateConstraints(commonSystem, initialStorage, nonFixedToVariablesSubstitutor, false)

        var effectivelyEmptyCommonSystem = true

        for (call in commonCalls) {
            val hasConstraints =
                integrateConstraints(commonSystem, call.callResolutionResult.constraintSystem, nonFixedToVariablesSubstitutor, false)
            if (hasConstraints) effectivelyEmptyCommonSystem = false
        }
        for (call in partiallyResolvedCallsInfo) {
            val hasConstraints =
                integrateConstraints(commonSystem, call.callResolutionResult.constraintSystem, nonFixedToVariablesSubstitutor, true)
            if (hasConstraints) effectivelyEmptyCommonSystem = false
        }

        for (diagnostic in diagnostics) {
            commonSystem.addError(diagnostic)
        }

        return commonSystem to effectivelyEmptyCommonSystem
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
        for (callInfo in partiallyResolvedCallsInfo) {
            val resolvedCall = completeCall(callInfo, lambdaAtomCompleter) ?: continue
            kotlinToResolvedCallTransformer.reportCallDiagnostic(
                callInfo.context,
                trace,
                callInfo.callResolutionResult.resultCallAtom,
                resolvedCall.resultingDescriptor,
                commonSystem.diagnostics
            )
        }
        lambdaAtomCompleter.completeAll(lambda)
    }

    private fun updateCall(
        completedCall: PSICompletedCallInfo,
        nonFixedTypesToResultSubstitutor: NewTypeSubstitutor,
        nonFixedTypesToResult: Map<TypeConstructor, UnwrappedType>
    ) {
        val resultingCallSubstitutor = completedCall.callResolutionResult.constraintSystem.fixedTypeVariables.entries
            .associate { it.key to nonFixedTypesToResultSubstitutor.safeSubstitute(it.value as UnwrappedType) } // TODO: SUB

        val resultingSubstitutor =
            NewTypeSubstitutorByConstructorMap((resultingCallSubstitutor + nonFixedTypesToResult).cast()) // TODO: SUB

        val atomCompleter = createResolvedAtomCompleter(
            resultingSubstitutor,
            completedCall.context.replaceBindingTrace(topLevelCallContext.trace)
        )

        completeCall(completedCall, atomCompleter)
    }

    private fun completeCall(
        callInfo: CallInfo,
        atomCompleter: ResolvedAtomCompleter
    ): ResolvedCall<*>? {
        val resultCallAtom = callInfo.callResolutionResult.resultCallAtom
        resultCallAtom.subResolvedAtoms?.forEach { subResolvedAtom ->
            atomCompleter.completeAll(subResolvedAtom)
        }
        val resolvedCall = atomCompleter.completeResolvedCall(resultCallAtom, callInfo.callResolutionResult.diagnostics)

        val callTrace = callInfo.context.trace
        if (callTrace is TemporaryBindingTrace) {
            callTrace.commit()
        }
        return resolvedCall
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
        val rightSubstitution = right.substituteNotNullTypeWithConstructor(constructor)
        return left.substituteNotNullTypeWithConstructor(rightSubstitution?.constructor ?: constructor) ?: rightSubstitution
    }

    override val isEmpty: Boolean get() = left.isEmpty && right.isEmpty
}
