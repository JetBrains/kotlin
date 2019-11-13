/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isUnderKotlinPackage
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.getBinaryWithTypeParent
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.components.PostponedArgumentsAnalyzer
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.inference.CoroutineInferenceSession
import org.jetbrains.kotlin.resolve.calls.inference.components.KotlinConstraintSystemCompleter
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.deprecation.DeprecationResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.DoubleColonExpressionResolver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNullable
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class LambdaContextInfo(
    var typeInfo: KotlinTypeInfo? = null,
    var dataFlowInfoAfter: DataFlowInfo? = null,
    var lexicalScope: LexicalScope? = null,
    var trace: BindingTrace? = null
)

class KotlinResolutionCallbacksImpl(
    val trace: BindingTrace,
    val expressionTypingServices: ExpressionTypingServices,
    val typeApproximator: TypeApproximator,
    val argumentTypeResolver: ArgumentTypeResolver,
    val languageVersionSettings: LanguageVersionSettings,
    val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
    val dataFlowValueFactory: DataFlowValueFactory,
    override val inferenceSession: InferenceSession,
    val constantExpressionEvaluator: ConstantExpressionEvaluator,
    val typeResolver: TypeResolver,
    val psiCallResolver: PSICallResolver,
    val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
    val kotlinConstraintSystemCompleter: KotlinConstraintSystemCompleter,
    val callComponents: KotlinCallComponents,
    val doubleColonExpressionResolver: DoubleColonExpressionResolver,
    val deprecationResolver: DeprecationResolver,
    val moduleDescriptor: ModuleDescriptor,
    val topLevelCallContext: BasicCallResolutionContext?,
    val missingSupertypesResolver: MissingSupertypesResolver
) : KotlinResolutionCallbacks {
    class LambdaInfo(val expectedType: UnwrappedType, val contextDependency: ContextDependency) {
        val returnStatements = ArrayList<Pair<KtReturnExpression, LambdaContextInfo?>>()
        val lastExpressionInfo = LambdaContextInfo()

        companion object {
            val STUB_EMPTY = LambdaInfo(TypeUtils.NO_EXPECTED_TYPE, ContextDependency.INDEPENDENT)
        }
    }

    override fun analyzeAndGetLambdaReturnArguments(
        lambdaArgument: LambdaKotlinCallArgument,
        isSuspend: Boolean,
        receiverType: UnwrappedType?,
        parameters: List<UnwrappedType>,
        expectedReturnType: UnwrappedType?,
        annotations: Annotations,
        stubsForPostponedVariables: Map<NewTypeVariable, StubType>
    ): Pair<List<KotlinCallArgument>, InferenceSession?> {
        val psiCallArgument = lambdaArgument.psiCallArgument as PSIFunctionKotlinCallArgument
        val outerCallContext = psiCallArgument.outerCallContext

        fun createCallArgument(
            ktExpression: KtExpression,
            typeInfo: KotlinTypeInfo,
            scope: LexicalScope?,
            newTrace: BindingTrace?
        ): PSIKotlinCallArgument? {
            var newContext = outerCallContext
            if (scope != null) newContext = newContext.replaceScope(scope)
            if (newTrace != null) newContext = newContext.replaceBindingTrace(newTrace)

            processFunctionalExpression(
                newContext, ktExpression, typeInfo.dataFlowInfo, CallMaker.makeExternalValueArgument(ktExpression),
                null, outerCallContext.scope.ownerDescriptor.builtIns, typeResolver
            )?.let {
                it.setResultDataFlowInfoIfRelevant(typeInfo.dataFlowInfo)
                return it
            }

            return createSimplePSICallArgument(
                trace.bindingContext, outerCallContext.statementFilter, outerCallContext.scope.ownerDescriptor,
                CallMaker.makeExternalValueArgument(ktExpression), DataFlowInfo.EMPTY, typeInfo, languageVersionSettings,
                dataFlowValueFactory
            )
        }

        val lambdaInfo = LambdaInfo(
            expectedReturnType ?: TypeUtils.NO_EXPECTED_TYPE,
            if (expectedReturnType == null) ContextDependency.DEPENDENT else ContextDependency.INDEPENDENT
        )

        trace.record(BindingContext.NEW_INFERENCE_LAMBDA_INFO, psiCallArgument.ktFunction, lambdaInfo)

        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns

        // We have to refine receiverType because resolve inside lambda needs proper scope from receiver,
        // and for implicit receivers there are no expression which type would've been refined in ExpTypingVisitor
        // Relevant test: multiplatformTypeRefinement/lambdas
        //
        // It doesn't happen in similar cases with other implicit receivers (e.g., with scope of extension receiver
        // inside extension function) because during resolution of types we correctly discriminate headers
        //
        // Also note that refining the whole type might be undesired because sometimes it contains NO_EXPECTED_TYPE
        // which throws exceptions on attempt to call equals
        val refinedReceiverType = receiverType?.let {
            @UseExperimental(TypeRefinement::class) callComponents.kotlinTypeChecker.kotlinTypeRefiner.refineType(it)
        }

        val expectedType = createFunctionType(
            builtIns, annotations, refinedReceiverType, parameters, null,
            lambdaInfo.expectedType, isSuspend
        )

        val approximatesExpectedType =
            typeApproximator.approximateToSubType(expectedType, TypeApproximatorConfiguration.LocalDeclaration) ?: expectedType

        val coroutineSession =
            if (stubsForPostponedVariables.isNotEmpty()) {
                require(topLevelCallContext != null) { "Top level call context should not be null to analyze coroutine-lambda" }

                CoroutineInferenceSession(
                    psiCallResolver, postponedArgumentsAnalyzer, kotlinConstraintSystemCompleter,
                    callComponents, builtIns, topLevelCallContext, stubsForPostponedVariables, trace,
                    kotlinToResolvedCallTransformer, expressionTypingServices, argumentTypeResolver,
                    doubleColonExpressionResolver, deprecationResolver, moduleDescriptor, typeApproximator,
                    missingSupertypesResolver
                )
            } else {
                null
            }

        val actualContext = outerCallContext
            .replaceBindingTrace(trace)
            .replaceContextDependency(lambdaInfo.contextDependency)
            .replaceExpectedType(approximatesExpectedType)
            .replaceDataFlowInfo(psiCallArgument.dataFlowInfoBeforeThisArgument).let {
                if (coroutineSession != null) it.replaceInferenceSession(coroutineSession) else it
            }

        val functionTypeInfo = expressionTypingServices.getTypeInfo(psiCallArgument.expression, actualContext)
        trace.record(BindingContext.NEW_INFERENCE_LAMBDA_INFO, psiCallArgument.ktFunction, LambdaInfo.STUB_EMPTY)

        var hasReturnWithoutExpression = false
        val returnArguments = lambdaInfo.returnStatements.mapNotNullTo(ArrayList()) { (expression, contextInfo) ->
            val returnedExpression = expression.returnedExpression
            if (returnedExpression != null) {
                createCallArgument(
                    returnedExpression,
                    contextInfo?.typeInfo ?: throw AssertionError("typeInfo should be non-null for return with expression"),
                    contextInfo.lexicalScope,
                    contextInfo.trace
                )
            } else {
                hasReturnWithoutExpression = true
                EmptyLabeledReturn(expression, builtIns)
            }
        }

        val lastExpressionArgument = getLastDeparentesizedExpression(psiCallArgument)?.let { lastExpression ->
            if (expectedReturnType?.isUnit() == true || hasReturnWithoutExpression) return@let null // coercion to Unit

            // todo lastExpression can be if without else
            val lastExpressionType = trace.getType(lastExpression)
            val contextInfo = lambdaInfo.lastExpressionInfo
            val lastExpressionTypeInfo = KotlinTypeInfo(lastExpressionType, contextInfo.dataFlowInfoAfter ?: functionTypeInfo.dataFlowInfo)
            createCallArgument(lastExpression, lastExpressionTypeInfo, contextInfo.lexicalScope, contextInfo.trace)
        }

        returnArguments.addIfNotNull(lastExpressionArgument)

        return Pair(returnArguments, coroutineSession)
    }

    private fun getLastDeparentesizedExpression(psiCallArgument: PSIKotlinCallArgument): KtExpression? {
        val lastExpression: KtExpression?
        if (psiCallArgument is LambdaKotlinCallArgumentImpl) {
            lastExpression = psiCallArgument.ktLambdaExpression.bodyExpression?.statements?.lastOrNull()
        } else {
            lastExpression = (psiCallArgument as FunctionExpressionImpl).ktFunction.bodyExpression?.lastBlockStatementOrThis()
        }

        return KtPsiUtil.deparenthesize(lastExpression)
    }

    override fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom) {
        kotlinToResolvedCallTransformer.createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(
            candidate, trace, emptyList(), substitutor = null
        )
    }

    override fun isCompileTimeConstant(resolvedAtom: ResolvedCallAtom, expectedType: UnwrappedType): Boolean {
        val descriptor = resolvedAtom.candidateDescriptor

        if (!isUnderKotlinPackage(descriptor)) return false

        val returnType = descriptor.returnType ?: return false
        if (!isPrimitiveTypeOrNullablePrimitiveType(returnType) || !isPrimitiveTypeOrNullablePrimitiveType(expectedType)) return false

        val callElement = resolvedAtom.atom.psiKotlinCall.psiCall.callElement.safeAs<KtExpression>() ?: return false
        val expression = findCommonParent(callElement, resolvedAtom.atom.psiKotlinCall.explicitReceiver)

        val temporaryBindingTrace = TemporaryBindingTrace.create(
            trace,
            "Trace to check if some expression is constant, we have to avoid writing probably wrong COMPILE_TIME_VALUE slice"
        )
        return constantExpressionEvaluator.evaluateExpression(expression, temporaryBindingTrace, expectedType) != null
    }

    private fun findCommonParent(callElement: KtExpression, receiver: ReceiverKotlinCallArgument?): KtExpression {
        if (receiver == null) return callElement
        return PsiTreeUtil.findCommonParent(callElement, receiver.psiExpression)?.safeAs() ?: callElement
    }

    override fun getExpectedTypeFromAsExpressionAndRecordItInTrace(resolvedAtom: ResolvedCallAtom): UnwrappedType? {
        val candidateDescriptor = resolvedAtom.candidateDescriptor as? FunctionDescriptor ?: return null
        val call = resolvedAtom.atom.safeAs<PSIKotlinCall>()?.psiCall ?: return null

        if (call.typeArgumentList != null || !candidateDescriptor.isFunctionForExpectTypeFromCastFeature()) return null
        val binaryParent = call.calleeExpression?.getBinaryWithTypeParent() ?: return null
        val operationType = binaryParent.operationReference.getReferencedNameElementType().takeIf {
            it == KtTokens.AS_KEYWORD || it == KtTokens.AS_SAFE
        } ?: return null

        val leftType = trace.get(BindingContext.TYPE, binaryParent.right ?: return null) ?: return null
        val expectedType = if (operationType == KtTokens.AS_SAFE) leftType.makeNullable() else leftType
        val resultType = expectedType.unwrap()
        trace.record(BindingContext.CAST_TYPE_USED_AS_EXPECTED_TYPE, binaryParent)
        return resultType
    }

    override fun disableContractsIfNecessary(resolvedAtom: ResolvedCallAtom) {
        val atom = resolvedAtom.atom as? PSIKotlinCall ?: return
        val context = topLevelCallContext ?: return
        disableContractsInsideContractsBlock(atom.psiCall, resolvedAtom.candidateDescriptor, context.scope, trace)
    }
}
