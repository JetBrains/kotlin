package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.TypeSubstitutor;

import java.util.Collections;
import java.util.Set;

/**
 * @author abreslav
 */
public class LazySubstitutingFunctionGroup implements FunctionGroup {
    private final TypeSubstitutor substitutor;
    private final FunctionGroup functionGroup;
    private Set<FunctionDescriptor> functionDescriptors;

    public LazySubstitutingFunctionGroup(TypeSubstitutor substitutor, FunctionGroup functionGroup) {
        this.substitutor = substitutor;
        this.functionGroup = functionGroup;
    }

    @NotNull
    @Override
    public String getName() {
        return functionGroup.getName();
    }

    @Override
    public boolean isEmpty() {
        return functionGroup.isEmpty();
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctionDescriptors() {
        if (functionDescriptors == null) {
            if (substitutor.isEmpty()) {
                functionDescriptors = functionGroup.getFunctionDescriptors();
            }
            else {
                Set<FunctionDescriptor> functionDescriptorSet = Sets.newLinkedHashSet();
                for (FunctionDescriptor descriptor : functionGroup.getFunctionDescriptors()) {
                    FunctionDescriptor substitute = descriptor.substitute(substitutor);
                    if (substitute != null) {
                        functionDescriptorSet.add(substitute);
                    }
                }
                functionDescriptors = Collections.unmodifiableSet(functionDescriptorSet);
            }
        }
        return functionDescriptors;
    }
}
