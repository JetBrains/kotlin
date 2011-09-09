package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public interface WritableScope extends JetScope {
    void addLabeledDeclaration(@NotNull DeclarationDescriptor descriptor);

    void addVariableDescriptor(@NotNull VariableDescriptor variableDescriptor);

    void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor);

    void addTypeParameterDescriptor(@NotNull TypeParameterDescriptor typeParameterDescriptor);

    void addClassifierDescriptor(@NotNull ClassifierDescriptor classDescriptor);

    void addClassifierAlias(@NotNull String name, @NotNull ClassifierDescriptor classifierDescriptor);

    void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor);

    @Nullable
    NamespaceDescriptor getDeclaredNamespace(@NotNull String name);

    void addPropertyDescriptorByFieldName(@NotNull String fieldName, @NotNull PropertyDescriptor propertyDescriptor);

    void importScope(@NotNull JetScope imported);

    void setThisType(@NotNull JetType thisType);
}
