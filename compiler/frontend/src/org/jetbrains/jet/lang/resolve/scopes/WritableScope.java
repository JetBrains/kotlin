package org.jetbrains.jet.lang.resolve.scopes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author abreslav
 */
public interface WritableScope extends JetScope {
    enum LockLevel {
        WRITING,
        BOTH,
        READING,
    }

    WritableScope changeLockLevel(LockLevel lockLevel);

    void addLabeledDeclaration(@NotNull DeclarationDescriptor descriptor);

    void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor);

    void addPropertyDescriptor(@NotNull VariableDescriptor propertyDescriptor);

    void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor);

    void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor);

    void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor);

    void addClassifierAlias(@NotNull String name, @NotNull ClassifierDescriptor classifierDescriptor);

    void addNamespaceAlias(@NotNull String name, @NotNull NamespaceDescriptor namespaceDescriptor);
    
    void addFunctionAlias(@NotNull String name, @NotNull FunctionDescriptor functionDescriptor);
    
    void addVariableAlias(@NotNull String name, @NotNull VariableDescriptor variableDescriptor);

    void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor);

    @Nullable
    NamespaceDescriptor getDeclaredNamespace(@NotNull String name);

    void importScope(@NotNull JetScope imported);

    void setImplicitReceiver(@NotNull ReceiverDescriptor implicitReceiver);

    void importClassifierAlias(@NotNull String importedClassifierName, @NotNull ClassifierDescriptor classifierDescriptor);

    void importNamespaceAlias(@NotNull String aliasName, @NotNull NamespaceDescriptor namespaceDescriptor);
    
    void importFunctionAlias(@NotNull String aliasName, @NotNull FunctionDescriptor functionDescriptor);
    
    void importVariableAlias(@NotNull String aliasName, @NotNull VariableDescriptor variableDescriptor);
}
