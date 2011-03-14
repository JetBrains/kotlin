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
    ClassDescriptor getClass(@NotNull String name);

    @Nullable
    PropertyDescriptor getProperty(@NotNull String name);

    @Nullable
    ExtensionDescriptor getExtension(@NotNull String name);

    @Nullable
    NamespaceDescriptor getNamespace(@NotNull String name);

    @Nullable
    TypeParameterDescriptor getTypeParameter(@NotNull String name);

    @NotNull
    Type getThisType();

    @NotNull
    FunctionGroup getFunctionGroup(@NotNull String name);

    @NotNull
    DeclarationDescriptor getContainingDeclaration();
}
