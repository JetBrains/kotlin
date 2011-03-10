package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptorImpl extends DeclarationDescriptorImpl implements PropertyDescriptor {
    private Type type;

    public PropertyDescriptorImpl(List<Attribute> attributes, String name, Type type) {
        super(attributes, name);
        this.type = type;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return visitor.visitPropertyDescriptor(this, data);
    }
}
