package org.jetbrains.jet.lang.descriptors;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class AnnotatedImpl implements Annotated {
    private final List<Annotation> annotations;

    public AnnotatedImpl(List<Annotation> annotations) {
        this.annotations = annotations;
    }

    @Override
    public List<Annotation> getAnnotations() {
        return annotations;
    }
}
