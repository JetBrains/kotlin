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
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;

import java.util.*;

public class CallableDescriptorCollectors {
    public static CallableDescriptorCollector<FunctionDescriptor> FUNCTIONS = new FunctionCollector();
    public static CallableDescriptorCollector<VariableDescriptor> VARIABLES = new VariableCollector();
    public static CallableDescriptorCollector<VariableDescriptor> PROPERTIES = new PropertyCollector();
    public static List<CallableDescriptorCollector<? extends CallableDescriptor>> FUNCTIONS_AND_VARIABLES =
            Lists.newArrayList(FUNCTIONS, VARIABLES);

    private static class FunctionCollector implements CallableDescriptorCollector<FunctionDescriptor> {

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getNonExtensionsByName(JetScope scope, Name name) {
            Set<FunctionDescriptor> functions = Sets.newLinkedHashSet(scope.getFunctions(name));
            for (Iterator<FunctionDescriptor> iterator = functions.iterator(); iterator.hasNext(); ) {
                FunctionDescriptor functionDescriptor = iterator.next();
                if (functionDescriptor.getReceiverParameter() != null) {
                    iterator.remove();
                }
            }
            addConstructors(scope, name, functions);
            return functions;
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getMembersByName(@NotNull JetType receiverType, Name name) {
            JetScope receiverScope = receiverType.getMemberScope();
            Set<FunctionDescriptor> members = Sets.newHashSet(receiverScope.getFunctions(name));
            addConstructors(receiverScope, name, members);
            return members;
        }

        @NotNull
        @Override
        public Collection<FunctionDescriptor> getNonMembersByName(JetScope scope, Name name) {
            return scope.getFunctions(name);
        }

        private static void addConstructors(JetScope scope, Name name, Collection<FunctionDescriptor> functions) {
            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (classifier instanceof ClassDescriptor && !ErrorUtils.isError(classifier.getTypeConstructor())) {
                ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
                functions.addAll(classDescriptor.getConstructors());
            }
        }

        @Override
        public String toString() {
            return "FUNCTIONS";
        }
    }

    private static class VariableCollector implements CallableDescriptorCollector<VariableDescriptor> {

        @NotNull
        @Override
        public Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, Name name) {
            VariableDescriptor descriptor = scope.getLocalVariable(name);
            if (descriptor == null) {
                descriptor = DescriptorUtils.filterNonExtensionProperty(scope.getProperties(name));
            }
            if (descriptor == null) return Collections.emptyList();
            return Collections.singleton(descriptor);
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiverType, Name name) {
            return receiverType.getMemberScope().getProperties(name);
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getNonMembersByName(JetScope scope, Name name) {
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
        public Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, Name name) {
            return filterProperties(VARIABLES.getNonExtensionsByName(scope, name));
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiver, Name name) {
            return filterProperties(VARIABLES.getMembersByName(receiver, name));
        }

        @NotNull
        @Override
        public Collection<VariableDescriptor> getNonMembersByName(JetScope scope, Name name) {
            return filterProperties(VARIABLES.getNonMembersByName(scope, name));
        }

        @Override
        public String toString() {
            return "PROPERTIES";
        }
    }

    private CallableDescriptorCollectors() {
    }
}
