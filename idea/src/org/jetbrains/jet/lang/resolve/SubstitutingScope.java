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
    public PropertyDescriptor getProperty(String name) {
        PropertyDescriptor property = workerScope.getProperty(name);
        if (property == null) {
            return null;
        }
        return new LazySubstitutedPropertyDescriptorImpl(property, substitutionContext);
    }

    @Override
    public ClassDescriptor getClass(String name) {
        ClassDescriptor descriptor = workerScope.getClass(name);
        if (descriptor == null) {
            return null;
        }
        return new LazySubstitutingClassDescriptor(descriptor, substitutionContext);
    }

    @Override
    public ExtensionDescriptor getExtension(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public NamespaceDescriptor getNamespace(String name) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public TypeParameterDescriptor getTypeParameter(String name) {
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
