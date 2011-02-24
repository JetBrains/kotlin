package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class WritableScope extends JetScopeAdapter {
    @Nullable
    private Map<String, PropertyDescriptor> propertyDescriptors;
    @Nullable
    private Map<String, WritableFunctionGroup> functionGroups;
    @Nullable
    private Map<String, TypeParameterDescriptor> typeParameterDescriptors;

    public WritableScope(JetScope scope) {
        super(scope);
    }

    public WritableScope() {
        super(JetScope.EMPTY);
    }

    @NotNull
    private Map<String, PropertyDescriptor> getPropertyDescriptors() {
        if (propertyDescriptors == null) {
            propertyDescriptors = new HashMap<String, PropertyDescriptor>();
        }
        return propertyDescriptors;
    }

    public void addPropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors();
        if (propertyDescriptors.containsKey(propertyDescriptor.getName())) {
            throw new UnsupportedOperationException("Property redeclared: " + propertyDescriptor.getName());
        }
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

    @Override
    @NotNull
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        WritableFunctionGroup functionGroup = getFunctionGroups().get(name);
        if (functionGroup == null) {
            return FunctionGroup.EMPTY;
        }
        return functionGroup;
    }

    @NotNull
    private Map<String, TypeParameterDescriptor> getTypeParameterDescriptors() {
        if (typeParameterDescriptors == null) {
            typeParameterDescriptors = new HashMap<String, TypeParameterDescriptor>();
        }
        return typeParameterDescriptors;
    }

    public void addTypeParameterDescriptor(TypeParameterDescriptor typeParameterDescriptor) {
        String name = typeParameterDescriptor.getName();
        Map<String, TypeParameterDescriptor> typeParameterDescriptors = getTypeParameterDescriptors();
        if (typeParameterDescriptors.containsKey(name)) {
            throw new UnsupportedOperationException("Type parameter redeclared"); // TODO
        }
        typeParameterDescriptors.put(name, typeParameterDescriptor);
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(String name) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptors().get(name);
        if (typeParameterDescriptor != null) {
            return typeParameterDescriptor;
        }
        return super.getTypeParameter(name);
    }

    @Override
    public ClassDescriptor getClass(String name) {
        return super.getClass(name); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Type getThisType() {
        throw new UnsupportedOperationException(); // TODO
    }
}
