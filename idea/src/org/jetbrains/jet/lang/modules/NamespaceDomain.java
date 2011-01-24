package org.jetbrains.jet.lang.modules;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.types.NamespaceDescriptor;

/**
 * @author abreslav
 */
public interface NamespaceDomain {
    @Nullable
    NamespaceDescriptor getNamespace(String namespaceName);
}
