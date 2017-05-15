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
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.psi.KtCallableDeclaration;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.resolve.inline.InlineAnalyzerExtension;
import org.jetbrains.kotlin.resolve.inline.InlineUtil;
import org.jetbrains.kotlin.resolve.inline.ReasonableInlineRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AnalyzerExtensions {

    public interface AnalyzerExtension {
        void process(@NotNull CallableMemberDescriptor descriptor, @NotNull KtCallableDeclaration functionOrProperty, @NotNull BindingTrace trace);
    }

    @NotNull private final BindingTrace trace;
    @NotNull private final Iterable<ReasonableInlineRule> reasonableInlineRules;
    private LanguageVersionSettings languageVersionSettings;

    public AnalyzerExtensions(
            @NotNull BindingTrace trace,
            @NotNull Iterable<ReasonableInlineRule> reasonableInlineRules,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        this.trace = trace;
        this.reasonableInlineRules = reasonableInlineRules;
        this.languageVersionSettings = languageVersionSettings;
    }

    public void process(@NotNull BodiesResolveContext bodiesResolveContext) {
        for (Map.Entry<KtNamedFunction, SimpleFunctionDescriptor> entry : bodiesResolveContext.getFunctions().entrySet()) {
            KtNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();

            for (AnalyzerExtension extension : getFunctionExtensions(functionDescriptor)) {
                extension.process(functionDescriptor, function, trace);
            }
        }

        for (Map.Entry<KtProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet()) {
            KtProperty function = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();

            for (AnalyzerExtension extension : getPropertyExtensions(propertyDescriptor)) {
                extension.process(propertyDescriptor, function, trace);
            }
        }
    }

    @NotNull
    private List<InlineAnalyzerExtension> getFunctionExtensions(@NotNull FunctionDescriptor functionDescriptor) {
        if (InlineUtil.isInline(functionDescriptor)) {
            return Collections.singletonList(new InlineAnalyzerExtension(reasonableInlineRules, languageVersionSettings));
        }
        return Collections.emptyList();
    }

    @NotNull
    private List<InlineAnalyzerExtension> getPropertyExtensions(@NotNull PropertyDescriptor propertyDescriptor) {
        if (InlineUtil.hasInlineAccessors(propertyDescriptor)) {
            return Collections.singletonList(new InlineAnalyzerExtension(reasonableInlineRules, languageVersionSettings));
        }
        return Collections.emptyList();
    }
}
