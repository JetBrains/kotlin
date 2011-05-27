package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;

/**
 * @author abreslav
 */
public class WriteThroughScope extends WritableScopeWithImports {
    private final WritableScope writableWorker;

    public WriteThroughScope(@NotNull JetScope outerScope, @NotNull WritableScope scope, @NotNull ErrorHandler errorHandler) {
        super(outerScope, errorHandler);
        this.writableWorker = scope;
    }

    @Override
    @Nullable
    public PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName) {
        return writableWorker.getPropertyByFieldReference(fieldName);
    }

    @Override
    @NotNull
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName) {
        return writableWorker.getDeclarationsByLabel(labelName);
    }

    @Override
    @NotNull
    public DeclarationDescriptor getContainingDeclaration() {
        return writableWorker.getContainingDeclaration();
    }

    @Override
    @NotNull
    public JetType getThisType() {
        return writableWorker.getThisType();
    }

    @Override
    @NotNull
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        FunctionGroup functionGroup = writableWorker.getFunctionGroup(name);
        if (!functionGroup.isEmpty()) return functionGroup; // TODO : Overloads from different places

        functionGroup = getWorkerScope().getFunctionGroup(name);
        if (!functionGroup.isEmpty()) return functionGroup; // TODO : Overloads from different places

        return super.getFunctionGroup(name); // Imports
    }

    @Override
    @Nullable
    public VariableDescriptor getVariable(@NotNull String name) {
        VariableDescriptor variable = writableWorker.getVariable(name);
        if (variable != null) return variable;

        variable = getWorkerScope().getVariable(name);
        if (variable != null) return variable;

        return super.getVariable(name); // Imports
    }

    @Override
    @Nullable
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        NamespaceDescriptor namespace = writableWorker.getNamespace(name);
        if (namespace != null) return namespace;

        namespace = getWorkerScope().getNamespace(name);
        if (namespace != null) return namespace;

        return super.getNamespace(name); // Imports
    }

    @Override
    @Nullable
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        ClassifierDescriptor classifier = writableWorker.getClassifier(name);
        if (classifier != null) return classifier;

        classifier = getWorkerScope().getClassifier(name);
        if (classifier != null) return classifier;

        return super.getClassifier(name); // Imports
    }

    @Override
    public void addLabeledDeclaration(@NotNull DeclarationDescriptor descriptor) {
        writableWorker.addLabeledDeclaration(descriptor); // TODO : review
    }

    @Override
    public void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor) {
        writableWorker.addVariableDescriptor(variableDescriptor);
    }

    @Override
    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
        writableWorker.addFunctionDescriptor(functionDescriptor);
    }

    @Override
    public void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor) {
        writableWorker.addTypeParameterDescriptor(typeParameterDescriptor);
    }

    @Override
    public void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        writableWorker.addClassifierDescriptor(classDescriptor);
    }

    @Override
    public void addClassifierAlias(@NotNull String name, @NotNull ClassifierDescriptor classifierDescriptor) {
        writableWorker.addClassifierAlias(name, classifierDescriptor);
    }

    @Override
    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
        writableWorker.addNamespace(namespaceDescriptor);
    }

    @Override
    @Nullable
    public NamespaceDescriptor getDeclaredNamespace(@NotNull String name) {
        return writableWorker.getDeclaredNamespace(name);
    }

    @Override
    public void addPropertyDescriptorByFieldName(@NotNull String fieldName, @NotNull PropertyDescriptor propertyDescriptor) {
        writableWorker.addPropertyDescriptorByFieldName(fieldName, propertyDescriptor);
    }

    @Override
    public void importScope(@NotNull JetScope imported) {
        super.importScope(imported); //
    }

    @Override
    public void setThisType(@NotNull JetType thisType) {
        writableWorker.setThisType(thisType);
    }
}
