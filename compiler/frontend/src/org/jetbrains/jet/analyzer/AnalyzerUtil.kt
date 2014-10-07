/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.analyzer

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.di.InjectorForMacros
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.types.JetTypeInfo
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.DescriptorUtils

public fun JetExpression.computeTypeInfoInContext(
        scope: JetScope,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: JetType = TypeUtils.NO_EXPECTED_TYPE,
        module: ModuleDescriptor = scope.getModule()
): JetTypeInfo {
    val injectorForMacros = InjectorForMacros(getProject(), module)
    return injectorForMacros.getExpressionTypingServices()!!.getTypeInfo(scope, this, expectedType, dataFlowInfo, trace)
}

public fun JetExpression.analyzeInContext(
        scope: JetScope,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: JetType = TypeUtils.NO_EXPECTED_TYPE,
        module: ModuleDescriptor = scope.getModule()
): BindingContext {
    computeTypeInfoInContext(scope, trace, dataFlowInfo, expectedType, module)
    return trace.getBindingContext()
}

public fun JetExpression.computeTypeInContext(
        scope: JetScope,
        trace: BindingTrace = BindingTraceContext(),
        dataFlowInfo: DataFlowInfo = DataFlowInfo.EMPTY,
        expectedType: JetType = TypeUtils.NO_EXPECTED_TYPE,
        module: ModuleDescriptor = scope.getModule()
): JetType {
    return computeTypeInfoInContext(scope, trace, dataFlowInfo, expectedType, module).getType().safeType(this)
}

public fun JetType?.safeType(expression: JetExpression): JetType {
    if (this != null) return this

    return ErrorUtils.createErrorType("Type for " + expression.getText())
}

private fun JetScope.getModule(): ModuleDescriptor = DescriptorUtils.getContainingModule(this.getContainingDeclaration())
