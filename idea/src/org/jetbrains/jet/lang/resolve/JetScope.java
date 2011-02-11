package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface JetScope {
    JetScope EMPTY = new JetScopeImpl() {};

    @NotNull
    Collection<MethodDescriptor> getMethods(String name);

    @Nullable
    ClassDescriptor getClass(String name);

    @Nullable
    PropertyDescriptor getProperty(String name);

    @Nullable
    ExtensionDescriptor getExtension(String name);

    @Nullable
    NamespaceDescriptor getNamespace(String name);

    @Nullable
    TypeParameterDescriptor getTypeParameterDescriptor(String name);

    @NotNull
    Type getThisType();
}
