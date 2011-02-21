package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public interface JetScope {
    JetScope EMPTY = new JetScopeImpl() {};

    @Nullable
    ClassDescriptor getClass(String name);

    @Nullable
    PropertyDescriptor getProperty(String name);

    @Nullable
    ExtensionDescriptor getExtension(String name);

    @Nullable
    NamespaceDescriptor getNamespace(String name);

    @Nullable
    TypeParameterDescriptor getTypeParameter(String name);

    @NotNull
    Type getThisType();

    @NotNull
    OverloadDomain getOverloadDomain(@Nullable Type receiverType, @NotNull String referencedName);
}
