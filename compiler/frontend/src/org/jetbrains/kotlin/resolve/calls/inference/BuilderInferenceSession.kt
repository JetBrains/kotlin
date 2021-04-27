/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.callUtil.toOldSubstitution
import org.jetbrains.kotlin.resolve.calls.components.*
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
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.utils.addToStdlib.cast

class BuilderInferenceSession(
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
    private var nestedBuilderInferenceSessions: MutableSet<BuilderInferenceSession> = mutableSetOf()

    private lateinit var lambda: ResolvedLambdaAtom
    private lateinit var commonSystem: NewConstraintSystemImpl

    init {
        if (topLevelCallContext.inferenceSession is BuilderInferenceSession) {
            topLevelCallContext.inferenceSession.nestedBuilderInferenceSessions.add(this)
        }
    }

    private val commonCalls = arrayListOf<PSICompletedCallInfo>()

    // These calls come from the old type inference
    private val oldDoubleColonExpressionCalls = arrayListOf<KtExpression>()

    private var hasInapplicableCall = false

    override val parentSession = topLevelCallContext.inferenceSession

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

        return storage.notFixedTypeVariables.keys.all {
            val variable = storage.allTypeVariables[it]
            val isPostponed = variable != null && variable in storage.postponedTypeVariables
            isPostponed || kotlinConstraintSystemCompleter.variableFixationFinder.isTypeVariableHasProperConstraint(
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

    fun addOldCallableReferenceCalls(callExpression: KtExpression) {
        oldDoubleColonExpressionCalls.add(callExpression)
    }

    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {
        require(callInfo is PSICompletedCallInfo) { "Wrong instance of callInfo: $callInfo" }

        if (skipCall(callInfo.callResolutionResult)) return

        commonCalls.add(callInfo)
    }

    private fun anyReceiverContainStubType(descriptor: CallableDescriptor): Boolean {
        return descriptor.dispatchReceiverParameter?.type?.contains { it is StubType } == true ||
                descriptor.extensionReceiverParameter?.type?.contains { it is StubType } == true
    }

    private fun isTopLevelBuilderInferenceCall() = findParentBuildInferenceSession() == null

    private fun findParentBuildInferenceSession(): BuilderInferenceSession? {
        var currentSession: InferenceSession? = parentSession

        while (currentSession != null) {
            if (currentSession is BuilderInferenceSession) return currentSession
            currentSession = currentSession.parentSession
        }

        return null
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun findAllParentBuildInferenceSessions() = buildList {
        var currentSession: BuilderInferenceSession? = findParentBuildInferenceSession()

        while (currentSession != null) {
            add(currentSession)
            currentSession = currentSession.findParentBuildInferenceSession()
        }
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

        this.lambda = lambda
        this.commonSystem = commonSystem

        if (effectivelyEmptyConstraintSystem) {
            if (isTopLevelBuilderInferenceCall()) {
                updateAllCalls(initialStorageSubstitutor)
            }
            return null
        }

        kotlinConstraintSystemCompleter.completeConstraintSystem(
            commonSystem.asConstraintSystemCompleterContext(),
            builtIns.unitType,
            partiallyResolvedCallsInfo.map { it.callResolutionResult.resultCallAtom },
            completionMode,
            diagnosticsHolder
        )

        if (isTopLevelBuilderInferenceCall()) {
            updateAllCalls(initialStorageSubstitutor)
        }

        return commonSystem.fixedTypeVariables.cast() // TODO: SUB
    }

    /*
     * We update calls in top-down way:
     * - updating calls within top-level builder inference call
     * - ...
     * - updating calls within the deepest builder inference call
     */
    private fun updateAllCalls(substitutor: NewTypeSubstitutor) {
        updateCalls(
            lambda,
            substitutor = ComposedSubstitutor(substitutor, commonSystem.buildCurrentSubstitutor() as NewTypeSubstitutor),
            commonSystem.errors
        )

        for (nestedSession in nestedBuilderInferenceSessions) {
            nestedSession.updateAllCalls(substitutor)
        }
    }

    override fun shouldCompleteResolvedSubAtomsOf(resolvedCallAtom: ResolvedCallAtom) = true

    private fun createNonFixedTypeToVariableMap(): Map<TypeConstructor, UnwrappedType> {
        val bindings = hashMapOf<TypeConstructor, UnwrappedType>()

        for ((variable, nonFixedType) in stubsForPostponedVariables) { // do it for nested sessions
            bindings[nonFixedType.constructor] = variable.defaultType
        }

        val parentBuilderInferenceCallSession = findParentBuildInferenceSession()

        if (parentBuilderInferenceCallSession != null) {
            bindings.putAll(parentBuilderInferenceCallSession.createNonFixedTypeToVariableMap())
        }

        return bindings
    }

    private fun createNonFixedTypeToVariableSubstitutor() = NewTypeSubstitutorByConstructorMap(createNonFixedTypeToVariableMap())

    private fun integrateConstraints(
        commonSystem: NewConstraintSystemImpl,
        storage: ConstraintStorage,
        nonFixedToVariablesSubstitutor: NewTypeSubstitutor,
        shouldIntegrateAllConstraints: Boolean
    ): Boolean {
        storage.notFixedTypeVariables.values.forEach { commonSystem.registerVariable(it.typeVariable) }

        for (parentSession in findAllParentBuildInferenceSessions()) {
            parentSession.stubsForPostponedVariables.keys.forEach { commonSystem.registerVariable(it) }
        }

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
            val lowerCallSubstituted = callSubstitutor.safeSubstitute(initialConstraint.a as UnwrappedType)
            val upperCallSubstituted = callSubstitutor.safeSubstitute(initialConstraint.b as UnwrappedType)

            val (lower, upper) = substituteNotFixedVariables(lowerCallSubstituted, upperCallSubstituted, nonFixedToVariablesSubstitutor)

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

    private fun substituteNotFixedVariables(
        lowerType: KotlinType,
        upperType: KotlinType,
        nonFixedToVariablesSubstitutor: NewTypeSubstitutor
    ): Pair<KotlinType, KotlinType> {
        val commonCapTypes = extractCommonCapturedTypes(lowerType, upperType)
        val substitutedCommonCapType = commonCapTypes.associate {
            it.constructor as TypeConstructor to nonFixedToVariablesSubstitutor.safeSubstitute(it).asTypeProjection()
        }

        val capTypesSubstitutor = TypeConstructorSubstitution.createByConstructorsMap(substitutedCommonCapType).buildSubstitutor()

        val substitutedLowerType = nonFixedToVariablesSubstitutor.safeSubstitute(capTypesSubstitutor.substitute(lowerType.unwrap()))
        val substitutedUpperType = nonFixedToVariablesSubstitutor.safeSubstitute(capTypesSubstitutor.substitute(upperType.unwrap()))

        return substitutedLowerType to substitutedUpperType
    }

    private fun extractCommonCapturedTypes(a: KotlinType, b: KotlinType): List<NewCapturedType> {
        val extractedCapturedTypes = mutableSetOf<NewCapturedType>().also { extractCapturedTypes(a, it) }
        return extractedCapturedTypes.filter { capturedType -> b.contains { it.constructor === capturedType.constructor } }
    }

    private fun extractCapturedTypes(type: KotlinType, capturedTypes: MutableSet<NewCapturedType>) {
        if (type is NewCapturedType) {
            capturedTypes.add(type)
        }
        for (typeArgument in type.arguments) {
            if (typeArgument.isStarProjection) continue
            extractCapturedTypes(typeArgument.type, capturedTypes)
        }
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
            completedCall.context.replaceBindingTrace(findTopLevelTrace()).replaceInferenceSession(this)
        )

        completeCall(completedCall, atomCompleter)
    }

    private fun findTopLevelTrace(): BindingTrace {
        var currentSession = this
        while (true) {
            currentSession = currentSession.findParentBuildInferenceSession() ?: break
        }
        return currentSession.topLevelCallContext.trace
    }

    private fun completeDoubleColonExpression(expression: KtDoubleColonExpression, substitutor: NewTypeSubstitutor) {
        val atomCompleter = createResolvedAtomCompleter(substitutor, topLevelCallContext)
        val declarationDescriptor = trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, expression)

        if (declarationDescriptor is SimpleFunctionDescriptorImpl) {
            atomCompleter.substituteFunctionLiteralDescriptor(resolvedAtom = null, descriptor = declarationDescriptor, substitutor)
        }

        val recordedType = trace.getType(expression)

        if (recordedType != null) {
            trace.recordType(expression, substitutor.safeSubstitute(recordedType.unwrap()))
        }

        val targetExpression = when (expression) {
            is KtCallableReferenceExpression -> expression.callableReference
            is KtClassLiteralExpression -> expression.receiverExpression
            else -> throw IllegalStateException("Unsupported double colon expression")
        }

        val call = trace.get(BindingContext.CALL, targetExpression) ?: return
        val resolvedCall = trace.get(BindingContext.RESOLVED_CALL, call)

        if (resolvedCall is ResolvedCallImpl<*>) {
            resolvedCall.setResultingSubstitutor(substitutor.toOldSubstitution().buildSubstitutor())
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

    companion object {
        private fun BuilderInferenceSession.updateCalls(
            lambda: ResolvedLambdaAtom,
            substitutor: NewTypeSubstitutor,
            errors: List<ConstraintSystemError>
        ) {
            val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

            val nonFixedTypesToResult = nonFixedToVariablesSubstitutor.map.mapValues { substitutor.safeSubstitute(it.value) }
            val nonFixedTypesToResultSubstitutor = ComposedSubstitutor(substitutor, nonFixedToVariablesSubstitutor)

            val atomCompleter = createResolvedAtomCompleter(
                nonFixedTypesToResultSubstitutor,
                topLevelCallContext.replaceBindingTrace(findTopLevelTrace()).replaceInferenceSession(this)
            )

            for (completedCall in commonCalls) {
                updateCall(completedCall, nonFixedTypesToResultSubstitutor, nonFixedTypesToResult)
                reportErrors(completedCall, completedCall.resolvedCall, errors)
            }

            for (callInfo in partiallyResolvedCallsInfo) {
                val resolvedCall = completeCall(callInfo, atomCompleter) ?: continue
                reportErrors(callInfo, resolvedCall, errors)
            }

            for (call in oldDoubleColonExpressionCalls) {
                when (call) {
                    is KtDoubleColonExpression -> completeDoubleColonExpression(call, nonFixedTypesToResultSubstitutor)
                    else -> throw Exception("Unsupported call expression type")
                }
            }

            atomCompleter.completeAll(lambda)
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
