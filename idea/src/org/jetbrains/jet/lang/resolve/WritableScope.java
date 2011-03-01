package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.HashMap;
import java.util.Map;

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
    @Nullable
    private Map<String, ClassDescriptor> classDescriptors;
    @Nullable
    private Type thisType;

    public WritableScope(JetScope scope) {
        super(scope);
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
    public PropertyDescriptor getProperty(@NotNull String name) {
        @NotNull
        Map<String, PropertyDescriptor> propertyDescriptors = getPropertyDescriptors();
        PropertyDescriptor propertyDescriptor = propertyDescriptors.get(name);
        if (propertyDescriptor != null) {
            return propertyDescriptor;
        }
        return super.getProperty(name);
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
        if (functionGroup != null && !functionGroup.isEmpty()) {
            return functionGroup;
        }
        return super.getFunctionGroup(name);
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
    public TypeParameterDescriptor getTypeParameter(@NotNull String name) {
        TypeParameterDescriptor typeParameterDescriptor = getTypeParameterDescriptors().get(name);
        if (typeParameterDescriptor != null) {
            return typeParameterDescriptor;
        }
        return super.getTypeParameter(name);
    }

    @NotNull
    private Map<String, ClassDescriptor> getClassDescriptors() {
        if (classDescriptors == null) {
            classDescriptors = new HashMap<String, ClassDescriptor>();
        }
        return classDescriptors;
    }

    public void addClassDescriptor(@NotNull ClassDescriptor classDescriptor) {
        addClassAlias(classDescriptor.getName(), classDescriptor);
    }

    public void addClassAlias(String name, ClassDescriptor classDescriptor) {
        Map<String, ClassDescriptor> classDescriptors = getClassDescriptors();
        if (classDescriptors.put(name, classDescriptor) != null) {
            throw new UnsupportedOperationException("Class redeclared: " + classDescriptor.getName());
        }
    }

    @Override
    public ClassDescriptor getClass(@NotNull String name) {
        ClassDescriptor classDescriptor = getClassDescriptors().get(name);
        if (classDescriptor != null) {
            return classDescriptor;
        }
        return super.getClass(name);
    }

    @NotNull
    @Override
    public Type getThisType() {
        if (thisType == null) {
            return super.getThisType();
        }
        return thisType;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return super.getNamespace(name); // TODO
    }

    @Override
    public ExtensionDescriptor getExtension(@NotNull String name) {
        return super.getExtension(name); // TODO
    }

    public void setThisType(Type thisType) {
        if (this.thisType != null) {
            throw new UnsupportedOperationException("Receiver redeclared");
        }
        this.thisType = thisType;
    }
}
