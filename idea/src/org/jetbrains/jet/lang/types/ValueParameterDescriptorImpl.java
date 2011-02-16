package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public class ValueParameterDescriptorImpl extends PropertyDescriptorImpl implements ValueParameterDescriptor {
    private final boolean hasDefaultValue;

    public ValueParameterDescriptorImpl(List<Attribute> attributes, String name, Type type, boolean hasDefaultValue) {
        super(attributes, name, type);
        this.hasDefaultValue = hasDefaultValue;
    }

    @Override
    public boolean hasDefaultValue() {
        return hasDefaultValue;
    }

    @Override
    public boolean isRef() {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public boolean isVararg() {
        throw new UnsupportedOperationException(); // TODO
    }
}
