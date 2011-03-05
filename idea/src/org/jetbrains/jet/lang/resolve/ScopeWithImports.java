package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author abreslav
 */
public class ScopeWithImports implements JetScope {

    private final List<JetScope> imports = new ArrayList<JetScope>();
    private final JetScope scope;

    public ScopeWithImports(JetScope scope) {
        this.scope = scope;
    }

    public void importScope(JetScope imported) {
        imports.add(0, imported);
    }

    @Override
    public ClassDescriptor getClass(@NotNull String name) {
        ClassDescriptor descriptor = scope.getClass(name);
        if (descriptor != null) return descriptor;
        for (JetScope imported : imports) {
            ClassDescriptor importedClass = imported.getClass(name);
            if (importedClass != null) {
                return importedClass;
            }
        }
        return null;
    }

    @Override
    public PropertyDescriptor getProperty(@NotNull String name) {
        PropertyDescriptor descriptor = scope.getProperty(name);
        if (descriptor != null) return descriptor;
        for (JetScope imported : imports) {
            PropertyDescriptor importedDescriptor = imported.getProperty(name);
            if (importedDescriptor != null) {
                return importedDescriptor;
            }
        }
        return null;
    }

    @Override
    public ExtensionDescriptor getExtension(@NotNull String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        NamespaceDescriptor descriptor = scope.getNamespace(name);
        if (descriptor != null) return descriptor;
        for (JetScope imported : imports) {
            NamespaceDescriptor importedDescriptor = imported.getNamespace(name);
            if (importedDescriptor != null) {
                return importedDescriptor;
            }
        }
        return null;
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(@NotNull String name) {
        return scope.getTypeParameter(name);
    }

    @NotNull
    @Override
    public Type getThisType() {
        return scope.getThisType();
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return scope.getFunctionGroup(name); // TODO
    }
}
