package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.JetScopeImpl;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.NamespaceDescriptor;

/**
 * @author abreslav
 */
public class JavaLangScope extends JetScopeImpl {

    private final JavaSemanticServices semanticServices;

    public JavaLangScope(JavaSemanticServices semanticServices) {
        this.semanticServices = semanticServices;
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
        return "java.lang." + name;
    }
}
