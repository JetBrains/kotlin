package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;

import java.util.Map;

/**
 * @author abreslav
 */
public class SubstitutingScope implements JetScope {

    private final JetScope workerScope;
    private final Map<TypeConstructor, TypeProjection> substitutionContext;

    public SubstitutingScope(JetScope workerScope, Map<TypeConstructor, TypeProjection> substitutionContext) {
        this.workerScope = workerScope;
        this.substitutionContext = substitutionContext;
    }

    @Override
    public PropertyDescriptor getProperty(@NotNull String name) {
        PropertyDescriptor property = workerScope.getProperty(name);
        if (property == null || substitutionContext.isEmpty()) {
            return property;
        }
        return new LazySubstitutedPropertyDescriptorImpl(property, substitutionContext);
    }

    @Override
    public ClassDescriptor getClass(@NotNull String name) {
        ClassDescriptor descriptor = workerScope.getClass(name);
        if (descriptor == null) {
            return null;
        }
        return new LazySubstitutingClassDescriptor(descriptor, substitutionContext);
    }

    @Override
    public ExtensionDescriptor getExtension(@NotNull String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(@NotNull String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public Type getThisType() {
        throw new UnsupportedOperationException(); // TODO
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        return new LazySubstitutingFunctionGroup(substitutionContext, workerScope.getFunctionGroup(name));
    }
}
