package org.jetbrains.jet.lang.descriptors.annotations;

import java.util.List;

/**
 * @author abreslav
 */
public abstract class AnnotatedImpl implements Annotated {
    private final List<AnnotationDescriptor> annotations;

    public AnnotatedImpl(List<AnnotationDescriptor> annotations) {
        this.annotations = annotations;
    }

    @Override
    public List<AnnotationDescriptor> getAnnotations() {
        return annotations;
    }
}
