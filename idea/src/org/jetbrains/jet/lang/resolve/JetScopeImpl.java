package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

/**
* @author abreslav
*/
public abstract class JetScopeImpl implements JetScope {
    @Override
    public ClassDescriptor getClass(@NotNull String name) {
        return null;
    }

    @Override
    public PropertyDescriptor getProperty(@NotNull String name) {
        return null;
    }

    @Override
    public ExtensionDescriptor getExtension(@NotNull String name) {
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return null;
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(@NotNull String name) {
        return null;
    }

    @NotNull
    @Override
    public Type getThisType() {
        return JetStandardClasses.getNothingType();
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return FunctionGroup.EMPTY;
    }
}
