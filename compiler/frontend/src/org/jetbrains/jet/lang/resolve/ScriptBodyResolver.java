/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.ExpressionPosition;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.CoercionStrategy;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;

import javax.inject.Inject;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class ScriptBodyResolver {

    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private BindingTrace trace;

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }



    public void resolveScriptBodies() {
        for (Map.Entry<JetScript, ScriptDescriptor> e : context.getScripts().entrySet()) {
            JetScript declaration = e.getKey();
            ScriptDescriptor descriptor = e.getValue();
            WritableScope scope = context.getScriptScopes().get(declaration);

            // TODO: lock in resolveScriptDeclarations
            scope.changeLockLevel(WritableScope.LockLevel.READING);

            ExpressionTypingContext context = ExpressionTypingContext.newContext(
                    expressionTypingServices,
                    trace,
                    scope,
                    DataFlowInfo.EMPTY,
                    NO_EXPECTED_TYPE,
                    ExpressionPosition.FREE);
            JetType returnType = expressionTypingServices.getBlockReturnedType(scope, declaration.getBlockExpression(), CoercionStrategy.NO_COERCION, context, trace).getType();
            if (returnType == null) {
                returnType = ErrorUtils.createErrorType("getBlockReturnedType returned null");
            }
            descriptor.initialize(returnType, declaration, trace.getBindingContext());
        }
    }

}
