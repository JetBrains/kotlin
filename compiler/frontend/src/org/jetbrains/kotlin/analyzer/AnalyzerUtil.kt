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

package org.jetbrains.kotlin.analyzer

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.frontend.di.createContainerForMacros
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.expressions.JetTypeInfo

public fun JetExpression.computeTypeInfoInContext(
        scope: JetScope,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: JetType = TypeUtils.NO_EXPECTED_TYPE,
        module: ModuleDescriptor = scope.getModule(),
        isStatement: Boolean = false
): JetTypeInfo {
    val expressionTypingServices = createContainerForMacros(getProject(), module).expressionTypingServices
    return expressionTypingServices.getTypeInfo(scope, this, expectedType, dataFlowInfo, trace, isStatement)
}

public fun JetExpression.analyzeInContext(
        scope: JetScope,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: JetType = TypeUtils.NO_EXPECTED_TYPE,
        module: ModuleDescriptor = scope.getModule(),
        isStatement: Boolean = false
): BindingContext {
    computeTypeInfoInContext(scope, trace, dataFlowInfo, expectedType, module, isStatement)
    return trace.getBindingContext()
}

public fun JetType?.safeType(expression: JetExpression): JetType {
    if (this != null) return this

    return ErrorUtils.createErrorType("Type for " + expression.getText())
}

private fun JetScope.getModule(): ModuleDescriptor = this.getContainingDeclaration().module
