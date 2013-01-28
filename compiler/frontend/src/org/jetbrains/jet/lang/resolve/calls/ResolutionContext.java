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

package org.jetbrains.jet.lang.resolve.calls;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;

public abstract class ResolutionContext<Context extends ResolutionContext> {
    public final BindingTrace trace;
    public final JetScope scope;
    public final JetType expectedType;
    public final DataFlowInfo dataFlowInfo;
    // true for positions on the lhs of a '.', i.e. allows namespace results and 'super'
    public final boolean namespacesAllowed;

    protected ResolutionContext(
            BindingTrace trace,
            JetScope scope,
            JetType expectedType,
            DataFlowInfo dataFlowInfo,
            boolean namespacesAllowed
    ) {
        this.trace = trace;
        this.scope = scope;
        this.expectedType = expectedType;
        this.dataFlowInfo = dataFlowInfo;
        this.namespacesAllowed = namespacesAllowed;
    }

    protected abstract Context replace(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            boolean namespacesAllowed
    );
    
    protected abstract Context self();
    
    public Context replaceBindingTrace(@NotNull BindingTrace trace) {
        if (this.trace == trace) return self();
        return replace(trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }
    
    @NotNull
    public Context replaceNamespacesAllowed(boolean namespacesAllowed) {
        if (namespacesAllowed == this.namespacesAllowed) return self();
        return replace(trace, scope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public Context replaceDataFlowInfo(DataFlowInfo newDataFlowInfo) {
        if (newDataFlowInfo == dataFlowInfo) return self();
        return replace(trace, scope, newDataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public Context replaceExpectedType(@Nullable JetType newExpectedType) {
        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return self();
        return replace(trace, scope, dataFlowInfo, newExpectedType, namespacesAllowed);
    }

    @NotNull
    public Context replaceScope(@NotNull JetScope newScope) {
        if (newScope == scope) return self();
        return replace(trace, newScope, dataFlowInfo, expectedType, namespacesAllowed);
    }

    @NotNull
    public BasicCallResolutionContext toCallResolutionContext(@NotNull Call call) {
        return BasicCallResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo, namespacesAllowed);
    }
}