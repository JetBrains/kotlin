package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class WritableScope implements JetScope {
    @Nullable
    private Map<String, PropertyDescriptor> propertyDescriptors;
    @Nullable
    private Map<String, WritableFunctionGroup> functionGroups;

    @NotNull
    private Map<String, PropertyDescriptor> getPropertyDescriptors() {
        if (propertyDescriptors == null) {
            propertyDescriptors = new HashMap<String, PropertyDescriptor>();
        }
        return propertyDescriptors;
    }

    public void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors();
        assert !propertyDescriptors.containsKey(propertyDescriptor.getName()) : "Property redeclared";
        propertyDescriptors.put(propertyDescriptor.getName(), propertyDescriptor);
    }

    @Override
    public PropertyDescriptor getProperty(String name) {
        @NotNull
        Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors();
        return propertyDescriptors.get(name);
    }

    @NotNull
    private Map<String, WritableFunctionGroup> getFunctionGroups() {
        if (functionGroups == null) {
            functionGroups = new HashMap<String, WritableFunctionGroup>();
        }
        return functionGroups;
    }

    public void addFunctionDescriptor(FunctionDescriptor functionDescriptor) {
        String name = functionDescriptor.getName();
        Map<String, WritableFunctionGroup> functionGroups = getFunctionGroups();

        @Nullable
        WritableFunctionGroup functionGroup = functionGroups.get(name);
        if (functionGroup == null) {
            functionGroup = new WritableFunctionGroup(name);
            functionGroups.put(name, functionGroup);
        }
        functionGroup.addFunction(functionDescriptor);
    }

    @NotNull
    private FunctionGroup getFunctionGroup(String name) {
        WritableFunctionGroup functionGroup = getFunctionGroups().get(name);
        if (functionGroup == null) {
            return FunctionGroup.EMPTY;
        }
        return functionGroup;
    }

    @NotNull
    @Override
    public OverloadDomain getOverloadDomain(Type receiverType, String referencedName) {
        final FunctionGroup functionGroup = getFunctionGroups().get(referencedName);
        if (functionGroup == null) {
            return OverloadDomain.EMPTY;
        }

        return new OverloadDomain() {
            @Override
            public Type getReturnTypeForPositionedArguments(@NotNull List<Type> typeArguments, @NotNull List<Type> positionedValueArgumentTypes) {
                Collection<FunctionDescriptor> possiblyApplicableFunctions = functionGroup.getPossiblyApplicableFunctions(typeArguments, positionedValueArgumentTypes);
                if (possiblyApplicableFunctions.isEmpty()) {
                    return null;
                }

                throw new UnsupportedOperationException();
            }

            @Override
            public Type getReturnTypeForNamedArguments(@NotNull List<Type> typeArguments, @NotNull Map<String, Type> valueArgumentTypes, @Nullable Type functionLiteralArgumentType) {
                throw new UnsupportedOperationException(); // TODO
            }
        };
    }

    @Override
    public ClassDescriptor getClass(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Type getThisType() {
        throw new UnsupportedOperationException(); // TODO
    }
}
