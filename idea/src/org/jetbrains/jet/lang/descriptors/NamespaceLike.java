package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author abreslav
 */
public interface NamespaceLike extends DeclarationDescriptor {
    @Nullable
    NamespaceDescriptor getNamespace(String name);

    void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor);

    void addClassifierDescriptor(MutableClassDescriptor classDescriptor);
}
