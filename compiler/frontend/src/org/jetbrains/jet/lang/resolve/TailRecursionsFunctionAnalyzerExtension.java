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
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetNamedFunction;

public class TailRecursionsFunctionAnalyzerExtension implements FunctionAnalyzerExtension.AnalyzerExtension {

    public static final TailRecursionsFunctionAnalyzerExtension INSTANCE = new TailRecursionsFunctionAnalyzerExtension();

    @Override
    public void process(
            @NotNull FunctionDescriptor descriptor, @NotNull JetNamedFunction function, @NotNull BindingTrace trace
    ) {
        Boolean hasTailCalls = trace.get(BindingContext.HAS_TAIL_CALLS, descriptor);

        if (Boolean.FALSE.equals(hasTailCalls)) {
            trace.report(Errors.NO_TAIL_CALLS_FOUND.on(function));
        }
    }
}
