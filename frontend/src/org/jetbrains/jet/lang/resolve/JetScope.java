package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface JetScope {
    JetScope EMPTY = new JetScopeImpl() {
        @NotNull
        @Override
        public DeclarationDescriptor getContainingDeclaration() {
            throw new UnsupportedOperationException(); // TODO
        }

        @Override
        public String toString() {
            return "EMPTY";
        }
    };

    @Nullable
    ClassifierDescriptor getClassifier(@NotNull String name);

    @Nullable
    NamespaceDescriptor getNamespace(@NotNull String name);

    @Nullable
    VariableDescriptor getVariable(@NotNull String name);

    @NotNull
    FunctionGroup getFunctionGroup(@NotNull String name);

    @NotNull
    JetType getThisType();

    @NotNull
    DeclarationDescriptor getContainingDeclaration();

    @NotNull
    Collection<DeclarationDescriptor> getDeclarationsByLabel(String labelName);

    /**
     * @param fieldName includes the "$"
     * @return the property declaring this field, if any
     */
    @Nullable
    PropertyDescriptor getPropertyByFieldReference(@NotNull String fieldName);

    @Nullable
    DeclarationDescriptor getDeclarationDescriptorForUnqualifiedThis();

    Collection<DeclarationDescriptor> getAllDescriptors();
}