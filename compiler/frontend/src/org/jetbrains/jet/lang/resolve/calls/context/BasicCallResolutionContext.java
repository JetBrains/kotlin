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

package org.jetbrains.jet.lang.resolve.calls.context;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

public class BasicCallResolutionContext extends CallResolutionContext<BasicCallResolutionContext> {
    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull Call call,
            @NotNull JetType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ResolveMode resolveMode,
            ExpressionPosition expressionPosition
    ) {
        return new BasicCallResolutionContext(trace, scope, call, expectedType, dataFlowInfo, resolveMode, expressionPosition);
    }
    @NotNull
    public static BasicCallResolutionContext create(@NotNull ResolutionContext context, @NotNull Call call, @NotNull ResolveMode resolveMode) {
        return create(context.trace, context.scope, call, context.expectedType, context.dataFlowInfo, resolveMode, context.expressionPosition);
    }

    private BasicCallResolutionContext(
            BindingTrace trace, JetScope scope, Call call, JetType expectedType,
            DataFlowInfo dataFlowInfo, ResolveMode resolveMode, ExpressionPosition expressionPosition
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, resolveMode, expressionPosition);
    }

    @Override
    protected BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            ExpressionPosition expressionPosition
    ) {
        return create(trace, scope, call, expectedType, dataFlowInfo, resolveMode, expressionPosition);
    }

    @Override
    protected BasicCallResolutionContext self() {
        return this;
    }
}
