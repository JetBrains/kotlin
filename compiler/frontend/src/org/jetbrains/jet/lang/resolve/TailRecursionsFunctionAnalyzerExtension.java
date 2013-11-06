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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.calls.TailRecursionKind;

import java.util.Collections;
import java.util.List;

import static com.google.common.base.Objects.firstNonNull;

public class TailRecursionsFunctionAnalyzerExtension implements FunctionAnalyzerExtension.AnalyzerExtension {

    public static final TailRecursionsFunctionAnalyzerExtension INSTANCE = new TailRecursionsFunctionAnalyzerExtension();

    @Override
    public void process(
            @NotNull FunctionDescriptor descriptor, @NotNull JetNamedFunction function, @NotNull final BindingTrace trace
    ) {
        List<JetCallExpression> callExpressions = firstNonNull(
                trace.get(BindingContext.FUNCTION_RECURSIVE_CALL_EXPRESSIONS, descriptor),
                Collections.<JetCallExpression>emptyList());

        boolean allNonTailRecursiveCalls = Iterables.all(callExpressions, new Predicate<JetCallExpression>() {
            @Override
            public boolean apply(JetCallExpression callExpression) {
                TailRecursionKind recursionKind = trace.get(BindingContext.TAIL_RECURSION_CALL, callExpression);
                return recursionKind == null || !recursionKind.isDoGenerateTailRecursion();
            }
        });

        if (allNonTailRecursiveCalls) {
            trace.report(Errors.NO_TAIL_RECURSIONS_FOUND.on(function));
        }
    }
}
