package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public class PropertyDescriptor extends MemberDescriptorImpl {
    private Type type;

    public PropertyDescriptor(List<Attribute> attributes, String name, Type type) {
        super(attributes, name);
        this.type = type;
    }

    public PropertyDescriptor(List<Attribute> attributes, String name) {
        this(attributes, name, null);
    }

    public Type getType() {
        return type;
    }
}
