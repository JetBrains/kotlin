package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.types.*;

import java.util.*;

/**
 * @author abreslav
 */
public class WritableScopeImpl extends WritableScopeWithImports {
    @NotNull
    private final ErrorHandler errorHandler;

    @NotNull
    private final DeclarationDescriptor ownerDeclarationDescriptor;

    // FieldNames include "$"
    @Nullable
    private Map<String, PropertyDescriptor> propertyDescriptorsByFieldNames;

    @Nullable
    private Map<String, VariableDescriptor> variableDescriptors;
    @Nullable
    private Map<String, WritableFunctionGroup> functionGroups;
    @Nullable
    private Map<String, ClassifierDescriptor> classifierDescriptors;
    @Nullable
    private Map<String, NamespaceDescriptor> namespaceDescriptors;
    @Nullable
    private Map<String, List<DeclarationDescriptor>> labelsToDescriptors;
    @Nullable
    private JetType thisType;

    public WritableScopeImpl(@NotNull JetScope scope, @NotNull DeclarationDescriptor owner, @NotNull ErrorHandler errorHandler) {
        super(scope);
        this.ownerDeclarationDescriptor = owner;
        this.errorHandler = errorHandler;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return ownerDeclarationDescriptor;
    }

    @NotNull
    private Map<String, List<DeclarationDescriptor>> getLabelsToDescriptors() {
        if (labelsToDescriptors == null) {
            labelsToDescriptors = new HashMap<String, List<DeclarationDescriptor>>();
        }
        return labelsToDescriptors;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull String labelName) {
        Map<String, List<DeclarationDescriptor>> labelsToDescriptors = getLabelsToDescriptors();
        Collection<DeclarationDescriptor> superResult = super.getDeclarationsByLabel(labelName);
        List<DeclarationDescriptor> declarationDescriptors = labelsToDescriptors.get(labelName);
        if (declarationDescriptors == null) {
            return superResult;
        }
        if (superResult.isEmpty()) return declarationDescriptors;
        List<DeclarationDescriptor> result = new ArrayList<DeclarationDescriptor>(declarationDescriptors);
        result.addAll(superResult);
        return result;
    }

    @Override
    public void addLabeledDeclaration(@NotNull DeclarationDescriptor descriptor) {
        Map<String, List<DeclarationDescriptor>> labelsToDescriptors = getLabelsToDescriptors();
        String name = descriptor.getName();
        assert name != null;
        List<DeclarationDescriptor> declarationDescriptors = labelsToDescriptors.get(name);
        if (declarationDescriptors == null) {
            declarationDescriptors = new ArrayList<DeclarationDescriptor>();
            labelsToDescriptors.put(name, declarationDescriptors);
        }
        declarationDescriptors.add(descriptor);
    }

    @NotNull
    private Map<String, VariableDescriptor> getVariableDescriptors() {
        if (variableDescriptors == null) {
            variableDescriptors = new HashMap<String, VariableDescriptor>();
        }
        return variableDescriptors;
    }

    @Override
    public void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor) {
        Map<String, VariableDescriptor> propertyDescriptors = getVariableDescriptors();
        VariableDescriptor existingDescriptor = propertyDescriptors.get(variableDescriptor.getName());
        if (existingDescriptor != null) {
            errorHandler.redeclaration(existingDescriptor, variableDescriptor);
        }
        // TODO : Should this always happen?
        propertyDescriptors.put(variableDescriptor.getName(), variableDescriptor);
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        @NotNull
        Map<String, VariableDescriptor> propertyDescriptors = getVariableDescriptors();
        VariableDescriptor variableDescriptor = propertyDescriptors.get(name);
        if (variableDescriptor != null) {
            return variableDescriptor;
        }
        variableDescriptor = getWorkerScope().getVariable(name);
        if (variableDescriptor != null) {
            return variableDescriptor;
        }
//        for (JetScope imported : getImports()) {
//            VariableDescriptor importedDescriptor = imported.getVariable(name);
//            if (importedDescriptor != null) {
//                return importedDescriptor;
//            }
//        }
//        return null;
        return super.getVariable(name);
    }

    @NotNull
    private Map<String, WritableFunctionGroup> getFunctionGroups() {
        if (functionGroups == null) {
            functionGroups = new HashMap<String, WritableFunctionGroup>();
        }
        return functionGroups;
    }

    @Override
    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
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
        functionGroup = getWorkerScope().getFunctionGroup(name);
        if (!functionGroup.isEmpty()) return functionGroup;
//        for (JetScope imported : getImports()) {
//            FunctionGroup importedDescriptor = imported.getFunctionGroup(name);
//            if (!importedDescriptor.isEmpty()) {
//                return importedDescriptor;
//            }
//        }
//        return functionGroup;
        return super.getFunctionGroup(name);
    }

    @Override
    public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
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

    @Override
    public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        addClassifierAlias(classDescriptor.getName(), classDescriptor);
    }

    @Override
    public void addClassifierAlias(@NotNull String name, @NotNull ClassifierDescriptor classifierDescriptor) {
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

        classifierDescriptor = getWorkerScope().getClassifier(name);
        if (classifierDescriptor != null) return classifierDescriptor;
//        for (JetScope imported : getImports()) {
//            ClassifierDescriptor importedClassifier = imported.getClassifier(name);
//            if (importedClassifier != null) {
//                return importedClassifier;
//            }
//        }
//        return null;
        return super.getClassifier(name);
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

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        NamespaceDescriptor oldValue = getNamespaceDescriptors().put(namespaceDescriptor.getName(), namespaceDescriptor);
        if (oldValue != null) {
            errorHandler.redeclaration(oldValue, namespaceDescriptor);
        }
    }

    @Override
    public NamespaceDescriptor getDeclaredNamespace(@NotNull String name) {
        NamespaceDescriptor namespaceDescriptor = getNamespaceDescriptors().get(name);
        if (namespaceDescriptor != null) return namespaceDescriptor;
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        NamespaceDescriptor declaredNamespace = getDeclaredNamespace(name);
        if (declaredNamespace != null) return declaredNamespace;

        NamespaceDescriptor namespace = getWorkerScope().getNamespace(name);
        if (namespace != null) return namespace;
//        for (JetScope imported : getImports()) {
//            NamespaceDescriptor importedDescriptor = imported.getNamespace(name);
//            if (importedDescriptor != null) {
//                return importedDescriptor;
//            }
//        }
//        return null;
        return super.getNamespace(name);
    }

    @Override
    public void setThisType(@NotNull JetType thisType) {
        if (this.thisType != null) {
            throw new UnsupportedOperationException("Receiver redeclared");
        }
        this.thisType = thisType;
    }

    @SuppressWarnings({"NullableProblems"})
    @NotNull
    private Map<String, PropertyDescriptor> getPropertyDescriptorsByFieldNames() {
        if (propertyDescriptorsByFieldNames == null) {
            propertyDescriptorsByFieldNames = new HashMap<String, PropertyDescriptor>();
        }
        return propertyDescriptorsByFieldNames;
    }

    @Override
    public void addPropertyDescriptorByFieldName(@NotNull String fieldName, @NotNull PropertyDescriptor propertyDescriptor) {
        getPropertyDescriptorsByFieldNames().put(fieldName, propertyDescriptor);
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        PropertyDescriptor descriptor = getPropertyDescriptorsByFieldNames().get(fieldName);
        if (descriptor != null) return descriptor;
        return super.getPropertyByFieldReference(fieldName);
    }
}
