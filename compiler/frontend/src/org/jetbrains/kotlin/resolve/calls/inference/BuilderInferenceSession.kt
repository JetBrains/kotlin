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
import org.jetbrains.kotlin.resolve.calls.util.shouldBeSubstituteWithStubTypes
import org.jetbrains.kotlin.resolve.calls.util.toOldSubstitution
import org.jetbrains.kotlin.resolve.calls.components.*
import org.jetbrains.kotlin.resolve.calls.components.candidate.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.*
import org.jetbrains.kotlin.resolve.calls.inference.model.*
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.tower.*
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.hasBuilderInferenceAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.shouldBeSubstituteWithStubTypes
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.NewCapturedType
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.model.freshTypeConstructor
import org.jetbrains.kotlin.types.model.safeSubstitute
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.contains
import org.jetbrains.kotlin.types.typeUtil.shouldBeUpdated
import org.jetbrains.kotlin.utils.addToStdlib.cast

class BuilderInferenceSession(
    psiCallResolver: PSICallResolver,
    postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    callComponents: KotlinCallComponents,
    builtIns: KotlinBuiltIns,
    private val topLevelCallContext: BasicCallResolutionContext,
    private val stubsForPostponedVariables: Map<NewTypeVariable, StubTypeForBuilderInference>,
    private val trace: BindingTrace,
    private val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
    private val expressionTypingServices: ExpressionTypingServices,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
    private val typeApproximator: TypeApproximator,
    private val missingSupertypesResolver: MissingSupertypesResolver,
    private val lambdaArgument: LambdaKotlinCallArgument
) : StubTypesBasedInferenceSession<CallableDescriptor>(
    psiCallResolver, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter, callComponents, builtIns
) {
    private lateinit var lambda: ResolvedLambdaAtom
    private val commonSystem = NewConstraintSystemImpl(callComponents.constraintInjector, builtIns, callComponents.kotlinTypeRefiner)

    init {
        if (topLevelCallContext.inferenceSession is StubTypesBasedInferenceSession<*>) {
            topLevelCallContext.inferenceSession.addNestedInferenceSession(this)
        }
        stubsForPostponedVariables.keys.forEach(commonSystem::registerVariable)
    }

    private val commonCalls = arrayListOf<PSICompletedCallInfo>()

    private val localVariables = arrayListOf<KtVariableDeclaration>()

    // These calls come from the old type inference
    private val oldDoubleColonExpressionCalls = arrayListOf<KtExpression>()

    private var hasInapplicableCall = false

    override val parentSession = topLevelCallContext.inferenceSession

    override fun shouldRunCompletion(candidate: ResolutionCandidate): Boolean {
        val system = candidate.getSystem() as NewConstraintSystemImpl

        if (system.hasContradiction) return true

        val storage = system.getBuilder().currentStorage()
        fun ResolvedAtom.hasPostponed(): Boolean {
            if (this is PostponedResolvedAtom && !analyzed) return true
            return subResolvedAtoms?.any { it.hasPostponed() } == true
        }

        if (!candidate.resolvedCall.isSuitableForBuilderInference()) {
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

    private fun ResolvedCallAtom.isSuitableForBuilderInference(): Boolean {
        val extensionReceiver = extensionReceiverArgument
        val dispatchReceiver = dispatchReceiverArgument
        val resolvedAtoms = subResolvedAtoms

        return when {
            resolvedAtoms != null && resolvedAtoms.map { it.atom }.filterIsInstance<SubKotlinCallArgument>().any {
                it.callResult.resultCallAtom.isSuitableForBuilderInference()
            } -> true
            extensionReceiver == null && dispatchReceiver == null -> false
            dispatchReceiver?.receiver?.stableType?.containsStubType() == true -> true
            extensionReceiver?.receiver?.stableType?.containsStubType() == true -> candidateDescriptor.hasBuilderInferenceAnnotation()
            else -> false
        }
    }

    private fun KotlinType.containsStubType(): Boolean {
        return this.contains {
            it is StubTypeForBuilderInference
        }
    }

    fun addOldCallableReferenceCalls(callExpression: KtExpression) {
        oldDoubleColonExpressionCalls.add(callExpression)
    }

    override fun addCompletedCallInfo(callInfo: CompletedCallInfo) {
        require(callInfo is PSICompletedCallInfo) { "Wrong instance of callInfo: $callInfo" }

        if (skipCall(callInfo.callResolutionResult)) return

        commonCalls.add(callInfo)

        val resultingDescriptor = callInfo.resolvedCall.resultingDescriptor

        // This check is similar to one for old inference, see getCoroutineInferenceData() function
        val checkCall = resultingDescriptor is LocalVariableDescriptor || anyReceiverContainStubType(resultingDescriptor)

        if (!checkCall) return

        val isApplicableCall = callComponents.statelessCallbacks.isApplicableCallForBuilderInference(
            resultingDescriptor,
            callComponents.languageVersionSettings
        )

        if (!isApplicableCall) {
            hasInapplicableCall = true
        }
    }

    fun addLocalVariable(variable: KtVariableDeclaration) {
        localVariables.add(variable)
    }

    private fun anyReceiverContainStubType(descriptor: CallableDescriptor): Boolean {
        return descriptor.dispatchReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true ||
                descriptor.extensionReceiverParameter?.type?.contains { it is StubTypeForBuilderInference } == true
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

    override fun currentConstraintSystem() = ConstraintStorage.Empty

    fun getNotFixedToInferredTypesSubstitutor(): NewTypeSubstitutor =
        ComposedSubstitutor(getCurrentSubstitutor(), createNonFixedTypeToVariableSubstitutor())

    fun getUsedStubTypes(): Set<StubTypeForBuilderInference> = stubsForPostponedVariables.values.toSet()

    fun getCurrentSubstitutor(): NewTypeSubstitutor =
        commonSystem.buildCurrentSubstitutor().cast<NewTypeSubstitutor>().takeIf { !it.isEmpty } ?: EmptySubstitutor

    override fun initializeLambda(lambda: ResolvedLambdaAtom) {
        this.lambda = lambda
    }

    override fun inferPostponedVariables(
        lambda: ResolvedLambdaAtom,
        initialStorage: ConstraintStorage,
        completionMode: ConstraintSystemCompletionMode,
        diagnosticsHolder: KotlinDiagnosticsHolder,
    ): Map<TypeConstructor, UnwrappedType>? {
        initializeLambda(lambda)

        fun getResultingSubstitutor(): NewTypeSubstitutor {
            val storageSubstitutor = initialStorage.buildResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false)
            return ComposedSubstitutor(storageSubstitutor, commonSystem.buildCurrentSubstitutor() as NewTypeSubstitutor)
        }

        val effectivelyEmptyConstraintSystem = initializeCommonSystem(initialStorage)

        if (effectivelyEmptyConstraintSystem) {
            if (isTopLevelBuilderInferenceCall()) {
                updateAllCalls(getResultingSubstitutor())
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
            updateAllCalls(getResultingSubstitutor())
        }

        return commonSystem.fixedTypeVariables.cast() // TODO: SUB
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getNestedBuilderInferenceSessions() = buildList {
        for (nestedSession in nestedInferenceSessions) {
            when (nestedSession) {
                is BuilderInferenceSession -> add(nestedSession)
                is DelegateInferenceSession -> addAll(nestedSession.getNestedBuilderInferenceSessions())
            }
        }
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
            substitutor = substitutor,
            commonSystem.errors
        )

        val nestedBuilderInferenceSessions = getNestedBuilderInferenceSessions()

        for (nestedSession in nestedBuilderInferenceSessions) {
            // TODO: exclude injected variables
            nestedSession.updateAllCalls(
                ComposedSubstitutor(nestedSession.commonSystem.buildCurrentSubstitutor() as NewTypeSubstitutor, substitutor)
            )
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
        storage: ConstraintStorage,
        nonFixedToVariablesSubstitutor: NewTypeSubstitutor,
        shouldIntegrateAllConstraints: Boolean
    ) {
        storage.notFixedTypeVariables.values.forEach {
            if (it.typeVariable.freshTypeConstructor(commonSystem.typeSystemContext) !in commonSystem.allTypeVariables) {
                commonSystem.registerVariable(it.typeVariable)
            }
        }

        for (parentSession in findAllParentBuildInferenceSessions()) {
            for ((variable, stubType) in parentSession.stubsForPostponedVariables) {
                commonSystem.registerVariable(variable)
                commonSystem.addSubtypeConstraint(
                    variable.defaultType,
                    stubType,
                    InjectedAnotherStubTypeConstraintPositionImpl(lambdaArgument)
                )
            }
        }

        /*
        * storage can contain the following substitutions:
        *  TypeVariable(A) -> ProperType
        *  TypeVariable(B) -> Special-Non-Fixed-Type
        *
        * while substitutor from parameter map non-fixed types to the original type variable
        * */
        val callSubstitutor = storage.buildResultingSubstitutor(commonSystem, transformTypeVariablesToErrorTypes = false)

        for (initialConstraint in storage.initialConstraints) {
            if (initialConstraint.position is BuilderInferencePosition) continue

            val substitutedConstraint = initialConstraint.substitute(callSubstitutor)
            val (lower, upper) = substituteNotFixedVariables(
                substitutedConstraint.a as KotlinType,
                substitutedConstraint.b as KotlinType,
                nonFixedToVariablesSubstitutor
            )

            if (commonSystem.isProperType(lower) && commonSystem.isProperType(upper)) continue

            when (initialConstraint.constraintKind) {
                ConstraintKind.LOWER -> error("LOWER constraint shouldn't be used, please use UPPER")

                ConstraintKind.UPPER -> commonSystem.addSubtypeConstraint(lower, upper, substitutedConstraint.position)

                ConstraintKind.EQUALITY ->
                    with(commonSystem) {
                        addSubtypeConstraint(lower, upper, substitutedConstraint.position)
                        addSubtypeConstraint(upper, lower, substitutedConstraint.position)
                    }
            }
        }

        if (shouldIntegrateAllConstraints) {
            for ((variableConstructor, type) in storage.fixedTypeVariables) {
                val typeVariable = storage.allTypeVariables.getValue(variableConstructor)
                commonSystem.registerVariable(typeVariable)
                commonSystem.addEqualityConstraint((typeVariable as NewTypeVariable).defaultType, type, BuilderInferencePosition)
            }
        }
    }

    fun addExpectedTypeConstraint(
        callExpression: KtExpression,
        a: KotlinType,
        b: KotlinType
    ) {
        val nonFixedToVariablesSubstitutor: NewTypeSubstitutor = createNonFixedTypeToVariableSubstitutor()
        val (lower, upper) = substituteNotFixedVariables(a, b, nonFixedToVariablesSubstitutor)
        val position = BuilderInferenceExpectedTypeConstraintPosition(callExpression)
        val currentSubstitutor = commonSystem.buildCurrentSubstitutor()

        commonSystem.addSubtypeConstraint(
            currentSubstitutor.safeSubstitute(commonSystem.typeSystemContext, lower),
            currentSubstitutor.safeSubstitute(commonSystem.typeSystemContext, upper),
            position
        )
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
        val extractedCapturedTypes = mutableSetOf<NewCapturedType>().also { extractCapturedTypesTo(a, it) }
        return extractedCapturedTypes.filter { capturedType -> b.contains { it.constructor === capturedType.constructor } }
    }

    private fun extractCapturedTypesTo(type: KotlinType, to: MutableSet<NewCapturedType>) {
        if (type is NewCapturedType) {
            to.add(type)
        }
        for (typeArgument in type.arguments) {
            if (typeArgument.isStarProjection) continue
            extractCapturedTypesTo(typeArgument.type, to)
        }
    }

    private fun initializeCommonSystem(initialStorage: ConstraintStorage): Boolean {
        val nonFixedToVariablesSubstitutor = createNonFixedTypeToVariableSubstitutor()

        integrateConstraints(initialStorage, nonFixedToVariablesSubstitutor, false)

        for (call in commonCalls) {
            val storage = call.callResolutionResult.constraintSystem.getBuilder().currentStorage()
            integrateConstraints(storage, nonFixedToVariablesSubstitutor, false)
        }
        for (call in partiallyResolvedCallsInfo) {
            val storage = call.callResolutionResult.constraintSystem.getBuilder().currentStorage()
            integrateConstraints(storage, nonFixedToVariablesSubstitutor, true)
        }

        return commonSystem.notFixedTypeVariables.all { it.value.constraints.isEmpty() }
    }

    private fun reportErrors(completedCall: CallInfo, resolvedCall: NewAbstractResolvedCall<*>, errors: List<ConstraintSystemError>) {
        kotlinToResolvedCallTransformer.reportCallDiagnostic(
            completedCall.context,
            trace,
            resolvedCall,
            resolvedCall.resultingDescriptor,
            errors.asDiagnostics()
        )
    }

    private fun updateLocalVariable(localVariable: KtVariableDeclaration, substitutor: NewTypeSubstitutor) {
        val descriptor = trace[BindingContext.VARIABLE, localVariable] as? LocalVariableDescriptor
        if (descriptor != null && descriptor.type.shouldBeUpdated()) {
            descriptor.setOutType(substitutor.safeSubstitute(descriptor.type.unwrap()))
        }
    }

    private fun updateCall(
        completedCall: PSICompletedCallInfo,
        nonFixedTypesToResultSubstitutor: NewTypeSubstitutor,
        nonFixedTypesToResult: Map<TypeConstructor, UnwrappedType>
    ) {
        val storage = completedCall.callResolutionResult.constraintSystem.getBuilder().currentStorage()
        val resultingCallSubstitutor = storage.fixedTypeVariables.entries
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

    fun completeDoubleColonExpression(expression: KtDoubleColonExpression, substitutor: NewTypeSubstitutor) {
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
            val oldSubstitutor = substitutor.toOldSubstitution().buildSubstitutor()
            if (resolvedCall.resultingDescriptor.shouldBeSubstituteWithStubTypes()) {
                resolvedCall.setResultingSubstitutor(oldSubstitutor)
            }
            if (resolvedCall.shouldBeSubstituteWithStubTypes()) {
                resolvedCall.setResolvedCallSubstitutor(oldSubstitutor)
            }
        }
    }

    private fun completeCall(
        callInfo: CallInfo,
        atomCompleter: ResolvedAtomCompleter
    ): NewAbstractResolvedCall<*>? {
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

    private fun InitialConstraint.substitute(substitutor: NewTypeSubstitutor): InitialConstraint {
        val lowerSubstituted = substitutor.safeSubstitute(a as UnwrappedType)
        val upperSubstituted = substitutor.safeSubstitute(b as UnwrappedType)

        if (lowerSubstituted == a && upperSubstituted == b) return this

        return InitialConstraint(
            lowerSubstituted,
            upperSubstituted,
            constraintKind,
            BuilderInferenceSubstitutionConstraintPositionImpl(lambdaArgument, this)
        )
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

            for (localVariable in localVariables) {
                updateLocalVariable(localVariable, nonFixedTypesToResultSubstitutor)
            }

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

class BuilderInferenceExpectedTypeConstraintPosition(callElement: KtExpression) : ExpectedTypeConstraintPosition<KtExpression>(callElement)
