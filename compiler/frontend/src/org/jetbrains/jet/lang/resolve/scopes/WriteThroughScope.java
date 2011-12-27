package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

import java.util.Collection;
import java.util.Set;

/**
 * @author abreslav
 */
public class WriteThroughScope extends WritableScopeWithImports {
    private final WritableScope writableWorker;
    private Collection<DeclarationDescriptor> allDescriptors;

    public WriteThroughScope(@NotNull JetScope outerScope, @NotNull WritableScope scope, @NotNull RedeclarationHandler redeclarationHandler) {
        super(outerScope, redeclarationHandler);
        this.writableWorker = scope;
    }

    @Override
    @Nullable
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        checkMayRead();

        return writableWorker.getPropertyByFieldReference(fieldName);
    }

    @Override
    @NotNull
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
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
    public ReceiverDescriptor getImplicitReceiver() {
        checkMayRead();

        return writableWorker.getImplicitReceiver();
    }

    @Override
    @NotNull
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
        checkMayRead();

        Set<FunctionDescriptor> result = Sets.newLinkedHashSet();

        result.addAll(writableWorker.getFunctions(name));

        result.addAll(getWorkerScope().getFunctions(name));

        result.addAll(super.getFunctions(name)); // Imports

        return result;
    }

    @Override
    @NotNull
    public Set<VariableDescriptor> getProperties(@NotNull String name) {
        checkMayRead();

        Set<VariableDescriptor> properties = Sets.newLinkedHashSet();
        properties.addAll(writableWorker.getProperties(name));
        properties.addAll(getWorkerScope().getProperties(name));
        properties.addAll(super.getProperties(name)); //imports
        return properties;
    }

    @Override
    @Nullable
    public VariableDescriptor getLocalVariable(@NotNull String name) {
        checkMayRead();

        VariableDescriptor variable = writableWorker.getLocalVariable(name);
        if (variable != null) return variable;

        variable = getWorkerScope().getLocalVariable(name);
        if (variable != null) return variable;

        return super.getLocalVariable(name); // Imports
    }

    @Override
    @Nullable
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        checkMayRead();

        NamespaceDescriptor namespace = writableWorker.getNamespace(name);
        if (namespace != null) return namespace;

        namespace = getWorkerScope().getNamespace(name);
        if (namespace != null) return namespace;

        return super.getNamespace(name); // Imports
    }

    @Override
    @Nullable
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        checkMayRead();

        ClassifierDescriptor classifier = writableWorker.getClassifier(name);
        if (classifier != null) return classifier;

        classifier = getWorkerScope().getClassifier(name);
        if (classifier != null) return classifier;

        return super.getClassifier(name); // Imports
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
    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {
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
    public void addClassifierAlias(@NotNull String name, @NotNull ClassifierDescriptor classifierDescriptor) {
        checkMayWrite();

        writableWorker.addClassifierAlias(name, classifierDescriptor);
    }

    @Override
    public void addNamespaceAlias(@NotNull String name, @NotNull NamespaceDescriptor namespaceDescriptor) {
        checkMayWrite();

        writableWorker.addNamespaceAlias(name, namespaceDescriptor);
    }

    @Override
    public void addVariableAlias(@NotNull String name, @NotNull VariableDescriptor variableDescriptor) {
        checkMayWrite();
        
        writableWorker.addVariableAlias(name, variableDescriptor);
    }

    @Override
    public void addFunctionAlias(@NotNull String name, @NotNull FunctionDescriptor functionDescriptor) {
        checkMayWrite();

        writableWorker.addFunctionAlias(name, functionDescriptor);
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        checkMayWrite();

        writableWorker.addNamespace(namespaceDescriptor);
    }

    @Override
    @Nullable
    public NamespaceDescriptor getDeclaredNamespace(@NotNull String name) {
        checkMayRead();

        return writableWorker.getDeclaredNamespace(name);
    }

    @Override
    public void importScope(@NotNull JetScope imported) {
        checkMayWrite();

        super.importScope(imported); //
    }

    @Override
    public void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver) {
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
