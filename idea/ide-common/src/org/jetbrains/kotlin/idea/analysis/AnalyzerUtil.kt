/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.analysis

import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.resolve.frontendService
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.calls.context.ContextDependency
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor

@JvmOverloads fun KtExpression.computeTypeInfoInContext(
        scope: LexicalScope,
        contextExpression: KtExpression = this,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: KotlinType = TypeUtils.NO_EXPECTED_TYPE,
        isStatement: Boolean = false,
        contextDependency: ContextDependency = ContextDependency.INDEPENDENT
): KotlinTypeInfo {
    PreliminaryDeclarationVisitor.createForExpression(this, trace)
    return contextExpression.getResolutionFacade().frontendService<ExpressionTypingServices>()
            .getTypeInfo(scope, this, expectedType, dataFlowInfo, trace, isStatement, contextExpression, contextDependency)
}

@JvmOverloads fun KtExpression.analyzeInContext(
        scope: LexicalScope,
        contextExpression: KtExpression = this,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: KotlinType = TypeUtils.NO_EXPECTED_TYPE,
        isStatement: Boolean = false,
        contextDependency: ContextDependency = ContextDependency.INDEPENDENT
): BindingContext {
    computeTypeInfoInContext(scope, contextExpression, trace, dataFlowInfo, expectedType, isStatement, contextDependency)
    return trace.bindingContext
}

@JvmOverloads fun KtExpression.computeTypeInContext(
        scope: LexicalScope,
        contextExpression: KtExpression = this,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: KotlinType = TypeUtils.NO_EXPECTED_TYPE
): KotlinType? {
    return computeTypeInfoInContext(scope, contextExpression, trace, dataFlowInfo, expectedType).type
}
