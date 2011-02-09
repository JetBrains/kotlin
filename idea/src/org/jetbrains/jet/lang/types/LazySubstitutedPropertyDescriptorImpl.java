package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public class LazySubstitutedPropertyDescriptorImpl implements PropertyDescriptor {
    private final PropertyDescriptor propertyDescriptor;
    private final Type contextType;
    private Type propertyType = null;

    public LazySubstitutedPropertyDescriptorImpl(PropertyDescriptor propertyDescriptor, Type contextType) {
        this.propertyDescriptor = propertyDescriptor;
        this.contextType = contextType;
    }

    @Override
    public Type getType() {
        if (propertyType == null) {
            propertyType = JetTypeChecker.INSTANCE.substitute(contextType, propertyDescriptor.getType(), Variance.OUT_VARIANCE);
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
