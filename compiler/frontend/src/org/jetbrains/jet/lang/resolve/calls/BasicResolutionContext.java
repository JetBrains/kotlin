/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.psi.Call;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

/**
* @author abreslav
*/
public class BasicResolutionContext extends ResolutionContext {
    @NotNull
    public static BasicResolutionContext create(@NotNull BindingTrace trace, @NotNull JetScope scope, @NotNull Call call, @NotNull JetType expectedType, @NotNull DataFlowInfo dataFlowInfo) {
        return new BasicResolutionContext(trace, scope, call, expectedType, dataFlowInfo);
    }

    private BasicResolutionContext(BindingTrace trace, JetScope scope, Call call, JetType expectedType, DataFlowInfo dataFlowInfo) {
        super(trace, scope, call, expectedType, dataFlowInfo);
    }

    @NotNull
    public BasicResolutionContext replaceTrace(@NotNull BindingTrace trace) {
        return create(trace, scope, call, expectedType, dataFlowInfo);
    }
}
