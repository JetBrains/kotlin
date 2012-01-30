package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
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
    private SetMultimap<String, VariableDescriptor> propertyGroups;

    @Nullable
    private Map<String, NamespaceDescriptor> namespaceAliases;

    @Nullable
    private Map<String, List<DeclarationDescriptor>> labelsToDescriptors;
    
    @Nullable
    private Map<String, ClassDescriptor> objectDescriptors;

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
    public void importScope(@NotNull JetScope imported) {
        checkMayWrite();
        super.importScope(imported);
    }

    @Override
    public void importClassifierAlias(@NotNull String importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        allDescriptors.add(classifierDescriptor);
        super.importClassifierAlias(importedClassifierName, classifierDescriptor);
    }

    @Override
    public void importNamespaceAlias(@NotNull String aliasName, @NotNull NamespaceDescriptor namespaceDescriptor) {
        checkMayWrite();

        allDescriptors.add(namespaceDescriptor);
        super.importNamespaceAlias(aliasName, namespaceDescriptor);
    }

    @Override
    public void importFunctionAlias(@NotNull String aliasName, @NotNull FunctionDescriptor functionDescriptor) {
        checkMayWrite();

        addFunctionDescriptor(functionDescriptor);
        super.importFunctionAlias(aliasName, functionDescriptor);

    }

    @Override
    public void importVariableAlias(@NotNull String aliasName, @NotNull VariableDescriptor variableDescriptor) {
        checkMayWrite();

        addPropertyDescriptor(variableDescriptor);
        super.importVariableAlias(aliasName, variableDescriptor);
    }

    @Override
    public void clearImports() {
        checkMayWrite();

        super.clearImports();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        checkMayRead();

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
    private Map<String, ClassDescriptor> getObjectDescriptorsMap() {
        if (objectDescriptors == null) {
            objectDescriptors = Maps.newHashMap();
        }
        return objectDescriptors;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull String labelName) {
        checkMayRead();

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
        checkMayWrite();

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

    @NotNull
    private Map<String, NamespaceDescriptor> getNamespaceAliases() {
        if (namespaceAliases == null) {
            namespaceAliases = Maps.newHashMap();
        }
        return namespaceAliases;
    }

    @Override
    public void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor) {
        addVariableDescriptor(variableDescriptor, false);
    }
    
    @Override
    public void addPropertyDescriptor(@NotNull VariableDescriptor propertyDescriptor) {
        addVariableDescriptor(propertyDescriptor, true);
    }
    
    private void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor, boolean isProperty) {
        checkMayWrite();

        String name = variableDescriptor.getName();
        if (isProperty) {
            checkForPropertyRedeclaration(name, variableDescriptor);
            getPropertyGroups().put(name, variableDescriptor);
        }
        if (!variableDescriptor.getReceiverParameter().exists()) {
            checkForRedeclaration(name, variableDescriptor);
            // TODO : Should this always happen?
            getVariableClassOrNamespaceDescriptors().put(name, variableDescriptor);
        }
        allDescriptors.add(variableDescriptor);
    }

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull String name) {
        checkMayRead();

        Set<VariableDescriptor> result = Sets.newLinkedHashSet(getPropertyGroups().get(name));

        result.addAll(getWorkerScope().getProperties(name));

        result.addAll(super.getProperties(name));
        
        return result;
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull String name) {
        checkMayRead();

        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor descriptor = variableClassOrNamespaceDescriptors.get(name);
        if (descriptor instanceof VariableDescriptor && !getPropertyGroups().get(name).contains(descriptor)) {
            return (VariableDescriptor) descriptor;
        }

        VariableDescriptor variableDescriptor = getWorkerScope().getLocalVariable(name);
        if (variableDescriptor != null) {
            return variableDescriptor;
        }
        return super.getLocalVariable(name);
    }

    @NotNull
    private SetMultimap<String, VariableDescriptor> getPropertyGroups() {
        if (propertyGroups == null) {
            propertyGroups = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        }
        return propertyGroups;
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
        checkMayWrite();

        getFunctionGroups().put(functionDescriptor.getName(), functionDescriptor);
        allDescriptors.add(functionDescriptor);
    }

    @Override
    @NotNull
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        checkMayRead();

        Set<FunctionDescriptor> result = Sets.newLinkedHashSet(getFunctionGroups().get(name));

        result.addAll(getWorkerScope().getFunctions(name));

        result.addAll(super.getFunctions(name));

        return result;
    }

    @Override
    public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        checkMayWrite();

        String name = typeParameterDescriptor.getName();
        addClassifierAlias(name, typeParameterDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        checkMayWrite();

        addClassifierAlias(classDescriptor.getName(), classDescriptor);
    }

    @Override
    public void addObjectDescriptor(@NotNull ClassDescriptor objectDescriptor) {
        checkMayWrite();
        
        getObjectDescriptorsMap().put(objectDescriptor.getName(), objectDescriptor);
    }

    @Override
    public void addClassifierAlias(@NotNull String name, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        checkForRedeclaration(name, classifierDescriptor);
        getVariableClassOrNamespaceDescriptors().put(name, classifierDescriptor);
        allDescriptors.add(classifierDescriptor);
    }

    @Override
    public void addNamespaceAlias(@NotNull String name, @NotNull NamespaceDescriptor namespaceDescriptor) {
        checkMayWrite();

        checkForRedeclaration(name, namespaceDescriptor);
        getNamespaceAliases().put(name, namespaceDescriptor);
        allDescriptors.add(namespaceDescriptor);
    }

    @Override
    public void addFunctionAlias(@NotNull String name, @NotNull FunctionDescriptor functionDescriptor) {
        checkMayWrite();
        
        checkForRedeclaration(name, functionDescriptor);
        getFunctionGroups().put(name, functionDescriptor);
        allDescriptors.add(functionDescriptor);
    }

    @Override
    public void addVariableAlias(@NotNull String name, @NotNull VariableDescriptor variableDescriptor) {
        checkMayWrite();
        
        checkForRedeclaration(name, variableDescriptor);
        getVariableClassOrNamespaceDescriptors().put(name, variableDescriptor);
        allDescriptors.add(variableDescriptor);
    }
    
    private void checkForPropertyRedeclaration(String name, VariableDescriptor variableDescriptor) {
        Set<VariableDescriptor> properties = getPropertyGroups().get(name);
        ReceiverDescriptor receiverParameter = variableDescriptor.getReceiverParameter();
        for (VariableDescriptor oldProperty : properties) {
            ReceiverDescriptor receiverParameterForOldVariable = oldProperty.getReceiverParameter();
            if (((receiverParameter.exists() && receiverParameterForOldVariable.exists()) &&
                 (JetTypeChecker.INSTANCE.equalTypes(receiverParameter.getType(), receiverParameterForOldVariable.getType())))) {
                redeclarationHandler.handleRedeclaration(oldProperty, variableDescriptor);
            }
        }
    }

    private void checkForRedeclaration(String name, DeclarationDescriptor classifierDescriptor) {
        DeclarationDescriptor originalDescriptor = getVariableClassOrNamespaceDescriptors().get(name);
        if (originalDescriptor != null) {
            redeclarationHandler.handleRedeclaration(originalDescriptor, classifierDescriptor);
        }
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        checkMayRead();

        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor descriptor = variableClassOrNamespaceDescriptors.get(name);
        if (descriptor instanceof ClassifierDescriptor) return (ClassifierDescriptor) descriptor;

        ClassifierDescriptor classifierDescriptor = getWorkerScope().getClassifier(name);
        if (classifierDescriptor != null) return classifierDescriptor;

        return super.getClassifier(name);
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull String name) {
        return getObjectDescriptorsMap().get(name);
    }

    @NotNull
    @Override
    public Set<ClassDescriptor> getObjectDescriptors() {
        return Sets.newHashSet(getObjectDescriptorsMap().values());
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        checkMayWrite();

        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor oldValue = variableClassOrNamespaceDescriptors.put(namespaceDescriptor.getName(), namespaceDescriptor);
        if (oldValue != null) {
            redeclarationHandler.handleRedeclaration(oldValue, namespaceDescriptor);
        }
        allDescriptors.add(namespaceDescriptor);
    }

    @Override
    public NamespaceDescriptor getDeclaredNamespace(@NotNull String name) {
        checkMayRead();

        Map<String, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableClassOrNamespaceDescriptors();
        DeclarationDescriptor namespaceDescriptor = variableClassOrNamespaceDescriptors.get(name);
        if (namespaceDescriptor instanceof NamespaceDescriptor) return (NamespaceDescriptor) namespaceDescriptor;
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        checkMayRead();

        NamespaceDescriptor declaredNamespace = getDeclaredNamespace(name);
        if (declaredNamespace != null) return declaredNamespace;

        NamespaceDescriptor aliased = getNamespaceAliases().get(name);
        if (aliased != null) return aliased;

        NamespaceDescriptor namespace = getWorkerScope().getNamespace(name);
        if (namespace != null) return namespace;
        return super.getNamespace(name);
    }

    @NotNull
    @Override
    public ReceiverDescriptor getImplicitReceiver() {
        checkMayRead();

        if (implicitReceiver == null) {
            return super.getImplicitReceiver();
        }
        return implicitReceiver;
    }

    @Override
    public void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver) {
        checkMayWrite();

        if (this.implicitReceiver != null) {
            throw new UnsupportedOperationException("Receiver redeclared");
        }
        this.implicitReceiver = implicitReceiver;
    }

    @Override
    public void getImplicitReceiversHierarchy(@NotNull List<ReceiverDescriptor> result) {
        checkMayRead();

        if (implicitReceiver != null && implicitReceiver.exists()) {
            result.add(implicitReceiver);
        }
        super.getImplicitReceiversHierarchy(result);
    }

//    @SuppressWarnings({"NullableProblems"})
    @NotNull
    private Map<String, PropertyDescriptor> getPropertyDescriptorsByFieldNames() {
        if (propertyDescriptorsByFieldNames == null) {
            propertyDescriptorsByFieldNames = new HashMap<String, PropertyDescriptor>();
        }
        return propertyDescriptorsByFieldNames;
    }

    @Override
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        checkMayRead();

        if (!fieldName.startsWith("$")) {
            throw new IllegalStateException();
        }

        PropertyDescriptor descriptor = getPropertyDescriptorsByFieldNames().get(fieldName);
        if (descriptor != null) return descriptor;
        return super.getPropertyByFieldReference(fieldName);
    }

    public List<VariableDescriptor> getDeclaredVariables() {
        checkMayRead();

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
        checkMayWrite();

        super.setDebugName(debugName);
        return this;
    }
}
