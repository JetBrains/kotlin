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

package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;

public class JetCompletionResultSet {
    private final ResolveSession resolveSession;
    private final BindingContext bindingContext;
    private final Condition<DeclarationDescriptor> descriptorFilter;
    private final CompletionResultSet result;
    private boolean isSomethingAdded;

    public JetCompletionResultSet(
            @NotNull CompletionResultSet result,
            @NotNull ResolveSession resolveSession,
            @NotNull BindingContext bindingContext) {
        this(result, resolveSession, bindingContext, Conditions.<DeclarationDescriptor>alwaysTrue());
    }

    public JetCompletionResultSet(
            @NotNull CompletionResultSet result,
            @NotNull ResolveSession resolveSession,
            @NotNull BindingContext bindingContext,
            @NotNull Condition<DeclarationDescriptor> descriptorFilter) {
        this.result = result;
        this.resolveSession = resolveSession;
        this.bindingContext = bindingContext;
        this.descriptorFilter = descriptorFilter;
    }

    public ResolveSession getResolveSession() {
        return resolveSession;
    }

    public BindingContext getBindingContext() {
        return bindingContext;
    }

    public CompletionResultSet getResult() {
        return result;
    }

    public void addAllElements(@NotNull Iterable<? extends DeclarationDescriptor> descriptors) {
        for (DeclarationDescriptor descriptor : descriptors) {
            addElement(descriptor);
        }
    }

    public void addElement(DeclarationDescriptor descriptor) {
        if (!descriptorFilter.value(descriptor)) {
            return;
        }

        addElement(DescriptorLookupConverter.createLookupElement(resolveSession, bindingContext, descriptor));
    }

    public void addElement(@NotNull LookupElement element) {
        if (!result.getPrefixMatcher().prefixMatches(element)) {
            return;
        }

        result.addElement(element);
        isSomethingAdded = true;
    }

    public boolean isSomethingAdded() {
        return isSomethingAdded;
    }

    @NotNull
    public Condition<String> getShortNameFilter() {
        return new Condition<String>() {
            @Override
            public boolean value(String s) {
                return result.getPrefixMatcher().prefixMatches(s);
            }
        };
    }
}
