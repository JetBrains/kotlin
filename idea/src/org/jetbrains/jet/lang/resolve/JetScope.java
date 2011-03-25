package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

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
    };

    @Nullable
    ClassifierDescriptor getClassifier(@NotNull String name);

    @Nullable
    NamespaceDescriptor getNamespace(@NotNull String name);

    @Nullable
    PropertyDescriptor getProperty(@NotNull String name);

    @NotNull
    FunctionGroup getFunctionGroup(@NotNull String name);

    @NotNull
    JetType getThisType();

    @NotNull
    DeclarationDescriptor getContainingDeclaration();
}