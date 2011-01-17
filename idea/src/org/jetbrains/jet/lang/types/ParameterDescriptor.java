package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public class ParameterDescriptor extends  AnnotatedImpl {
    private final Type type;
    private final String name;

    public ParameterDescriptor(List<Annotation> annotations, Type type, String name) {
        super(annotations);
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
