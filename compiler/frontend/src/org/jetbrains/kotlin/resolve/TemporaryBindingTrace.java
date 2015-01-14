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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TemporaryBindingTrace extends DelegatingBindingTrace {

    @NotNull
    public static TemporaryBindingTrace create(@NotNull BindingTrace trace, String debugName) {
        return new TemporaryBindingTrace(trace, debugName);
    }

    @NotNull
    public static TemporaryBindingTrace create(@NotNull BindingTrace trace, String debugName, @Nullable Object resolutionSubjectForMessage) {
        return create(trace, AnalyzingUtils.formDebugNameForBindingTrace(debugName, resolutionSubjectForMessage));
    }

    protected final BindingTrace trace;

    protected TemporaryBindingTrace(@NotNull BindingTrace trace, String debugName) {
        super(trace.getBindingContext(), debugName);
        this.trace = trace;
    }

    public void commit() {
        addAllMyDataTo(trace);
        clear();
    }

    public void commit(@NotNull TraceEntryFilter filter, boolean commitDiagnostics) {
        addAllMyDataTo(trace, filter, commitDiagnostics);
        clear();
    }
}
