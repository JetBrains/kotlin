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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind
import org.jetbrains.kotlin.types.TypeUtils

class ValueParameterResolver(
        private val expressionTypingServices: ExpressionTypingServices,
        private val constantExpressionEvaluator: ConstantExpressionEvaluator
) {

    fun resolveValueParameters(
            valueParameters: List<KtParameter>,
            valueParameterDescriptors: List<ValueParameterDescriptor>,
            declaringScope: LexicalScope,
            dataFlowInfo: DataFlowInfo,
            trace: BindingTrace
    ) {
        val scopeForDefaultValue = LexicalScopeImpl(declaringScope, declaringScope.ownerDescriptor, false, null, LexicalScopeKind.DEFAULT_VALUE)

        val contextForDefaultValue = ExpressionTypingContext.newContext(trace, scopeForDefaultValue, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE)

        for ((descriptor, parameter) in valueParameterDescriptors.zip(valueParameters)) {
            ForceResolveUtil.forceResolveAllContents(descriptor.annotations)
            resolveDefaultValue(descriptor, parameter, contextForDefaultValue)
        }
    }

    private fun resolveDefaultValue(
            valueParameterDescriptor: ValueParameterDescriptor,
            jetParameter: KtParameter,
            context: ExpressionTypingContext
    ) {
        if (!valueParameterDescriptor.declaresDefaultValue()) return
        val defaultValue = jetParameter.getDefaultValue() ?: return
        expressionTypingServices.getTypeInfo(defaultValue, context.replaceExpectedType(valueParameterDescriptor.type))
        if (DescriptorUtils.isAnnotationClass(DescriptorResolver.getContainingClass(context.scope))) {
            constantExpressionEvaluator.evaluateExpression(defaultValue, context.trace, valueParameterDescriptor.type)
            ?: context.trace.report(Errors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT.on(defaultValue))
        }
    }
}