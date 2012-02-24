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

package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.*;

/**
 * @author abreslav
 */
public class TaskPrioritizers {


    /*package*/ static TaskPrioritizer<FunctionDescriptor> FUNCTION_TASK_PRIORITIZER = new TaskPrioritizer<FunctionDescriptor>() {

        @NotNull
        @Override
        protected Collection<FunctionDescriptor> getNonExtensionsByName(JetScope scope, String name) {
            Set<FunctionDescriptor> functions = Sets.newLinkedHashSet(scope.getFunctions(name));
            for (Iterator<FunctionDescriptor> iterator = functions.iterator(); iterator.hasNext(); ) {
                FunctionDescriptor functionDescriptor = iterator.next();
                if (functionDescriptor.getReceiverParameter().exists()) {
                    iterator.remove();
                }
            }
            addConstructors(scope, name, functions);

            addVariableAsFunction(scope, name, functions, false);
            return functions;
        }

        @NotNull
        @Override
        protected Collection<FunctionDescriptor> getMembersByName(@NotNull JetType receiverType, String name) {
            JetScope receiverScope = receiverType.getMemberScope();
            Set<FunctionDescriptor> members = Sets.newHashSet(receiverScope.getFunctions(name));
            addConstructors(receiverScope, name, members);
            addVariableAsFunction(receiverScope, name, members, false);
            return members;
        }

        @NotNull
        @Override
        protected Collection<FunctionDescriptor> getExtensionsByName(JetScope scope, String name) {
            Set<FunctionDescriptor> extensionFunctions = Sets.newHashSet(scope.getFunctions(name));
            for (Iterator<FunctionDescriptor> iterator = extensionFunctions.iterator(); iterator.hasNext(); ) {
                FunctionDescriptor descriptor = iterator.next();
                if (!descriptor.getReceiverParameter().exists()) {
                    iterator.remove();
                }
            }
            addVariableAsFunction(scope, name, extensionFunctions, true);
            return extensionFunctions;
        }

        private void addConstructors(JetScope scope, String name, Collection<FunctionDescriptor> functions) {
            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (classifier instanceof ClassDescriptor && !ErrorUtils.isError(classifier.getTypeConstructor())) {
                ClassDescriptor classDescriptor = (ClassDescriptor) classifier;
                functions.addAll(classDescriptor.getConstructors());
            }
        }

        private void addVariableAsFunction(JetScope scope, String name, Set<FunctionDescriptor> functions, boolean receiverNeeded) {
            VariableDescriptor variable = scope.getLocalVariable(name);
            if (variable == null) {
                variable = DescriptorUtils.filterNonExtensionProperty(scope.getProperties(name));
            }
            if (variable != null) {
                JetType outType = variable.getType();
                if (outType != null && JetStandardClasses.isFunctionType(outType)) {
                    VariableAsFunctionDescriptor functionDescriptor = VariableAsFunctionDescriptor.create(variable);
                    if ((functionDescriptor.getReceiverParameter().exists()) == receiverNeeded) {
                        functions.add(functionDescriptor);
                    }
                }
            }
        }
    };

    /*package*/ static TaskPrioritizer<VariableDescriptor> VARIABLE_TASK_PRIORITIZER = new TaskPrioritizer<VariableDescriptor>() {

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, String name) {
            VariableDescriptor descriptor = scope.getLocalVariable(name);
            if (descriptor == null) {
                descriptor = DescriptorUtils.filterNonExtensionProperty(scope.getProperties(name));
            }
            if (descriptor == null) return Collections.emptyList();
            return Collections.singleton(descriptor);
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiverType, String name) {
            return receiverType.getMemberScope().getProperties(name);
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getExtensionsByName(JetScope scope, String name) {
            return Collections2.filter(scope.getProperties(name), new Predicate<VariableDescriptor>() {
                @Override
                public boolean apply(@Nullable VariableDescriptor variableDescriptor) {
                    return (variableDescriptor != null) && variableDescriptor.getReceiverParameter().exists();
                }
            });
        }
    };
    
    /*package*/ static TaskPrioritizer<VariableDescriptor> PROPERTY_TASK_PRIORITIZER = new TaskPrioritizer<VariableDescriptor>() {
        private Collection<VariableDescriptor> filterProperties(Collection<? extends VariableDescriptor> variableDescriptors) {
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
        protected Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, String name) {
            return filterProperties(VARIABLE_TASK_PRIORITIZER.getNonExtensionsByName(scope, name));
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiver, String name) {
            return filterProperties(VARIABLE_TASK_PRIORITIZER.getMembersByName(receiver, name));
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getExtensionsByName(JetScope scope, String name) {
            return filterProperties(VARIABLE_TASK_PRIORITIZER.getExtensionsByName(scope, name));
        }
    };
}
