/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.ModifierCheckerCore
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.calls.*
import org.jetbrains.kotlin.resolve.calls.callResolverUtil.ResolveArgumentsMode
import org.jetbrains.kotlin.resolve.calls.callUtil.createLookupLocation
import org.jetbrains.kotlin.resolve.calls.callUtil.isSafeCall
import org.jetbrains.kotlin.resolve.calls.components.ArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.components.CallableReferenceResolver
import org.jetbrains.kotlin.resolve.calls.components.LambdaAnalyzer
import org.jetbrains.kotlin.resolve.calls.components.TypeArgumentsToParametersMapper
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector
import org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.results.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.tasks.DynamicCallableDescriptors
import org.jetbrains.kotlin.resolve.calls.tasks.ResolutionCandidate
import org.jetbrains.kotlin.resolve.calls.tasks.TracingStrategy
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.resolve.scopes.receivers.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.ControlStructureTypingUtils.ControlStructureDataFlowInfo
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.DoubleColonLHS
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult
import java.util.*

class PSICallResolver(
        private val typeResolver: TypeResolver,
        private val expressionTypingServices: ExpressionTypingServices,
        private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
        private val languageVersionSettings: LanguageVersionSettings,
        private val dynamicCallableDescriptors: DynamicCallableDescriptors,
        private val syntheticScopes: SyntheticScopes,
        private val argumentsToParametersMapper: ArgumentsToParametersMapper,
        val typeArgumentsToParametersMapper: TypeArgumentsToParametersMapper,
        val resultTypeResolver: ResultTypeResolver,
        val callableReferenceResolver: CallableReferenceResolver,
        val constraintInjector: ConstraintInjector,
        private val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
        private val kotlinCallResolver: KotlinCallResolver,
        private val typeApproximator: TypeApproximator
) {
    private val GIVEN_CANDIDATES_NAME = Name.special("<given candidates>")

    fun <D : CallableDescriptor> runResolutionAndInference(
            context: BasicCallResolutionContext,
            name: Name,
            resolutionKind: NewResolutionOldInference.ResolutionKind<D>,
            tracingStrategy: TracingStrategy
    ) : OverloadResolutionResults<D> {
        val kotlinCall = toKotlinCall(context, resolutionKind.kotlinCallKind, context.call, name, tracingStrategy)
        val scopeTower = ASTScopeTower(context)
        val lambdaAnalyzer = LambdaAnalyzerImpl(expressionTypingServices, context.trace, typeApproximator)

        val callContext = createCallContext(scopeTower, lambdaAnalyzer)
        val factoryProviderForInvoke = FactoryProviderForInvoke(context, callContext, kotlinCall)

        val result = kotlinCallResolver.resolveCall(callContext, kotlinCall, calculateExpectedType(context), factoryProviderForInvoke)
        if (result.isEmpty() && reportAdditionalDiagnosticIfNoCandidates(context, scopeTower, resolutionKind.kotlinCallKind, kotlinCall)) {
            return OverloadResolutionResultsImpl.nameNotFound()
        }

        return convertToOverloadResolutionResults(context, result, tracingStrategy)
    }

    // actually, `D` is at least FunctionDescriptor, but right now because of CallResolver it isn't possible change upper bound for `D`
    fun <D : CallableDescriptor> runResolutionAndInferenceForGivenCandidates(
            context: BasicCallResolutionContext,
            resolutionCandidates: Collection<ResolutionCandidate<D>>,
            tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        val dispatchReceiver = resolutionCandidates.firstNotNullResult { it.dispatchReceiver }

        val kotlinCall = toKotlinCall(context, KotlinCallKind.FUNCTION, context.call, GIVEN_CANDIDATES_NAME, tracingStrategy, dispatchReceiver)
        val scopeTower = ASTScopeTower(context)
        val lambdaAnalyzer = LambdaAnalyzerImpl(expressionTypingServices, context.trace, typeApproximator)
        val callContext = createCallContext(scopeTower, lambdaAnalyzer)

        val givenCandidates = resolutionCandidates.map {
            GivenCandidate(it.descriptor as FunctionDescriptor,
                           it.dispatchReceiver?.let { context.transformToReceiverWithSmartCastInfo(it) },
                           it.knownTypeParametersResultingSubstitutor)
        }

        val result = kotlinCallResolver.resolveGivenCandidates(callContext, kotlinCall, calculateExpectedType(context), givenCandidates)
        return convertToOverloadResolutionResults(context, result, tracingStrategy)

    }

    private fun calculateExpectedType(context: BasicCallResolutionContext): UnwrappedType? {
        val expectedType = context.expectedType.unwrap()

        return if (context.contextDependency == ContextDependency.DEPENDENT) {
            assert(expectedType == TypeUtils.NO_EXPECTED_TYPE)
            null
        }
        else {
            if (expectedType.isError) TypeUtils.NO_EXPECTED_TYPE else expectedType
        }
    }

    private fun createCallContext(scopeTower: ASTScopeTower, lambdaAnalyzer: LambdaAnalyzer) =
            KotlinCallContext(scopeTower, lambdaAnalyzer, argumentsToParametersMapper, typeArgumentsToParametersMapper, resultTypeResolver,
                              callableReferenceResolver, constraintInjector)

    private fun <D : CallableDescriptor> convertToOverloadResolutionResults(
            context: BasicCallResolutionContext,
            result: Collection<ResolvedKotlinCall>,
            tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
        val trace = context.trace
        when (result.size) {
            0 -> {
                tracingStrategy.unresolvedReference(trace)
                return OverloadResolutionResultsImpl.nameNotFound()
            }
            1 -> {
                val singleCandidate = result.single()
                val resolvedCall = kotlinToResolvedCallTransformer.transformAndReport<D>(singleCandidate, context, trace)
                return SingleOverloadResolutionResult(resolvedCall)
            }
            else -> {
                val resolvedCalls = result.map { kotlinToResolvedCallTransformer.transformAndReport<D>(it, context, trace = null) }
                if (result.areAllCompletedAndFailed()) {
                    tracingStrategy.noneApplicable(trace, resolvedCalls)
                    tracingStrategy.recordAmbiguity(trace, resolvedCalls)
                }
                else {
                    tracingStrategy.recordAmbiguity(trace, resolvedCalls)
                    if (resolvedCalls.first().status == ResolutionStatus.INCOMPLETE_TYPE_INFERENCE) {
                        tracingStrategy.cannotCompleteResolve(trace, resolvedCalls)
                    }
                    else {
                        tracingStrategy.ambiguity(trace, resolvedCalls)
                    }
                }
                return ManyCandidates(resolvedCalls)
            }
        }
    }

    private fun Collection<ResolvedKotlinCall>.areAllCompletedAndFailed() =
            all {
                it is ResolvedKotlinCall.CompletedResolvedKotlinCall &&
                !it.completedCall.resolutionStatus.resultingApplicability.isSuccess
            }

    // true if we found something
    private fun reportAdditionalDiagnosticIfNoCandidates(
            context: BasicCallResolutionContext,
            scopeTower: ImplicitScopeTower,
            kind: KotlinCallKind,
            kotlinCall: KotlinCall
    ): Boolean {
        val reference = context.call.calleeExpression as? KtReferenceExpression ?: return false

        val errorCandidates = when (kind) {
            KotlinCallKind.FUNCTION ->
                collectErrorCandidatesForFunction(scopeTower, kotlinCall.name, kotlinCall.explicitReceiver?.receiver)
            KotlinCallKind.VARIABLE ->
                collectErrorCandidatesForVariable(scopeTower, kotlinCall.name, kotlinCall.explicitReceiver?.receiver)
            else -> emptyList()
        }

        for (candidate in errorCandidates) {
            if (candidate is ErrorCandidate.Classifier) {
                context.trace.record(BindingContext.REFERENCE_TARGET, reference, candidate.descriptor)
                context.trace.report(Errors.RESOLUTION_TO_CLASSIFIER.on(reference, candidate.descriptor, candidate.kind, candidate.errorMessage))
                return true
            }
        }
        return false
    }


    private inner class ASTScopeTower(
            val context: BasicCallResolutionContext
    ): ImplicitScopeTower {
        // todo may be for invoke for case variable + invoke we should create separate dynamicScope(by newCall for invoke)
        override val dynamicScope: MemberScope = dynamicCallableDescriptors.createDynamicDescriptorScope(context.call, context.scope.ownerDescriptor)
        // same for location
        override val location: LookupLocation = context.call.createLookupLocation()

        override val syntheticScopes: SyntheticScopes get() = this@PSICallResolver.syntheticScopes
        override val isDebuggerContext: Boolean get() = context.isDebuggerContext
        override val lexicalScope: LexicalScope get() = context.scope
        private val cache = HashMap<ReceiverParameterDescriptor, ReceiverValueWithSmartCastInfo>()

        override fun getImplicitReceiver(scope: LexicalScope): ReceiverValueWithSmartCastInfo? {
            val implicitReceiver = scope.implicitReceiver ?: return null

            return cache.getOrPut(implicitReceiver) {
                context.transformToReceiverWithSmartCastInfo(implicitReceiver.value)
            }
        }
    }

    private inner class FactoryProviderForInvoke(
            val context: BasicCallResolutionContext,
            val callContext: KotlinCallContext,
            val kotlinCall: PSIKotlinCallImpl
    ) : CandidateFactoryProviderForInvoke<KotlinResolutionCandidate> {

        init {
            assert(kotlinCall.dispatchReceiverForInvokeExtension == null) { kotlinCall }
        }

        override fun transformCandidate(
                variable: KotlinResolutionCandidate,
                invoke: KotlinResolutionCandidate
        ): VariableAsFunctionKotlinResolutionCandidate {
            assert(variable is SimpleKotlinResolutionCandidate) {
                "VariableAsFunction variable is not allowed here: $variable"
            }
            assert(invoke is SimpleKotlinResolutionCandidate) {
                "VariableAsFunction candidate is not allowed here: $invoke"
            }

            return VariableAsFunctionKotlinResolutionCandidate(kotlinCall, variable as SimpleKotlinResolutionCandidate, invoke as SimpleKotlinResolutionCandidate)
        }

        override fun factoryForVariable(stripExplicitReceiver: Boolean): CandidateFactory<SimpleKotlinResolutionCandidate> {
            val explicitReceiver = if (stripExplicitReceiver) null else kotlinCall.explicitReceiver
            val variableCall = PSIKotlinCallForVariable(kotlinCall, explicitReceiver, kotlinCall.name)
            return SimpleCandidateFactory(callContext, variableCall)
        }

        override fun factoryForInvoke(variable: KotlinResolutionCandidate, useExplicitReceiver: Boolean):
                Pair<ReceiverValueWithSmartCastInfo, CandidateFactory<KotlinResolutionCandidate>>? {
            assert(variable is SimpleKotlinResolutionCandidate) {
                "VariableAsFunction variable is not allowed here: $variable"
            }
            if (isRecursiveVariableResolution(variable as SimpleKotlinResolutionCandidate)) return null

            assert(variable.isSuccessful) {
                "Variable call should be successful: $variable " +
                "Descriptor: ${variable.descriptorWithFreshTypes}"
            }
            val variableCallArgument = createReceiverCallArgument(variable)

            val explicitReceiver = kotlinCall.explicitReceiver
            val callForInvoke = if (useExplicitReceiver && explicitReceiver is SimpleKotlinCallArgument) {
                PSIKotlinCallForInvoke(kotlinCall, explicitReceiver, variableCallArgument)
            }
            else {
                PSIKotlinCallForInvoke(kotlinCall, variableCallArgument, null)
            }

            return variableCallArgument.receiver to SimpleCandidateFactory(callContext, callForInvoke)
        }

        // todo: create special check that there is no invoke on variable
        private fun isRecursiveVariableResolution(variable: SimpleKotlinResolutionCandidate): Boolean {
            val variableType = variable.candidateDescriptor.returnType
            return variableType is DeferredType && variableType.isComputing
        }

        // todo: review
        private fun createReceiverCallArgument(variable: SimpleKotlinResolutionCandidate): ExpressionKotlinCallArgument =
                ReceiverExpressionKotlinCallArgument(createReceiverValueWithSmartCastInfo(variable), isVariableReceiverForInvoke = true)

        // todo: decrease hacks count
        private fun createReceiverValueWithSmartCastInfo(variable: SimpleKotlinResolutionCandidate): ReceiverValueWithSmartCastInfo {
            val callForVariable = variable.kotlinCall as PSIKotlinCallForVariable
            val calleeExpression = callForVariable.baseCall.psiCall.calleeExpression as? KtReferenceExpression ?:
                                   error("Unexpected call : ${callForVariable.baseCall.psiCall}")

            val temporaryTrace = TemporaryBindingTrace.create(context.trace, "Context for resolve candidate")
            val type = variable.descriptorWithFreshTypes.returnType!!.unwrap()
            val variableReceiver = ExpressionReceiver.create(calleeExpression, type, temporaryTrace.bindingContext)

            temporaryTrace.record(BindingContext.REFERENCE_TARGET, calleeExpression, variable.descriptorWithFreshTypes)
            val dataFlowValue = DataFlowValueFactory.createDataFlowValue(variableReceiver, temporaryTrace.bindingContext, context.scope.ownerDescriptor)
            return ReceiverValueWithSmartCastInfo(variableReceiver, context.dataFlowInfo.getCollectedTypes(dataFlowValue), dataFlowValue.isStable)
        }
    }


    private fun toKotlinCall(
            context: BasicCallResolutionContext,
            kotlinCallKind: KotlinCallKind,
            oldCall: Call,
            name: Name,
            tracingStrategy: TracingStrategy,
            forcedExplicitReceiver: Receiver? = null
    ): PSIKotlinCallImpl {
        val resolvedExplicitReceiver = resolveExplicitReceiver(context, forcedExplicitReceiver?: oldCall.explicitReceiver, oldCall.isSafeCall())
        val resolvedTypeArguments = resolveTypeArguments(context, oldCall.typeArguments)

        // this is hack for special calls. Note that special call has only arguments in parenthesis.
        val givenDataFlowInfo: ControlStructureDataFlowInfo? = context.dataFlowInfoForArguments as? ControlStructureDataFlowInfo

        val argumentsInParenthesis = if (oldCall.callType != Call.CallType.ARRAY_SET_METHOD && oldCall.functionLiteralArguments.isEmpty()) {
            oldCall.valueArguments
        }
        else {
            oldCall.valueArguments.dropLast(1)
        }

        val (resolvedArgumentsInParenthesis, dataFlowInfoAfterArgumentsInParenthesis) = resolveArgumentsInParenthesis(
                context, context.dataFlowInfoForArguments.resultInfo, argumentsInParenthesis, givenDataFlowInfo)

        val externalLambdaArguments = oldCall.functionLiteralArguments
        val externalArgument = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) {
            assert(externalLambdaArguments.isEmpty()) {
                "Unexpected lambda parameters for call $oldCall"
            }
            oldCall.valueArguments.last()
        }
        else {
            if (externalLambdaArguments.size > 2) {
                externalLambdaArguments.drop(1).forEach {
                    context.trace.report(Errors.MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(it.getLambdaExpression()))
                }
            }

            externalLambdaArguments.firstOrNull()
        }

        val astExternalArgument = externalArgument?.let { resolveValueArgument(context, dataFlowInfoAfterArgumentsInParenthesis, it) }
        val resultDataFlowInfo = astExternalArgument?.dataFlowInfoAfterThisArgument ?: dataFlowInfoAfterArgumentsInParenthesis

        return PSIKotlinCallImpl(kotlinCallKind, oldCall, tracingStrategy, resolvedExplicitReceiver, name, resolvedTypeArguments, resolvedArgumentsInParenthesis,
                                 astExternalArgument, context.dataFlowInfo, resultDataFlowInfo)
    }

    private fun resolveExplicitReceiver(context: BasicCallResolutionContext, oldReceiver: Receiver?, isSafeCall: Boolean): ReceiverKotlinCallArgument? =
            when(oldReceiver) {
                null -> null
                is QualifierReceiver -> QualifierReceiverKotlinCallArgument(oldReceiver) // todo report warning if isSafeCall
                is ReceiverValue -> {
                    val detailedReceiver = context.transformToReceiverWithSmartCastInfo(oldReceiver)
                    ReceiverExpressionKotlinCallArgument(detailedReceiver, isSafeCall)
                }
                else -> error("Incorrect receiver: $oldReceiver")
            }

    private fun resolveType(context: BasicCallResolutionContext, typeReference: KtTypeReference?): UnwrappedType? {
        if (typeReference == null) return null

        val type = typeResolver.resolveType(context.scope, typeReference, context.trace, checkBounds = true)
        ForceResolveUtil.forceResolveAllContents(type)
        return type.unwrap()
    }

    private fun resolveTypeArguments(context: BasicCallResolutionContext, typeArguments: List<KtTypeProjection>): List<TypeArgument> =
            typeArguments.map { projection ->
                if (projection.projectionKind != KtProjectionKind.NONE) {
                    context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
                }
                ModifierCheckerCore.check(projection, context.trace, null, languageVersionSettings)

                resolveType(context, projection.typeReference)?.let { SimpleTypeArgumentImpl(projection.typeReference!!, it) }  ?: TypeArgumentPlaceholder
            }

    private fun resolveArgumentsInParenthesis(
            context: BasicCallResolutionContext,
            dataFlowInfoForArguments: DataFlowInfo,
            arguments: List<ValueArgument>,
            givenDataFlowInfo: ControlStructureDataFlowInfo?
    ): Pair<List<KotlinCallArgument>, DataFlowInfo> {
        if (givenDataFlowInfo != null) {
            val resolvedArguments = arguments.map {
                resolveValueArgument(context, givenDataFlowInfo.getInfo(it), it)
            }
            return resolvedArguments to givenDataFlowInfo.resultInfo
        }

        var dataFlowInfo = dataFlowInfoForArguments

        val resolvedArguments = arguments.map {
            val argument = resolveValueArgument(context, dataFlowInfo, it)
            dataFlowInfo = argument.dataFlowInfoAfterThisArgument
            argument
        }

        return resolvedArguments to dataFlowInfo
    }

    private fun resolveValueArgument(
            outerCallContext: BasicCallResolutionContext,
            startDataFlowInfo: DataFlowInfo,
            valueArgument: ValueArgument
    ): PSIKotlinCallArgument {
        val parseErrorArgument = ParseErrorKotlinCallArgument(valueArgument, startDataFlowInfo, outerCallContext.scope.ownerDescriptor.builtIns)
        val ktExpression = KtPsiUtil.deparenthesize(valueArgument.getArgumentExpression()) ?:
                           return parseErrorArgument

        val argumentName = valueArgument.getArgumentName()?.asName

        val lambdaArgument: PSIKotlinCallArgument? = when (ktExpression) {
            is KtLambdaExpression ->
                LambdaKotlinCallArgumentImpl(outerCallContext, valueArgument, startDataFlowInfo, ktExpression, argumentName,
                                             resolveParametersTypes(outerCallContext, ktExpression.functionLiteral))
            is KtNamedFunction -> {
                val receiverType = resolveType(outerCallContext, ktExpression.receiverTypeReference)
                val parametersTypes = resolveParametersTypes(outerCallContext, ktExpression) ?: emptyArray()
                val returnType = resolveType(outerCallContext, ktExpression.typeReference)
                FunctionExpressionImpl(outerCallContext, valueArgument, startDataFlowInfo, ktExpression, argumentName, receiverType, parametersTypes, returnType)
            }
            else -> null
        }
        if (lambdaArgument != null) {
            checkNoSpread(outerCallContext, valueArgument)
            return lambdaArgument
        }

        val context = outerCallContext.replaceContextDependency(ContextDependency.DEPENDENT)
                .replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE).replaceDataFlowInfo(startDataFlowInfo)

        if (ktExpression is KtCallableReferenceExpression) {
            checkNoSpread(outerCallContext, valueArgument)

            // todo analyze left expression and get constraint system
            val (lhsResult, rightResults) = doubleColonExpressionResolver.resolveCallableReference(
                    ktExpression, ExpressionTypingContext.newContext(context), ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS)

            val newDataFlowInfo = (lhsResult as? DoubleColonLHS.Expression)?.dataFlowInfo ?: startDataFlowInfo

            // todo ChosenCallableReferenceDescriptor
            val argument = CallableReferenceKotlinCallArgumentImpl(valueArgument, startDataFlowInfo, newDataFlowInfo,
                                                                   ktExpression, argumentName, (lhsResult as? DoubleColonLHS.Type)?.type?.unwrap(),
                                                                   ConstraintStorage.Empty) // todo

            return argument
        }

        // valueArgument.getArgumentExpression()!! instead of ktExpression is hack -- type info should be stored also for parenthesized expression
        val typeInfo = expressionTypingServices.getTypeInfo(valueArgument.getArgumentExpression()!!, context)
        return createSimplePSICallArgument(context, valueArgument, typeInfo) ?: parseErrorArgument
    }

    private fun checkNoSpread(context: BasicCallResolutionContext, valueArgument: ValueArgument) {
        valueArgument.getSpreadElement()?.let {
            context.trace.report(Errors.SPREAD_OF_LAMBDA_OR_CALLABLE_REFERENCE.on(it))
        }
    }

    private fun resolveParametersTypes(context: BasicCallResolutionContext, ktFunction: KtFunction): Array<UnwrappedType?>? {
        val parameterList = ktFunction.valueParameterList ?: return null

        return Array(parameterList.parameters.size) {
            parameterList.parameters[it]?.typeReference?.let { resolveType(context, it) }
        }
    }


}