package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class AnnotatedImpl implements Annotated {
    private final List<Attribute> attributes;

    public AnnotatedImpl(List<Attribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public List<Attribute> getAttributes() {
        return attributes;
    }
}
