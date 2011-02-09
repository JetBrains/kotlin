package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class NamedAnnotatedImpl extends AnnotatedImpl implements Named {

    private final String name;

    public NamedAnnotatedImpl(List<Attribute> attributes, String name) {
        super(attributes);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
