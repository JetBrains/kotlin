package org.jetbrains.jet.lang.resolve.calls;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

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
                if (functionDescriptor.getReceiver().exists()) {
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
                if (!descriptor.getReceiver().exists()) {
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
            VariableDescriptor variable = scope.getVariable(name);
            if (variable != null && !variable.getReceiver().exists()) {
                JetType outType = variable.getOutType();
                if (outType != null && JetStandardClasses.isFunctionType(outType)) {
                    VariableAsFunctionDescriptor functionDescriptor = VariableAsFunctionDescriptor.create(variable);
                    if ((functionDescriptor.getReceiver().exists()) == receiverNeeded) {
                        functions.add(functionDescriptor);
                    }
                }
            }
        }
    };

    /*package*/ static TaskPrioritizer<VariableDescriptor> PROPERTY_TASK_PRIORITIZER = new TaskPrioritizer<VariableDescriptor>() {

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getNonExtensionsByName(JetScope scope, String name) {
            VariableDescriptor variable = scope.getVariable(name);
            if (variable != null && !variable.getReceiver().exists()) {
                return Collections.singleton(variable);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getMembersByName(@NotNull JetType receiverType, String name) {
            VariableDescriptor variable = receiverType.getMemberScope().getVariable(name);
            if (variable != null) {
                return Collections.singleton(variable);
            }
            return Collections.emptyList();
        }

        @NotNull
        @Override
        protected Collection<VariableDescriptor> getExtensionsByName(JetScope scope, String name) {
            VariableDescriptor variable = scope.getVariable(name);
            if (variable != null && variable.getReceiver().exists()) {
                return Collections.singleton(variable);
            }
            return Collections.emptyList();
        }
    };
}
