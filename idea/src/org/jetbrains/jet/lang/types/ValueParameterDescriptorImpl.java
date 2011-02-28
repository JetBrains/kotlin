package org.jetbrains.jet.lang.types;

import org.jetbrains.jet.lang.psi.JetParameter;

import java.util.List;

/**
 * @author abreslav
 */
public class ValueParameterDescriptorImpl extends PropertyDescriptorImpl implements ValueParameterDescriptor {
    private final boolean hasDefaultValue;
    private final boolean isVararg;

    public ValueParameterDescriptorImpl(JetParameter psiElement, List<Attribute> attributes, String name, Type type, boolean hasDefaultValue, boolean isVararg) {
        super(psiElement, attributes, name, type);
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
