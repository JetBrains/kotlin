package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class WritableScope extends JetScopeAdapter {
    @NotNull
    private final ErrorHandler errorHandler;

    @Nullable
    private Map<String, PropertyDescriptor> propertyDescriptors;
    @Nullable
    private Map<String, WritableFunctionGroup> functionGroups;
    @Nullable
    private Map<String, ClassifierDescriptor> classifierDescriptors;
    @Nullable
    private Map<String, NamespaceDescriptor> namespaceDescriptors;
    @Nullable
    private JetType thisType;
    @Nullable
    private List<JetScope> imports;

    @NotNull
    private final DeclarationDescriptor ownerDeclarationDescriptor;

    public WritableScope(@NotNull JetScope scope, @NotNull DeclarationDescriptor owner, @NotNull ErrorHandler errorHandler) {
        super(scope);
        this.ownerDeclarationDescriptor = owner;
        this.errorHandler = errorHandler;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return ownerDeclarationDescriptor;
    }

    public void importScope(@NotNull JetScope imported) {
        getImports().add(0, imported);
    }

    @NotNull
    private List<JetScope> getImports() {
        if (imports == null) {
            imports = new ArrayList<JetScope>();
        }
        return imports;
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
        PropertyDescriptor existingDescriptor = propertyDescriptors.get(propertyDescriptor.getName());
        if (existingDescriptor != null) {
            errorHandler.redeclaration(existingDescriptor, propertyDescriptor);
        }
        // TODO : Should this always happen?
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
        propertyDescriptor = super.getProperty(name);
        if (propertyDescriptor != null) {
            return propertyDescriptor;
        }
        for (JetScope imported : getImports()) {
            PropertyDescriptor importedDescriptor = imported.getProperty(name);
            if (importedDescriptor != null) {
                return importedDescriptor;
            }
        }
        return null;
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
        FunctionGroup functionGroup = getFunctionGroups().get(name);
        if (functionGroup != null && !functionGroup.isEmpty()) {
            return functionGroup;
        }
        // TODO : this logic is questionable
        functionGroup = super.getFunctionGroup(name);
        if (!functionGroup.isEmpty()) return functionGroup;
        for (JetScope imported : getImports()) {
            FunctionGroup importedDescriptor = imported.getFunctionGroup(name);
            if (!importedDescriptor.isEmpty()) {
                return importedDescriptor;
            }
        }
        return functionGroup;
    }

    public void addTypeParameterDescriptor(TypeParameterDescriptor typeParameterDescriptor) {
        String name = typeParameterDescriptor.getName();
        Map<String, ClassifierDescriptor> classifierDescriptors = getClassifierDescriptors();
        ClassifierDescriptor originalDescriptor = classifierDescriptors.get(name);
        if (originalDescriptor != null) {
            errorHandler.redeclaration(originalDescriptor, typeParameterDescriptor);
        }
        classifierDescriptors.put(name, typeParameterDescriptor);
    }

    @NotNull
    private Map<String, ClassifierDescriptor> getClassifierDescriptors() {
        if (classifierDescriptors == null) {
            classifierDescriptors = new HashMap<String, ClassifierDescriptor>();
        }
        return classifierDescriptors;
    }

    public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        addClassifierAlias(classDescriptor.getName(), classDescriptor);
    }

    public void addClassifierAlias(String name, ClassifierDescriptor classifierDescriptor) {
        Map<String, ClassifierDescriptor> classifierDescriptors = getClassifierDescriptors();
        ClassifierDescriptor originalDescriptor = classifierDescriptors.get(name);
        if (originalDescriptor != null) {
            errorHandler.redeclaration(originalDescriptor, classifierDescriptor);
        }
        classifierDescriptors.put(name, classifierDescriptor);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor classifierDescriptor = getClassifierDescriptors().get(name);
        if (classifierDescriptor != null) return classifierDescriptor;

        classifierDescriptor = super.getClassifier(name);
        if (classifierDescriptor != null) return classifierDescriptor;
        for (JetScope imported : getImports()) {
            ClassifierDescriptor importedClassifier = imported.getClassifier(name);
            if (importedClassifier != null) {
                return importedClassifier;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public JetType getThisType() {
        if (thisType == null) {
            return super.getThisType();
        }
        return thisType;
    }

    @NotNull
    public Map<String, NamespaceDescriptor> getNamespaceDescriptors() {
        if (namespaceDescriptors == null) {
            namespaceDescriptors = new HashMap<String, NamespaceDescriptor>();
        }
        return namespaceDescriptors;
    }

    public void addNamespace(NamespaceDescriptor namespaceDescriptor) {
        NamespaceDescriptor oldValue = getNamespaceDescriptors().put(namespaceDescriptor.getName(), namespaceDescriptor);
        if (oldValue != null) {
            errorHandler.redeclaration(oldValue, namespaceDescriptor);
        }
    }

    public NamespaceDescriptor getDeclaredNamespace(@NotNull String name) {
        NamespaceDescriptor namespaceDescriptor = getNamespaceDescriptors().get(name);
        if (namespaceDescriptor != null) return namespaceDescriptor;
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        NamespaceDescriptor declaredNamespace = getDeclaredNamespace(name);
        if (declaredNamespace != null) return declaredNamespace;

        NamespaceDescriptor namespace = super.getNamespace(name);
        if (namespace != null) return namespace;
        for (JetScope imported : getImports()) {
            NamespaceDescriptor importedDescriptor = imported.getNamespace(name);
            if (importedDescriptor != null) {
                return importedDescriptor;
            }
        }
        return null;
    }

    public void setThisType(JetType thisType) {
        if (this.thisType != null) {
            throw new UnsupportedOperationException("Receiver redeclared");
        }
        this.thisType = thisType;
    }
}
