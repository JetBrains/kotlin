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

import org.jetbrains.kotlin.builtins.createFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.ArgumentTypeResolver
import org.jetbrains.kotlin.resolve.calls.components.LambdaAnalyzer
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.model.*
import org.jetbrains.kotlin.resolve.calls.util.CallMaker
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo

class LambdaAnalyzerImpl(
        val expressionTypingServices: ExpressionTypingServices,
        val trace: BindingTrace,
        val typeApproximator: TypeApproximator,
        val kotlinToResolvedCallTransformer: KotlinToResolvedCallTransformer,
        val argumentTypeResolver: ArgumentTypeResolver
): LambdaAnalyzer {

    override fun analyzeAndGetLambdaResultArguments(
            topLevelCall: KotlinCall,
            lambdaArgument: LambdaKotlinCallArgument,
            receiverType: UnwrappedType?,
            parameters: List<UnwrappedType>,
            expectedReturnType: UnwrappedType?
    ): List<KotlinCallArgument> {
        val psiCallArgument = lambdaArgument.psiCallArgument
        val outerCallContext = (psiCallArgument as? LambdaKotlinCallArgumentImpl)?.outerCallContext ?:
                               (psiCallArgument as FunctionExpressionImpl).outerCallContext
        val expression: KtExpression = (psiCallArgument as? LambdaKotlinCallArgumentImpl)?.ktLambdaExpression ?:
                               (psiCallArgument as FunctionExpressionImpl).ktFunction

        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns
        val expectedType = createFunctionType(builtIns, Annotations.EMPTY, receiverType, parameters, null,
                           expectedReturnType ?: TypeUtils.NO_EXPECTED_TYPE)

        val approximatesExpectedType = typeApproximator.approximateToSubType(expectedType, TypeApproximatorConfiguration.LocalDeclaration) ?: expectedType

        val actualContext = outerCallContext.replaceBindingTrace(trace).
                replaceContextDependency(ContextDependency.DEPENDENT).replaceExpectedType(approximatesExpectedType)


        val functionTypeInfo = expressionTypingServices.getTypeInfo(expression, actualContext)
        val lastExpressionType = functionTypeInfo.type?.let {
            if (it.isFunctionType) it.getReturnTypeFromFunctionType() else it
        }
        val lastExpressionTypeInfo = KotlinTypeInfo(lastExpressionType, functionTypeInfo.dataFlowInfo)

        val lastExpression: KtExpression?
        if (psiCallArgument is LambdaKotlinCallArgumentImpl) {
            lastExpression = psiCallArgument.ktLambdaExpression.bodyExpression?.statements?.lastOrNull()
        }
        else {
            lastExpression = (psiCallArgument as FunctionExpressionImpl).ktFunction.bodyExpression?.lastBlockStatementOrThis()
        }

        val deparentesized = KtPsiUtil.deparenthesize(lastExpression) ?: return emptyList()

        val simpleArgument = createSimplePSICallArgument(actualContext, CallMaker.makeExternalValueArgument(deparentesized), lastExpressionTypeInfo)

        return listOfNotNull(simpleArgument)
    }

    override fun bindStubResolvedCallForCandidate(candidate: KotlinResolutionCandidate) {
        kotlinToResolvedCallTransformer.createStubResolvedCallAndWriteItToTrace<CallableDescriptor>(candidate, trace)
    }

    override fun completeLambdaReturnType(lambdaArgument: ResolvedLambdaArgument, returnType: KotlinType) {
        val psiCallArgument = lambdaArgument.argument.psiCallArgument
        val ktFunction = when (psiCallArgument) {
            is LambdaKotlinCallArgumentImpl -> psiCallArgument.ktLambdaExpression.functionLiteral
            is FunctionExpressionImpl -> psiCallArgument.ktFunction
            else -> throw AssertionError("Unexpected psiCallArgument for resolved lambda argument: $psiCallArgument")
        }

        val functionDescriptor = trace.bindingContext.get(BindingContext.FUNCTION, ktFunction) as? FunctionDescriptorImpl ?:
                                 throw AssertionError("No function descriptor for resolved lambda argument")
        functionDescriptor.setReturnType(returnType)

        for (lambdaResult in lambdaArgument.resultArguments) {
            val resultValueArgument = lambdaResult.psiCallArgument.valueArgument
            val deparenthesized = resultValueArgument.getArgumentExpression()?.let {
                KtPsiUtil.getLastElementDeparenthesized(it, expressionTypingServices.statementFilter)
            } ?: continue

            val recordedType = trace.getType(deparenthesized)
            if (recordedType != null && !recordedType.constructor.isDenotable) {
                argumentTypeResolver.updateResultArgumentTypeIfNotDenotable(trace, expressionTypingServices.statementFilter, returnType, deparenthesized)
            }
        }
    }
}