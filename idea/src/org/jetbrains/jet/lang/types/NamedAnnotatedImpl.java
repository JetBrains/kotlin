package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class NamedAnnotatedImpl extends AnnotatedImpl {

    private final String name;

    public NamedAnnotatedImpl(List<Attribute> attributes, String name) {
        super(attributes);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
