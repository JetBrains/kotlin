/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.tower

import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isPrimitiveTypeOrNullablePrimitiveType
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isUnderKotlinPackage
import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TemporaryBindingTrace
import org.jetbrains.kotlin.resolve.TypeResolver
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactory
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.typeUtil.isUnit
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
    val typeResolver: TypeResolver
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
        expectedReturnType: UnwrappedType?
    ): List<KotlinCallArgument> {
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
        val expectedType = createFunctionType(
            builtIns, Annotations.EMPTY, receiverType, parameters, null,
            lambdaInfo.expectedType, isSuspend
        )

        val approximatesExpectedType =
            typeApproximator.approximateToSubType(expectedType, TypeApproximatorConfiguration.LocalDeclaration) ?: expectedType

        val actualContext = outerCallContext
            .replaceBindingTrace(trace)
            .replaceContextDependency(lambdaInfo.contextDependency)
            .replaceExpectedType(approximatesExpectedType)
            .replaceDataFlowInfo(psiCallArgument.lambdaInitialDataFlowInfo)

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
            if (expectedReturnType?.isUnit() == true) return@let null // coercion to Unit

            // todo lastExpression can be if without else
            val lastExpressionType = if (hasReturnWithoutExpression) null else trace.getType(lastExpression)
            val contextInfo = lambdaInfo.lastExpressionInfo
            val lastExpressionTypeInfo = KotlinTypeInfo(lastExpressionType, contextInfo.dataFlowInfoAfter ?: functionTypeInfo.dataFlowInfo)
            createCallArgument(lastExpression, lastExpressionTypeInfo, contextInfo.lexicalScope, contextInfo.trace)
        }

        returnArguments.addIfNotNull(lastExpressionArgument)

        return returnArguments
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
        kotlinToResolvedCallTransformer.createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(candidate, trace, emptyList())
    }

    override fun createReceiverWithSmartCastInfo(resolvedAtom: ResolvedCallAtom): ReceiverValueWithSmartCastInfo? {
        val returnType = resolvedAtom.candidateDescriptor.returnType ?: return null
        val psiKotlinCall = resolvedAtom.atom.psiKotlinCall
        val expression = psiKotlinCall.psiCall.callElement.safeAs<KtExpression>() ?: return null

        return transformToReceiverWithSmartCastInfo(
            resolvedAtom.candidateDescriptor,
            trace.bindingContext,
            psiKotlinCall.resultDataFlowInfo,
            ExpressionReceiver.create(expression, returnType, trace.bindingContext),
            languageVersionSettings, dataFlowValueFactory
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
}
