package org.jetbrains.jet.lang.descriptors;

import org.jetbrains.annotations.NotNull;

/**
 * @author Nikolay Krasko
 */
public final class DescriptorsUtils {

    private DescriptorsUtils() {
    }

    public static String getFQName(@NotNull NamespaceDescriptor namespaceDescriptor) {

        NamespaceDescriptor tempNamespace = namespaceDescriptor;
        String fqn = tempNamespace.getName();

        while (tempNamespace.getContainingDeclaration() instanceof NamespaceDescriptor) {
            tempNamespace = (NamespaceDescriptor) tempNamespace.getContainingDeclaration();

            assert tempNamespace != null;
            fqn = tempNamespace.getName() + "." + fqn;
        }

        return fqn;
    }
    
    public static String getFQName(@NotNull NamedFunctionDescriptor functionDescriptor) {
        if (functionDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor) {
            final String namespaceFQN = getFQName((NamespaceDescriptor) functionDescriptor.getContainingDeclaration());
            return !namespaceFQN.isEmpty() ? namespaceFQN + "." + functionDescriptor.getName() : functionDescriptor.getName();
        }

        throw new IllegalArgumentException("Currently supported only for top level functions");
    }

    public static boolean isTopLevelFunction(@NotNull NamedFunctionDescriptor functionDescriptor) {
        return functionDescriptor.getContainingDeclaration() instanceof NamespaceDescriptor;
    }
}
