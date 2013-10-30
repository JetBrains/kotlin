/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.utils.CommonSuppliers;
import org.jetbrains.jet.utils.Printer;

import java.util.*;

// Reads from:
// 1. Maps
// 2. Worker
// 3. Imports

// Writes to: maps

public class WritableScopeImpl extends WritableScopeWithImports {

    private final Collection<DeclarationDescriptor> allDescriptors = Lists.newArrayList();
    private final Multimap<Name, DeclarationDescriptor> declaredDescriptorsAccessibleBySimpleName = HashMultimap.create();
    private boolean allDescriptorsDone = false;

    @NotNull
    private final DeclarationDescriptor ownerDeclarationDescriptor;

    @Nullable
    private SetMultimap<Name, FunctionDescriptor> functionGroups;

    @Nullable
    private Map<Name, DeclarationDescriptor> variableOrClassDescriptors;
    
    @Nullable
    private SetMultimap<Name, VariableDescriptor> propertyGroups;

    @Nullable
    private Map<Name, PackageViewDescriptor> packageAliases;

    @Nullable
    private Map<LabelName, List<DeclarationDescriptor>> labelsToDescriptors;

    @Nullable
    private ReceiverParameterDescriptor implicitReceiver;

    public WritableScopeImpl(@NotNull JetScope scope, @NotNull DeclarationDescriptor owner,
            @NotNull RedeclarationHandler redeclarationHandler, @NotNull String debugName) {
        super(scope, redeclarationHandler, debugName);
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
    public void importClassifierAlias(@NotNull Name importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        allDescriptors.add(classifierDescriptor);
        super.importClassifierAlias(importedClassifierName, classifierDescriptor);
    }

    @Override
    public void importPackageAlias(@NotNull Name aliasName, @NotNull PackageViewDescriptor packageView) {
        checkMayWrite();

        allDescriptors.add(packageView);
        super.importPackageAlias(aliasName, packageView);
    }

    @Override
    public void importFunctionAlias(@NotNull Name aliasName, @NotNull FunctionDescriptor functionDescriptor) {
        checkMayWrite();

        addFunctionDescriptor(functionDescriptor);
        super.importFunctionAlias(aliasName, functionDescriptor);

    }

    @Override
    public void importVariableAlias(@NotNull Name aliasName, @NotNull VariableDescriptor variableDescriptor) {
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

            // make sure no descriptors added to allDescriptors collection
            changeLockLevel(LockLevel.READING);

            allDescriptors.addAll(getWorkerScope().getAllDescriptors());
            for (JetScope imported : getImports()) {
                allDescriptors.addAll(imported.getAllDescriptors());
            }
        }
        return allDescriptors;
    }

    @NotNull
    private Map<LabelName, List<DeclarationDescriptor>> getLabelsToDescriptors() {
        if (labelsToDescriptors == null) {
            labelsToDescriptors = new HashMap<LabelName, List<DeclarationDescriptor>>();
        }
        return labelsToDescriptors;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
        checkMayRead();

        Collection<DeclarationDescriptor> superResult = super.getDeclarationsByLabel(labelName);
        Map<LabelName, List<DeclarationDescriptor>> labelsToDescriptors = getLabelsToDescriptors();
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

        Map<LabelName, List<DeclarationDescriptor>> labelsToDescriptors = getLabelsToDescriptors();
        LabelName name = new LabelName(descriptor.getName().asString());
        List<DeclarationDescriptor> declarationDescriptors = labelsToDescriptors.get(name);
        if (declarationDescriptors == null) {
            declarationDescriptors = new ArrayList<DeclarationDescriptor>();
            labelsToDescriptors.put(name, declarationDescriptors);
        }
        declarationDescriptors.add(descriptor);
    }

    @NotNull
    private Map<Name, DeclarationDescriptor> getVariableOrClassDescriptors() {
        if (variableOrClassDescriptors == null) {
            variableOrClassDescriptors = Maps.newHashMap();
        }
        return variableOrClassDescriptors;
    }

    @NotNull
    private Map<Name, PackageViewDescriptor> getPackageAliases() {
        if (packageAliases == null) {
            packageAliases = Maps.newHashMap();
        }
        return packageAliases;
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

        Name name = variableDescriptor.getName();
        if (isProperty) {
            checkForPropertyRedeclaration(name, variableDescriptor);
            getPropertyGroups().put(name, variableDescriptor);
        }
        if (variableDescriptor.getReceiverParameter() == null) {
            checkForRedeclaration(name, variableDescriptor);
            // TODO : Should this always happen?
            getVariableOrClassDescriptors().put(name, variableDescriptor);
        }
        allDescriptors.add(variableDescriptor);
        addToDeclared(variableDescriptor);
    }

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        checkMayRead();

        Set<VariableDescriptor> result = Sets.newLinkedHashSet(getPropertyGroups().get(name));

        result.addAll(getWorkerScope().getProperties(name));

        result.addAll(super.getProperties(name));
        
        return result;
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        checkMayRead();

        Map<Name, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableOrClassDescriptors();
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
    private SetMultimap<Name, VariableDescriptor> getPropertyGroups() {
        if (propertyGroups == null) {
            propertyGroups = CommonSuppliers.newLinkedHashSetHashSetMultimap();
        }
        return propertyGroups;
    }
    
