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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Set;

public class JetScopeSelectorUtil {
    private JetScopeSelectorUtil() {
    }

    public interface ScopeByNameSelector<D extends DeclarationDescriptor> {
        @Nullable
        D get(@NotNull JetScope scope, @NotNull Name name);
    }

    public interface ScopeByNameMultiSelector<D extends DeclarationDescriptor> {
        @NotNull
        Collection<D> get(JetScope scope, Name name);
    }

    public interface ScopeDescriptorSelector<D extends DeclarationDescriptor> {
        @NotNull
        Collection<D> get(JetScope scope);
    }

    @NotNull
    public static <D extends DeclarationDescriptor> Collection<D> collect(Collection<JetScope> scopes, ScopeByNameMultiSelector<D> selector, Name name) {
        Set<D> descriptors = Sets.newHashSet();

        for (JetScope scope : scopes) {
            descriptors.addAll(selector.get(scope, name));
        }

        return descriptors;
    }

    @NotNull
    public static <D extends DeclarationDescriptor> Collection<D> collect(Collection<JetScope> scopes, ScopeDescriptorSelector<D> selector) {
        Set<D> descriptors = Sets.newHashSet();

        for (JetScope scope : scopes) {
            descriptors.addAll(selector.get(scope));
        }

        return descriptors;
    }

    public static final ScopeByNameSelector<ClassifierDescriptor> CLASSIFIER_DESCRIPTOR_SCOPE_SELECTOR =
            new ScopeByNameSelector<ClassifierDescriptor>() {
                @Nullable
                @Override
                public ClassifierDescriptor get(@NotNull JetScope scope, @NotNull Name name) {
                    return scope.getClassifier(name);
                }
            };

    public static final ScopeByNameSelector<PackageViewDescriptor> PACKAGE_SCOPE_SELECTOR =
            new ScopeByNameSelector<PackageViewDescriptor>() {
                @Nullable
                @Override
                public PackageViewDescriptor get(@NotNull JetScope scope, @NotNull Name name) {
                    return scope.getPackage(name);
                }
            };

    public static final ScopeByNameMultiSelector<FunctionDescriptor> NAMED_FUNCTION_SCOPE_SELECTOR =
            new ScopeByNameMultiSelector<FunctionDescriptor>() {
                @NotNull
                @Override
                public Collection<FunctionDescriptor> get(@NotNull JetScope scope, @NotNull Name name) {
                    return scope.getFunctions(name);
                }
            };

    public static final ScopeByNameMultiSelector<VariableDescriptor> NAMED_PROPERTIES_SCOPE_SELECTOR =
            new ScopeByNameMultiSelector<VariableDescriptor>() {
                @NotNull
                @Override
                public Collection<VariableDescriptor> get(@NotNull JetScope scope, @NotNull Name name) {
                    return scope.getProperties(name);
                }
            };

    public static final ScopeDescriptorSelector<DeclarationDescriptor> ALL_DESCRIPTORS_SCOPE_SELECTOR =
            new ScopeDescriptorSelector<DeclarationDescriptor>() {
                @NotNull
                @Override
                public Collection<DeclarationDescriptor> get(@NotNull JetScope scope) {
                    return scope.getAllDescriptors();
                }
            };
}
