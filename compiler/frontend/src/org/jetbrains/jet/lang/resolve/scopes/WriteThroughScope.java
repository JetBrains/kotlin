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

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Set;

public class WriteThroughScope extends WritableScopeWithImports {
    private final WritableScope writableWorker;
    private Collection<DeclarationDescriptor> allDescriptors;

    public WriteThroughScope(@NotNull JetScope outerScope, @NotNull WritableScope scope,
            @NotNull RedeclarationHandler redeclarationHandler, @NotNull String debugName) {
        super(outerScope, redeclarationHandler, debugName);
        this.writableWorker = scope;
    }

    @Override
    @Nullable
    public PropertyDescriptor getPropertyByFieldReference(@NotNull Name fieldName) {
        checkMayRead();

        return writableWorker.getPropertyByFieldReference(fieldName);
    }

    @Override
    @NotNull
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        checkMayRead();

        return writableWorker.getDeclarationsByLabel(labelName);
    }

    @Override
    @NotNull
    public DeclarationDescriptor getContainingDeclaration() {
        checkMayRead();

        return writableWorker.getContainingDeclaration();
    }

    @Override
    @NotNull
    public Collection<FunctionDescriptor> getFunctions(@NotNull Name name) {
        checkMayRead();

        Set<FunctionDescriptor> result = Sets.newLinkedHashSet();

        result.addAll(writableWorker.getFunctions(name));

        result.addAll(getWorkerScope().getFunctions(name));

        result.addAll(super.getFunctions(name)); // Imports

        return result;
    }

    @Override
    @NotNull
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        checkMayRead();

        Set<VariableDescriptor> properties = Sets.newLinkedHashSet();
        properties.addAll(writableWorker.getProperties(name));
        properties.addAll(getWorkerScope().getProperties(name));
        properties.addAll(super.getProperties(name)); //imports
        return properties;
    }

    @Override
    @Nullable
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        checkMayRead();

        VariableDescriptor variable = writableWorker.getLocalVariable(name);
        if (variable != null) return variable;

        variable = getWorkerScope().getLocalVariable(name);
        if (variable != null) return variable;

        return super.getLocalVariable(name); // Imports
    }

    @Override
    @Nullable
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        checkMayRead();

        PackageViewDescriptor namespace = writableWorker.getPackage(name);
        if (namespace != null) return namespace;

        namespace = getWorkerScope().getPackage(name);
        if (namespace != null) return namespace;

        return super.getPackage(name); // Imports
    }

    @Override
    @Nullable
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        checkMayRead();

        ClassifierDescriptor classifier = writableWorker.getClassifier(name);
        if (classifier != null) return classifier;

        classifier = getWorkerScope().getClassifier(name);
        if (classifier != null) return classifier;

        return super.getClassifier(name); // Imports
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        checkMayRead();

        ClassDescriptor objectDescriptor = writableWorker.getObjectDescriptor(name);
        if (objectDescriptor != null) return objectDescriptor;

        objectDescriptor = getWorkerScope().getObjectDescriptor(name);
        if (objectDescriptor != null) return objectDescriptor;

        return super.getObjectDescriptor(name); // Imports
    }

    @NotNull
    @Override
    public Set<ClassDescriptor> getObjectDescriptors() {
        checkMayRead();
        Set<ClassDescriptor> objectDescriptors = Sets.newHashSet();

        objectDescriptors.addAll(super.getObjectDescriptors());
        objectDescriptors.addAll(getWorkerScope().getObjectDescriptors());
        objectDescriptors.addAll(writableWorker.getObjectDescriptors());
        return objectDescriptors;
    }

    @Override
    public void addLabeledDeclaration(@NotNull DeclarationDescriptor descriptor) {
        checkMayWrite();

        writableWorker.addLabeledDeclaration(descriptor); // TODO : review
    }

    @Override
    public void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor) {
        checkMayWrite();

        writableWorker.addVariableDescriptor(variableDescriptor);
    }

    @Override
    public void addPropertyDescriptor(@NotNull VariableDescriptor propertyDescriptor) {
        checkMayWrite();

        writableWorker.addPropertyDescriptor(propertyDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        checkMayWrite();

        writableWorker.addFunctionDescriptor(functionDescriptor);
    }

    @Override
    public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        checkMayWrite();

        writableWorker.addTypeParameterDescriptor(typeParameterDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        checkMayWrite();

        writableWorker.addClassifierDescriptor(classDescriptor);
    }

    @Override
    public void addObjectDescriptor(@NotNull ClassDescriptor objectDescriptor) {
        checkMayWrite();

        writableWorker.addObjectDescriptor(objectDescriptor);
    }

    @Override
    public void addClassifierAlias(@NotNull Name name, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        writableWorker.addClassifierAlias(name, classifierDescriptor);
    }

    @Override
    public void addPackageAlias(@NotNull Name name, @NotNull PackageViewDescriptor packageViewDescriptor) {
        checkMayWrite();

        writableWorker.addPackageAlias(name, packageViewDescriptor);
    }

    @Override
    public void addVariableAlias(@NotNull Name name, @NotNull VariableDescriptor variableDescriptor) {
        checkMayWrite();
        
        writableWorker.addVariableAlias(name, variableDescriptor);
    }

    @Override
    public void addFunctionAlias(@NotNull Name name, @NotNull FunctionDescriptor functionDescriptor) {
        checkMayWrite();

        writableWorker.addFunctionAlias(name, functionDescriptor);
    }

    @NotNull
    @Override
    public Multimap<Name, DeclarationDescriptor> getDeclaredDescriptorsAccessibleBySimpleName() {
        return writableWorker.getDeclaredDescriptorsAccessibleBySimpleName();
    }

    @Override
    public void importScope(@NotNull JetScope imported) {
        checkMayWrite();

        super.importScope(imported); //
    }

    @Override
    public void setImplicitReceiver(@NotNull ReceiverParameterDescriptor implicitReceiver) {
        checkMayWrite();

        writableWorker.setImplicitReceiver(implicitReceiver);
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        checkMayRead();

        if (allDescriptors == null) {
            allDescriptors = Lists.newArrayList();
            allDescriptors.addAll(writableWorker.getAllDescriptors());
            allDescriptors.addAll(getWorkerScope().getAllDescriptors());

            for (JetScope imported : getImports()) {
                allDescriptors.addAll(imported.getAllDescriptors());
            }
        }
        return allDescriptors;
    }

    @NotNull
    public JetScope getOuterScope() {
        checkMayRead();

        return getWorkerScope();
    }
}
