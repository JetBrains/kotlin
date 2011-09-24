package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.util.CommonSuppliers;

import java.util.*;

/**
 * @author abreslav
 */
public class WritableScopeImpl extends WritableScopeWithImports {

    private final Collection<DeclarationDescriptor> allDescriptors = Sets.newLinkedHashSet();
    private boolean allDescriptorsDone = false;

    @NotNull
    private final DeclarationDescriptor ownerDeclarationDescriptor;

    // FieldNames include "$"
    @Nullable
    private Map<String, PropertyDescriptor> propertyDescriptorsByFieldNames;

    @Nullable
    private SetMultimap<String, FunctionDescriptor> functionGroups;
    @Nullable
    private Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors;

    @Nullable
    private Map<String, List<DeclarationDescriptor>> labelsToDescriptors;

    @Nullable
    private ReceiverDescriptor implicitReceiver;

    public WritableScopeImpl(@NotNull JetScope scope, @NotNull DeclarationDescriptor owner, @NotNull RedeclarationHandler redeclarationHandler) {
        super(scope, redeclarationHandler);
        this.ownerDeclarationDescriptor = owner;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return ownerDeclarationDescriptor;
    }

    @Override
    public DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis() {
        if (DescriptorUtils.definesItsOwnThis(ownerDeclarationDescriptor)) {
            return ownerDeclarationDescriptor;
        }
        return super.getDeclarationDescriptorForUnqualifiedThis();
    }

    @Override
    public void importScope(@NotNull JetScope imported) {
        super.importScope(imported);
    }

    @Override
    public void importClassifierAlias(@NotNull String importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor) {
        allDescriptors.add(classifierDescriptor);
        super.importClassifierAlias(importedClassifierName, classifierDescriptor);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (!allDescriptorsDone) {
            allDescriptorsDone = true;
            allDescriptors.addAll(getWorkerScope().getAllDescriptors());
            for (JetScope imported : getImports()) {
                allDescriptors.addAll(imported.getAllDescriptors());
            }
        }
        return allDescriptors;
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
        Collection<DeclarationDescriptor> superResult = super.getDeclarationsByLabel(labelName);
        Map<String, List<DeclarationDescriptor>> labelsToDescriptors = getLabelsToDescriptors();
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
    private Map<String, DeclarationDescriptor> getVariableClassOrNamespaceDescriptors() {
        if (variableClassOrNamespaceDescriptors == null) {
            variableClassOrNamespaceDescriptors = Maps.newHashMap();
        }
        return variableClassOrNamespaceDescriptors;
    }

    @Override
    public void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor) {
        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor existingDescriptor = variableClassOrNamespaceDescriptors.get(variableDescriptor.getName());
        if (existingDescriptor != null) {
            redeclarationHandler.handleRedeclaration(existingDescriptor, variableDescriptor);
        }
        // TODO : Should this always happen?
        variableClassOrNamespaceDescriptors.put(variableDescriptor.getName(), variableDescriptor);
        allDescriptors.add(variableDescriptor);
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor descriptor = variableClassOrNamespaceDescriptors.get(name);
        if (descriptor instanceof VariableDescriptor) {
            return (VariableDescriptor) descriptor;
        }

        VariableDescriptor variableDescriptor = getWorkerScope().getVariable(name);
        if (variableDescriptor != null) {
            return variableDescriptor;
        }
        return super.getVariable(name);
    }

    @NotNull
    private SetMultimap<String, FunctionDescriptor> getFunctionGroups() {
        if (functionGroups == null) {
            functionGroups = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        }
        return functionGroups;
    }

    @Override
    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        getFunctionGroups().put(functionDescriptor.getName(), functionDescriptor);
        allDescriptors.add(functionDescriptor);
    }

    @Override
    @NotNull
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        Set<FunctionDescriptor> result = Sets.newLinkedHashSet(getFunctionGroups().get(name));

        result.addAll(getWorkerScope().getFunctions(name));

        result.addAll(super.getFunctions(name));

        return result;
    }

    @Override
    public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        String name = typeParameterDescriptor.getName();
        addClassifierAlias(name, typeParameterDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        addClassifierAlias(classDescriptor.getName(), classDescriptor);
    }

    @Override
    public void addClassifierAlias(@NotNull String name, @NotNull ClassifierDescriptor classifierDescriptor) {
        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor originalDescriptor = variableClassOrNamespaceDescriptors.get(name);
        if (originalDescriptor != null) {
            redeclarationHandler.handleRedeclaration(originalDescriptor, classifierDescriptor);
        }
        variableClassOrNamespaceDescriptors.put(name, classifierDescriptor);
        allDescriptors.add(classifierDescriptor);
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor descriptor = variableClassOrNamespaceDescriptors.get(name);
        if (descriptor instanceof ClassifierDescriptor) return (ClassifierDescriptor) descriptor;

        ClassifierDescriptor classifierDescriptor = getWorkerScope().getClassifier(name);
        if (classifierDescriptor != null) return classifierDescriptor;

        return super.getClassifier(name);
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        if (implicitReceiver == null) {
            return super.getImplicitReceiver();
        }
        return implicitReceiver;
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor oldValue = variableClassOrNamespaceDescriptors.put(namespaceDescriptor.getName(), namespaceDescriptor);
        if (oldValue != null) {
            redeclarationHandler.handleRedeclaration(oldValue, namespaceDescriptor);
        }
        allDescriptors.add(namespaceDescriptor);
    }

    @Override
    public NamespaceDescriptor getDeclaredNamespace(@NotNull String name) {
        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor namespaceDescriptor = variableClassOrNamespaceDescriptors.get(name);
        if (namespaceDescriptor instanceof NamespaceDescriptor) return (NamespaceDescriptor) namespaceDescriptor;
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        NamespaceDescriptor declaredNamespace = getDeclaredNamespace(name);
        if (declaredNamespace != null) return declaredNamespace;

        NamespaceDescriptor namespace = getWorkerScope().getNamespace(name);
        if (namespace != null) return namespace;
        return super.getNamespace(name);
    }

    @Override
    public void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver) {
        if (this.implicitReceiver != null) {
            throw new UnsupportedOperationException("Receiver redeclared");
        }
        this.implicitReceiver = implicitReceiver;
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        if (implicitReceiver != null && implicitReceiver.exists()) {
            result.add(implicitReceiver);
        }
        super.getImplicitReceiversHierarchy(result);
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

    public List<VariableDescriptor> getDeclaredVariables() {
        List<VariableDescriptor> result = Lists.newArrayList();
        for (DeclarationDescriptor descriptor : getVariableClassOrNamespaceDescriptors().values()) {
            if (descriptor instanceof VariableDescriptor) {
                VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
                result.add(variableDescriptor);
            }
        }
        return result;
    }

    public boolean hasDeclaredItems() {
        return variableClassOrNamespaceDescriptors != null  && !variableClassOrNamespaceDescriptors.isEmpty();
    }

    @Override
    public WritableScopeImpl setDebugName(@NotNull String debugName) {
        super.setDebugName(debugName);
        return this;
    }
}
