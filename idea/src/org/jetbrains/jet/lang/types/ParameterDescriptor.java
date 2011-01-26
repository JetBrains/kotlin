package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public class ParameterDescriptor extends  AnnotatedImpl {
    private final Type type;
    private final String name;

    public ParameterDescriptor(List<Attribute> attributes, Type type, String name) {
        super(attributes);
        this.type = type;
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

}
