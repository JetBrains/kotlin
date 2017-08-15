/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.context.BasicCallResolutionContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.LambdaKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCallAtom
import org.jetbrains.kotlin.resolve.calls.model.SimpleKotlinCallArgument
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.TypeApproximator
import org.jetbrains.kotlin.types.TypeApproximatorConfiguration
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.utils.addIfNotNull

class KotlinResolutionCallbacksImpl(
        val topLevelCallContext: BasicCallResolutionContext,
        val expressionTypingServices: ExpressionTypingServices,
        val typeApproximator: TypeApproximator,
        val argumentTypeResolver: ArgumentTypeResolver,
        val languageVersionSettings: LanguageVersionSettings,
        val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer
): KotlinResolutionCallbacks {
    val trace: BindingTrace = topLevelCallContext.trace

    class LambdaInfo(val expectedType: UnwrappedType, val contextDependency: ContextDependency) {
        var dataFlowInfoAfter: DataFlowInfo? = null
        val returnStatements = ArrayList<Pair<KtReturnExpression, KotlinTypeInfo?>>()

        companion object {
            val STUB_EMPTY = LambdaInfo(TypeUtils.NO_EXPECTED_TYPE, ContextDependency.INDEPENDENT)
        }
    }

    override fun analyzeAndGetLambdaResultArguments(
            lambdaArgument: LambdaKotlinCallArgument,
            isSuspend: Boolean,
            receiverType: UnwrappedType?,
            parameters: List<UnwrappedType>,
            expectedReturnType: UnwrappedType?
    ): List<SimpleKotlinCallArgument> {
        val psiCallArgument = lambdaArgument.psiCallArgument as PSIFunctionKotlinCallArgument
        val outerCallContext = psiCallArgument.outerCallContext

        fun createCallArgument(ktExpression: KtExpression, typeInfo: KotlinTypeInfo) =
                createSimplePSICallArgument(trace.bindingContext, outerCallContext.statementFilter, outerCallContext.scope.ownerDescriptor,
                                            CallMaker.makeExternalValueArgument(ktExpression), DataFlowInfo.EMPTY, typeInfo)

        val lambdaInfo = LambdaInfo(expectedReturnType ?: TypeUtils.NO_EXPECTED_TYPE,
                                    if (expectedReturnType == null) ContextDependency.DEPENDENT else ContextDependency.INDEPENDENT)

        trace.record(BindingContext.NEW_INFERENCE_LAMBDA_INFO, psiCallArgument.ktFunction, lambdaInfo)

        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns
        val expectedType = createFunctionType(builtIns, Annotations.EMPTY, receiverType, parameters, null,
                           lambdaInfo.expectedType, isSuspend)

        val approximatesExpectedType = typeApproximator.approximateToSubType(expectedType, TypeApproximatorConfiguration.LocalDeclaration) ?: expectedType

        val actualContext = outerCallContext
                .replaceBindingTrace(trace)
                .replaceContextDependency(lambdaInfo.contextDependency)
                .replaceExpectedType(approximatesExpectedType)
                .replaceDataFlowInfo(psiCallArgument.lambdaInitialDataFlowInfo)

        val functionTypeInfo = expressionTypingServices.getTypeInfo(psiCallArgument.expression, actualContext)
        trace.record(BindingContext.NEW_INFERENCE_LAMBDA_INFO, psiCallArgument.ktFunction, LambdaInfo.STUB_EMPTY)

        var hasReturnWithoutExpression = false
        val returnArguments = lambdaInfo.returnStatements.mapNotNullTo(ArrayList()) { (expression, typeInfo) ->
            val returnedExpression = expression.returnedExpression
            if (returnedExpression != null) {
                createCallArgument(
                        returnedExpression,
                        typeInfo ?: throw AssertionError("typeInfo should be non-null for return with expression")
                )
            }
            else {
                hasReturnWithoutExpression = true
                EmptyLabeledReturn(expression, builtIns)
            }
        }

        if (!hasReturnWithoutExpression) {
            val lastExpressionArgument = getLastDeparentesizedExpression(psiCallArgument)?.let { lastExpression ->
                if (expectedReturnType?.isUnit() == true) return@let null // coercion to Unit

                // todo lastExpression can be if without else
                val lastExpressionType = trace.getType(lastExpression)
                val lastExpressionTypeInfo = KotlinTypeInfo(lastExpressionType, lambdaInfo.dataFlowInfoAfter ?: functionTypeInfo.dataFlowInfo)
                createCallArgument(lastExpression, lastExpressionTypeInfo)
            }
            returnArguments.addIfNotNull(lastExpressionArgument)
        }

        return returnArguments
    }

    private fun getLastDeparentesizedExpression(psiCallArgument: PSIKotlinCallArgument): KtExpression? {
        val lastExpression: KtExpression?
        if (psiCallArgument is LambdaKotlinCallArgumentImpl) {
            lastExpression = psiCallArgument.ktLambdaExpression.bodyExpression?.statements?.lastOrNull()
        }
        else {
            lastExpression = (psiCallArgument as FunctionExpressionImpl).ktFunction.bodyExpression?.lastBlockStatementOrThis()
        }

        return KtPsiUtil.deparenthesize(lastExpression)
    }

    override fun bindStubResolvedCallForCandidate(candidate: ResolvedCallAtom) {
        kotlinToResolvedCallTransformer.createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(candidate, trace)
    }

}