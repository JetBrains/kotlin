package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class TypeParameterDescriptor extends DeclarationDescriptorImpl {
    private final Variance variance;
    private final Set<Type> upperBounds;
    private final TypeConstructor typeConstructor;

    public TypeParameterDescriptor(@NotNull DeclarationDescriptor containingDeclaration, List<Attribute> attributes, Variance variance, String name, Set<Type> upperBounds) {
        super(containingDeclaration, attributes, name);
        this.variance = variance;
        this.upperBounds = upperBounds;
        // TODO: Should we actually pass the attributes on to the type constructor?
        this.typeConstructor = new TypeConstructor(
                attributes,
                false,
                "&" + name,
                Collections.<TypeParameterDescriptor>emptyList(),
                upperBounds);
    }

    public TypeParameterDescriptor(@NotNull DeclarationDescriptor containingDeclaration, List<Attribute> attributes, Variance variance, String name) {
        this(containingDeclaration, attributes, variance, name, Collections.singleton(JetStandardClasses.getNullableAnyType()));
    }

    public Variance getVariance() {
        return variance;
    }

    public Set<Type> getUpperBounds() {
        return upperBounds;
    }

    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public String toString() {
        return typeConstructor.toString();
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitTypeParameterDescriptor(this, data);
    }
}
