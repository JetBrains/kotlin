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

import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

public abstract class CallResolutionContext<Context extends CallResolutionContext> extends ResolutionContext<Context> {
    public final Call call;
    public final ResolveMode resolveMode;

    protected CallResolutionContext(
            BindingTrace trace,
            JetScope scope,
            Call call,
            JetType expectedType,
            DataFlowInfo dataFlowInfo,
            ResolveMode resolveMode,
            ExpressionPosition expressionPosition
    ) {
        super(trace, scope, expectedType, dataFlowInfo, expressionPosition);
        this.call = call;
        this.resolveMode = resolveMode;
    }

    public BasicCallResolutionContext toBasic() {
        return BasicCallResolutionContext.create(trace, scope, call, expectedType, dataFlowInfo, resolveMode, expressionPosition);
    }
}
