package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;

import java.util.*;

/**
 * @author alex.tkachman
 *
 * Utility class used by back-end to
 */
public class ProjectionErasingJetType implements JetType {
    private final JetType delegate;
    private List<TypeProjection> arguments;

    public ProjectionErasingJetType(JetType delegate) {
        this.delegate = delegate;
        arguments = new ArrayList<TypeProjection>();
        for(TypeProjection tp : delegate.getArguments()) {
            arguments.add(new TypeProjection(Variance.INVARIANT, tp.getType()));
        }
    }

    @NotNull
    @Override
    public TypeConstructor getConstructor() {
        return delegate.getConstructor();
    }

    @NotNull
    @Override
    public List<TypeProjection> getArguments() {
        return arguments;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @NotNull
    @Override
    public JetScope getMemberScope() {
        return delegate.getMemberScope();
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return delegate.getAnnotations();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return ((obj instanceof ProjectionErasingJetType) && delegate.equals(((ProjectionErasingJetType)obj).delegate)) || delegate.equals(obj);
    }
}
