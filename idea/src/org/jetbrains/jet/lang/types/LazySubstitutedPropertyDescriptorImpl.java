package org.jetbrains.jet.lang.types;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
public class LazySubstitutedPropertyDescriptorImpl implements PropertyDescriptor {
    private final PropertyDescriptor propertyDescriptor;
    private final Map<TypeConstructor, TypeProjection> substitutionContext;
    private Type propertyType = null;

    public LazySubstitutedPropertyDescriptorImpl(@NotNull PropertyDescriptor propertyDescriptor, @NotNull Map<TypeConstructor, TypeProjection> substitutionContext) {
        this.propertyDescriptor = propertyDescriptor;
        this.substitutionContext = substitutionContext;
    }

    @Override
    public Type getType() {
        if (propertyType == null) {
            propertyType = TypeSubstitutor.INSTANCE.substitute(substitutionContext, propertyDescriptor.getType(), Variance.OUT_VARIANCE);
        }
        return propertyType;
    }

    @Override
    public List<Attribute> getAttributes() {
        // TODO : Substitute, lazily
        return propertyDescriptor.getAttributes();
    }

    @Override
    public String getName() {
        return propertyDescriptor.getName();
    }
}