    @NotNull
    private SetMultimap<Name, FunctionDescriptor> getFunctionGroups() {
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
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        checkMayRead();

        Set<FunctionDescriptor> result = Sets.newLinkedHashSet(getFunctionGroups().get(name));

        result.addAll(getWorkerScope().getFunctions(name));

        result.addAll(super.getFunctions(name));

        return result;
    }

    @Override
    public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        checkMayWrite();

        Name name = typeParameterDescriptor.getName();
        addClassifierAlias(name, typeParameterDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        checkMayWrite();

        addClassifierAlias(classDescriptor.getName(), classDescriptor);
    }

    @Override
    public void addClassifierAlias(@NotNull Name name, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        checkForRedeclaration(name, classifierDescriptor);
        getVariableOrClassDescriptors().put(name, classifierDescriptor);
        allDescriptors.add(classifierDescriptor);
        addToDeclared(classifierDescriptor);
    }

    @Override
    public void addPackageAlias(@NotNull Name name, @NotNull PackageViewDescriptor packageView) {
        checkMayWrite();

        checkForRedeclaration(name, packageView);
        getPackageAliases().put(name, packageView);
        allDescriptors.add(packageView);
        addToDeclared(packageView);
    }

    @Override
    public void addFunctionAlias(@NotNull Name name, @NotNull FunctionDescriptor functionDescriptor) {
        checkMayWrite();
        
        checkForRedeclaration(name, functionDescriptor);
        getFunctionGroups().put(name, functionDescriptor);
        allDescriptors.add(functionDescriptor);
    }

    @Override
    public void addVariableAlias(@NotNull Name name, @NotNull VariableDescriptor variableDescriptor) {
        checkMayWrite();
        
        checkForRedeclaration(name, variableDescriptor);
        getVariableOrClassDescriptors().put(name, variableDescriptor);
        allDescriptors.add(variableDescriptor);
        addToDeclared(variableDescriptor);
    }
    
    private void checkForPropertyRedeclaration(@NotNull Name name, VariableDescriptor variableDescriptor) {
        Set<VariableDescriptor> properties = getPropertyGroups().get(name);
        ReceiverParameterDescriptor receiverParameter = variableDescriptor.getReceiverParameter();
        for (VariableDescriptor oldProperty : properties) {
            ReceiverParameterDescriptor receiverParameterForOldVariable = oldProperty.getReceiverParameter();
            if (((receiverParameter != null && receiverParameterForOldVariable != null) &&
                 (JetTypeChecker.INSTANCE.equalTypes(receiverParameter.getType(), receiverParameterForOldVariable.getType())))) {
                redeclarationHandler.handleRedeclaration(oldProperty, variableDescriptor);
            }
        }
    }

    private void checkForRedeclaration(@NotNull Name name, DeclarationDescriptor classifierDescriptor) {
        DeclarationDescriptor originalDescriptor = getVariableOrClassDescriptors().get(name);
        if (originalDescriptor != null) {
            redeclarationHandler.handleRedeclaration(originalDescriptor, classifierDescriptor);
        }
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        checkMayRead();

        Map<Name, DeclarationDescriptor> variableClassOrNamespaceDescriptors = getVariableOrClassDescriptors();
        DeclarationDescriptor descriptor = variableClassOrNamespaceDescriptors.get(name);
        if (descriptor instanceof ClassifierDescriptor) return (ClassifierDescriptor) descriptor;

        ClassifierDescriptor classifierDescriptor = getWorkerScope().getClassifier(name);
        if (classifierDescriptor != null) return classifierDescriptor;

        return super.getClassifier(name);
    }

    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        checkMayRead();

        PackageViewDescriptor aliased = getPackageAliases().get(name);
        if (aliased != null) return aliased;

        PackageViewDescriptor packageView = getWorkerScope().getPackage(name);
        if (packageView != null) return packageView;
        return super.getPackage(name);
    }

    @Override
    public void setImplicitReceiver(@NotNull ReceiverParameterDescriptor implicitReceiver) {
        checkMayWrite();

        if (this.implicitReceiver != null) {
            throw new UnsupportedOperationException("Receiver redeclared");
        }
        this.implicitReceiver = implicitReceiver;
    }

    @Override
    protected List<ReceiverParameterDescriptor> computeImplicitReceiversHierarchy() {
        List<ReceiverParameterDescriptor> implicitReceiverHierarchy = Lists.newArrayList();
        if (implicitReceiver != null) {
            implicitReceiverHierarchy.add(implicitReceiver);
        }
        implicitReceiverHierarchy.addAll(super.computeImplicitReceiversHierarchy());
        return implicitReceiverHierarchy;
    }

    private void addToDeclared(DeclarationDescriptor descriptor) {
        declaredDescriptorsAccessibleBySimpleName.put(descriptor.getName(), descriptor);
    }

    @NotNull
    @Override
    public Multimap<Name, DeclarationDescriptor> getDeclaredDescriptorsAccessibleBySimpleName() {
        return declaredDescriptorsAccessibleBySimpleName;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        return declaredDescriptorsAccessibleBySimpleName.values();
    }

    @TestOnly
    @Override
    protected void printAdditionalScopeStructure(@NotNull Printer p) {
        p.println("allDescriptorsDone = ", allDescriptorsDone);
    }
}
