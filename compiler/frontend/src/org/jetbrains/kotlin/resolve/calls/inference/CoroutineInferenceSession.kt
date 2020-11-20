/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.components.CompletedCallInfo
import org.jetbrains.kotlin.resolve.calls.components.NewConstraintSystemImpl
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.components.stableType
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutor
import org.jetbrains.kotlin.resolve.calls.inference.components.NewTypeSubstitutorByConstructorMap
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.hasBuilderInferenceAnnotation
import org.jetbrains.kotlin.types.*
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

    // Simple calls are calls which might not have gone through type inference, but may contain unsubstituted postponed variables inside their types.
    private val simpleCommonCalls = arrayListOf<KtExpression>()

    private var hasInapplicableCall = false

    override fun shouldRunCompletion(candidate: KotlinResolutionCandidate): Boolean {
        val system = candidate.getSystem() as NewConstraintSystemImpl

        if (system.hasContradiction) return true

        val storage = system.getBuilder().currentStorage()
        fun ResolvedAtom.hasPostponed(): Boolean {
            if (this is PostponedResolvedAtom && !analyzed) return true
            return subResolvedAtoms?.any { it.hasPostponed() } == true
        }

        if (!candidate.isSuitableForBuilderInference()) {
            return true
        }

        return !storage.notFixedTypeVariables.keys.any {
            val variable = storage.allTypeVariables[it]
            val isPostponed = variable != null && variable in storage.postponedTypeVariables
            !isPostponed && !kotlinConstraintSystemCompleter.variableFixationFinder.isTypeVariableHasProperConstraint(
                system,
                it,
            )
        } || candidate.getSubResolvedAtoms().any { it.hasPostponed() }
    }

    private fun KotlinResolutionCandidate.isSuitableForBuilderInference(): Boolean {
        val extensionReceiver = resolvedCall.extensionReceiverArgument
        val dispatchReceiver = resolvedCall.dispatchReceiverArgument
        return when {
            extensionReceiver == null && dispatchReceiver == null -> false
            dispatchReceiver?.receiver?.stableType?.containsStubType() == true -> true
            extensionReceiver?.receiver?.stableType?.containsStubType() == true -> resolvedCall.candidateDescriptor.hasBuilderInferenceAnnotation()
            else -> false
        }
    }

    private fun KotlinType.containsStubType(): Boolean {
        return this.contains {
            it is StubType
        }
    }

    fun addSimpleCall(callExpression: KtExpression) {
        simpleCommonCalls.add(callExpression)
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
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder,
    ): Map<TypeConstructor, UnwrappedType>? {
        val (commonSystem, effectivelyEmptyConstraintSystem) = buildCommonSystem(initialStorage)
        val initialStorageSubstitutor = initialStorage.buildResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false)
        if (effectivelyEmptyConstraintSystem) {
            updateCalls(
                lambda,
                initialStorageSubstitutor,
                commonSystem.errors
            )
            return null
        }

        val context = commonSystem.asConstraintSystemCompleterContext()
        kotlinConstraintSystemCompleter.completeConstraintSystem(
            context,
            builtIns.unitType,
            partiallyResolvedCallsInfo.map { it.callResolutionResult.resultCallAtom },
            completionMode,
            diagnosticsHolder
        )

        val resultingSubstitutor =
            ComposedSubstitutor(initialStorageSubstitutor, commonSystem.buildCurrentSubstitutor() as NewTypeSubstitutor)
        updateCalls(lambda, resultingSubstitutor, commonSystem.errors)

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
                commonSystem.addEqualityConstraint((typeVariable as NewTypeVariable).defaultType, type, CoroutinePosition)
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

        return commonSystem to effectivelyEmptyCommonSystem
    }

    private fun reportErrors(completedCall: CallInfo, resolvedCall: ResolvedCall<*>, errors: List<ConstraintSystemError>) {
        kotlinToResolvedCallTransformer.reportCallDiagnostic(
            completedCall.context,
            trace,
            completedCall.callResolutionResult.resultCallAtom,
            resolvedCall.resultingDescriptor,
            errors.asDiagnostics()
        )
    }

    private fun updateCalls(lambda: ResolvedLambdaAtom, substitutor: NewTypeSubstitutor, errors: List<ConstraintSystemError>) {
        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

        val nonFixedTypesToResult = nonFixedToVariablesSubstitutor.map.mapValues { substitutor.safeSubstitute(it.value) }
        val nonFixedTypesToResultSubstitutor = ComposedSubstitutor(substitutor, nonFixedToVariablesSubstitutor)

        val atomCompleter = createResolvedAtomCompleter(nonFixedTypesToResultSubstitutor, topLevelCallContext)

        for (completedCall in commonCalls) {
            updateCall(completedCall, nonFixedTypesToResultSubstitutor, nonFixedTypesToResult)
            reportErrors(completedCall, completedCall.resolvedCall, errors)
        }

        for (callInfo in partiallyResolvedCallsInfo) {
            val resolvedCall = completeCall(callInfo, atomCompleter) ?: continue
            reportErrors(callInfo, resolvedCall, errors)
        }

        for (simpleCall in simpleCommonCalls) {
            when (simpleCall) {
                is KtCallableReferenceExpression -> updateCallableReferenceType(simpleCall, nonFixedTypesToResultSubstitutor)
                else -> throw Exception("Unsupported call expression type")
            }
        }

        atomCompleter.completeAll(lambda)
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

    private fun updateCallableReferenceType(expression: KtCallableReferenceExpression, substitutor: NewTypeSubstitutor) {
        val functionDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression) as? SimpleFunctionDescriptorImpl ?: return
        val returnType = functionDescriptor.returnType

        fun KotlinType.substituteAndApproximate() = typeApproximator.approximateDeclarationType(
            substitutor.safeSubstitute(this.unwrap()),
            local = true,
            languageVersionSettings = topLevelCallContext.languageVersionSettings
        )

        if (returnType != null && returnType.contains { it is StubType }) {
            functionDescriptor.setReturnType(returnType.substituteAndApproximate())
        }

        for (valueParameter in functionDescriptor.valueParameters) {
            if (valueParameter !is ValueParameterDescriptorImpl || valueParameter.type !is StubType)
                continue

            valueParameter.setOutType(valueParameter.type.substituteAndApproximate())
        }
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

    /*
     * It's used only for `+=` resolve to clear calls info before the second analysis of right side.
     * TODO: remove it after moving `+=` resolve into OR mechanism
     */
    fun clearCallsInfoByContainingElement(containingElement: KtElement) {
        commonCalls.removeIf remove@{ callInfo ->
            val atom = callInfo.callResolutionResult.resultCallAtom.atom
            if (atom !is PSIKotlinCallImpl) return@remove false

            containingElement.anyDescendantOfType<KtElement> { it == atom.psiCall.callElement }
        }
    }
}

class ComposedSubstitutor(val left: NewTypeSubstitutor, val right: NewTypeSubstitutor) : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
        val rightSubstitution = right.substituteNotNullTypeWithConstructor(constructor)
        return left.substituteNotNullTypeWithConstructor(rightSubstitution?.constructor ?: constructor) ?: rightSubstitution
    }

    override val isEmpty: Boolean get() = left.isEmpty && right.isEmpty
}
