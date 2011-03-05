package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScopeImpl;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.NamespaceDescriptor;

/**
 * @author abreslav
 */
public class JavaPackageScope extends JetScopeImpl {

    private final JavaSemanticServices semanticServices;
    private String packagePrefix;

    public JavaPackageScope(@NotNull String packageFQN, JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
        this.packagePrefix = packageFQN.isEmpty() ? "" : packageFQN + ".";
    }

    @Override
    public ClassDescriptor getClass(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveClass(getQualifiedName(name));
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return semanticServices.getDescriptorResolver().resolveNamespace(getQualifiedName(name));
    }

    private String getQualifiedName(String name) {
        return packagePrefix + name;
    }
}
