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

package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

/**
 * @author abreslav
 */
public class TemporaryBindingTrace extends DelegatingBindingTrace {

    public static TemporaryBindingTrace create(@NotNull BindingTrace trace) {
        return new TemporaryBindingTrace(trace);
    }

    protected final BindingTrace trace;

    protected TemporaryBindingTrace(@NotNull BindingTrace trace) {
        super(trace.getBindingContext());
        this.trace = trace;
    }

    public void commit() {
        addAllMyDataTo(trace);
        clear();
    }

    public void commit(@NotNull Predicate<WritableSlice> filter, boolean commitDiagnostics) {
        addAllMyDataTo(trace, filter, commitDiagnostics);
        clear();
    }
}
