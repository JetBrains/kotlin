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

package org.jetbrains.jet.lang.resolve.calls.tasks;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.LibrarySourceHacks.filterOutMembersFromLibrarySource;

@SuppressWarnings("unchecked")
public class CallableDescriptorCollectors<D extends CallableDescriptor> implements Iterable<CallableDescriptorCollector<D>> {
    private static final CallableDescriptorCollector<FunctionDescriptor> FUNCTIONS_COLLECTOR =
            new FilteredCollector<FunctionDescriptor>(new FunctionCollector());
    private static final CallableDescriptorCollector<VariableDescriptor> VARIABLES_COLLECTOR =
            new FilteredCollector<VariableDescriptor>(new VariableCollector());
    private static final CallableDescriptorCollector<VariableDescriptor> PROPERTIES_COLLECTOR =
            new FilteredCollector<VariableDescriptor>(new PropertyCollector());

    public static final CallableDescriptorCollectors<CallableDescriptor> FUNCTIONS_AND_VARIABLES =
            new CallableDescriptorCollectors(FUNCTIONS_COLLECTOR, VARIABLES_COLLECTOR);
    public static final CallableDescriptorCollectors<CallableDescriptor> FUNCTIONS =
            new CallableDescriptorCollectors(FUNCTIONS_COLLECTOR);
    public static final CallableDescriptorCollectors<VariableDescriptor> VARIABLES =
            new CallableDescriptorCollectors(VARIABLES_COLLECTOR);
    public static final CallableDescriptorCollectors<VariableDescriptor> PROPERTIES =
            new CallableDescriptorCollectors(PROPERTIES_COLLECTOR);

    private static class FunctionCollector implements CallableDescriptorCollector<FunctionDescriptor> {

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getNonExtensionsByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            Set<FunctionDescriptor> functions = Sets.newLinkedHashSet();
            for (FunctionDescriptor function : scope.getFunctions(name)) {
                if (function.getReceiverParameter() == null) {
                    functions.add(function);
                }
            }
            addConstructors(scope, name, functions);
            return functions;
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getMembersByName(@NotNull JetType receiverType, Name name, @NotNull BindingTrace bindingTrace) {
            JetScope receiverScope = receiverType.getMemberScope();
            Set<FunctionDescriptor> members = Sets.newHashSet(receiverScope.getFunctions(name));
            addConstructors(receiverScope, name, members);
            return members;
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getNonMembersByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            return scope.getFunctions(name);
        }

        private static void addConstructors(JetScope scope, Name name, Collection<FunctionDescriptor> functions) {
            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (!(classifier instanceof ClassDescriptor) || ErrorUtils.isError(classifier)) return;
            ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
            if (classDescriptor.getKind().isSingleton()) {
                // Constructors of singletons shouldn't be callable from the code
                return;
            }
            functions.addAll(classDescriptor.getConstructors());
        }

        @Override
        public String toString() {
            return "FUNCTIONS";
        }
    }

    private static class VariableCollector implements CallableDescriptorCollector<VariableDescriptor> {

        @NotNull
        @Override
        public Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            VariableDescriptor localVariable = scope.getLocalVariable(name);
            if (localVariable != null) {
                return Collections.singleton(localVariable);
            }

            LinkedHashSet<VariableDescriptor> variables = Sets.newLinkedHashSet();
            for (VariableDescriptor variable : scope.getProperties(name)) {
                if (variable.getReceiverParameter() == null) {
                    variables.add(variable);
                }
            }
            return variables;
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiverType, Name name, @NotNull BindingTrace bindingTrace) {
            return receiverType.getMemberScope().getProperties(name);
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getNonMembersByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            Collection<VariableDescriptor> result = Sets.newLinkedHashSet();

            VariableDescriptor localVariable = scope.getLocalVariable(name);
            if (localVariable != null) {
                result.add(localVariable);
            }
            result.addAll(scope.getProperties(name));
            return result;
        }

        @Override
        public String toString() {
            return "VARIABLES";
        }
    }

    private static class PropertyCollector implements CallableDescriptorCollector<VariableDescriptor> {
        private static Collection<VariableDescriptor> filterProperties(Collection<? extends VariableDescriptor> variableDescriptors) {
            ArrayList<VariableDescriptor> properties = Lists.newArrayList();
            for (VariableDescriptor descriptor : variableDescriptors) {
                if (descriptor instanceof PropertyDescriptor) {
                    properties.add(descriptor);
                }
            }
            return properties;
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            return filterProperties(VARIABLES_COLLECTOR.getNonExtensionsByName(scope, name, bindingTrace));
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiver, Name name, @NotNull BindingTrace bindingTrace) {
            return filterProperties(VARIABLES_COLLECTOR.getMembersByName(receiver, name, bindingTrace));
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getNonMembersByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            return filterProperties(VARIABLES_COLLECTOR.getNonMembersByName(scope, name, bindingTrace));
        }

        @Override
        public String toString() {
            return "PROPERTIES";
        }
    }
    
    private static class FilteredCollector<D extends CallableDescriptor> implements CallableDescriptorCollector<D> {
        private final CallableDescriptorCollector<D> delegate;

        private FilteredCollector(CallableDescriptorCollector<D> delegate) {
            this.delegate = delegate;
        }

        @NotNull
        @Override
        public Collection<D> getNonExtensionsByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            return filterOutMembersFromLibrarySource(delegate.getNonExtensionsByName(scope, name, bindingTrace));
        }

        @NotNull
        @Override
        public Collection<D> getMembersByName(@NotNull JetType receiver, Name name, @NotNull BindingTrace bindingTrace) {
            return filterOutMembersFromLibrarySource(delegate.getMembersByName(receiver, name, bindingTrace));
        }

        @NotNull
        @Override
        public Collection<D> getNonMembersByName(JetScope scope, Name name, @NotNull BindingTrace bindingTrace) {
            return filterOutMembersFromLibrarySource(delegate.getNonMembersByName(scope, name, bindingTrace));
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    private final Collection<CallableDescriptorCollector<D>> collectors;

    private CallableDescriptorCollectors(CallableDescriptorCollector<D>... collectors) {
        this.collectors = Lists.newArrayList(collectors);
    }

    @NotNull
    @Override
    public Iterator<CallableDescriptorCollector<D>> iterator() {
        return collectors.iterator();
    }
}
