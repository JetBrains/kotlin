package org.jetbrains.jet.lang.descriptors;

import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * @author abreslav
 */
public class WritableFunctionGroup implements FunctionGroup {
    private final String name;
    private Set<FunctionDescriptor> functionDescriptors;
    private Set<FunctionDescriptor> unmodifiableFunctionDescriptors;

    public WritableFunctionGroup(String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "FunctionGroup{" +
                "name='" + name + '\'' +
                '}';
    }

    public void addFunction(@NotNull FunctionDescriptor functionDescriptor) {
        getWritableFunctionDescriptors().add(functionDescriptor);
    }

    @Override
    @NotNull
    public Set<FunctionDescriptor> getFunctionDescriptors() {
        if (unmodifiableFunctionDescriptors == null) {
            unmodifiableFunctionDescriptors = Collections.unmodifiableSet(getWritableFunctionDescriptors());
        }
        return unmodifiableFunctionDescriptors;
    }

    private Set<FunctionDescriptor> getWritableFunctionDescriptors() {
        if (functionDescriptors == null) {
            functionDescriptors = Sets.newLinkedHashSet();
        }
        return functionDescriptors;
    }

    @Override
    public boolean isEmpty() {
        return functionDescriptors == null || functionDescriptors.isEmpty();
    }

    public void addAllFunctions(@NotNull FunctionGroup functionGroup) {
        if (functionGroup.isEmpty()) return;
        getWritableFunctionDescriptors().addAll(functionGroup.getFunctionDescriptors());
    }
}
