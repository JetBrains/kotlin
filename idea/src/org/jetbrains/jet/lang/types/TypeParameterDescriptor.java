package org.jetbrains.jet.lang.types;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeParameterDescriptor extends NamedAnnotatedImpl {
    private final Variance variance;
    private final Collection<Type> upperBounds;
    private final TypeConstructor typeConstructor;

    public TypeParameterDescriptor(List<Attribute> attributes, Variance variance, String name, Collection<Type> upperBounds) {
        super(attributes, name);
        this.variance = variance;
        this.upperBounds = upperBounds;
        // TODO: Should we actually pass the attributes on to the type constructor?
        this.typeConstructor = new TypeConstructor(
                attributes,
                "&" + name,
                Collections.<TypeParameterDescriptor>emptyList(),
                upperBounds);
    }

    public Variance getVariance() {
        return variance;
    }

    public Collection<Type> getUpperBounds() {
        return upperBounds;
    }

    public TypeConstructor getTypeConstructor() {
        return typeConstructor;
    }

    @Override
    public String toString() {
        return typeConstructor.toString();
    }
}
