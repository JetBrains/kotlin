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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.resolve.inline.InlineAnalyzerExtension;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FunctionAnalyzerExtension {

    public interface AnalyzerExtension {
        void process(@NotNull FunctionDescriptor descriptor, @NotNull KtNamedFunction function, @NotNull BindingTrace trace);
    }

    @NotNull private final BindingTrace trace;

    public FunctionAnalyzerExtension(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    public void process(@NotNull BodiesResolveContext bodiesResolveContext) {
        for (Map.Entry<KtNamedFunction, SimpleFunctionDescriptor> entry : bodiesResolveContext.getFunctions().entrySet()) {
            KtNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();

            for (AnalyzerExtension extension : getExtensions(functionDescriptor)) {
                extension.process(functionDescriptor, function, trace);
            }
        }
    }

    @NotNull
    private static List<AnalyzerExtension> getExtensions(@NotNull FunctionDescriptor functionDescriptor) {
        if (InlineUtil.isInline(functionDescriptor)) {
            return Collections.<AnalyzerExtension>singletonList(InlineAnalyzerExtension.INSTANCE);
        }
        return Collections.emptyList();
    }
}
