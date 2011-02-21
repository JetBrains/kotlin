package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public class ValueParameterDescriptorImpl extends PropertyDescriptorImpl implements ValueParameterDescriptor {
    private final boolean hasDefaultValue;
    private final boolean isVararg;

    public ValueParameterDescriptorImpl(List<Attribute> attributes, String name, Type type, boolean hasDefaultValue, boolean isVararg) {
        super(attributes, name, type);
        this.hasDefaultValue = hasDefaultValue;
        this.isVararg = isVararg;
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
        return isVararg;
    }
}
