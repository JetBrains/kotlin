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
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Set;

// Reads from:
// 1. Worker (a.k.a outer)
// 2. Imports

// Writes to: writable worker
public class WriteThroughScope extends WritableScopeWithImports {
    private final WritableScope writableWorker;
    private Collection<DeclarationDescriptor> allDescriptors;

    public WriteThroughScope(@NotNull JetScope outerScope, @NotNull WritableScope scope,
            @NotNull RedeclarationHandler redeclarationHandler, @NotNull String debugName) {
        super(outerScope, redeclarationHandler, debugName);
        this.writableWorker = scope;
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
        result.addAll(getWorkerScope().getFunctions(name));
        result.addAll(super.getFunctions(name)); // Imports
        return result;
    }

    @Override
    @NotNull
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        checkMayRead();

        Set<VariableDescriptor> properties = Sets.newLinkedHashSet();
        properties.addAll(getWorkerScope().getProperties(name));
        properties.addAll(super.getProperties(name)); //imports
        return properties;
    }

    @Override
    @Nullable
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        checkMayRead();

        VariableDescriptor variable = getWorkerScope().getLocalVariable(name);
        if (variable != null) return variable;

        return super.getLocalVariable(name); // Imports
    }

    @Override
    @Nullable
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        checkMayRead();

        PackageViewDescriptor aPackage = getWorkerScope().getPackage(name);
        if (aPackage != null) return aPackage;

        return super.getPackage(name); // Imports
    }

    @Override
    @Nullable
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        checkMayRead();

        ClassifierDescriptor classifier = getWorkerScope().getClassifier(name);
        if (classifier != null) return classifier;

        return super.getClassifier(name); // Imports
    }

    @Override
    public void addLabeledDeclaration(@NotNull DeclarationDescriptor descriptor) {
        checkMayWrite();

        writableWorker.addLabeledDeclaration(descriptor);
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
    public void addClassifierAlias(@NotNull Name name, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        writableWorker.addClassifierAlias(name, classifierDescriptor);
    }

    @Override
    public void addPackageAlias(@NotNull Name name, @NotNull PackageViewDescriptor packageView) {
        checkMayWrite();

        writableWorker.addPackageAlias(name, packageView);
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

        super.importScope(imported);
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
            allDescriptors.addAll(getWorkerScope().getAllDescriptors());

            for (JetScope imported : getImports()) {
                allDescriptors.addAll(imported.getAllDescriptors());
            }
        }
        return allDescriptors;
    }

    @TestOnly
    @Override
    protected void printAdditionalScopeStructure(@NotNull Printer p) {
        p.print("writableWorker = ");
        writableWorker.printScopeStructure(p.withholdIndentOnce());
    }
}
