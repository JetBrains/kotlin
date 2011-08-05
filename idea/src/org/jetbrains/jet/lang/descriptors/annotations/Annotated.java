package org.jetbrains.jet.lang.descriptors.annotations;

import java.util.List;

/**
 * @author abreslav
 */
public interface Annotated {
    List<AnnotationDescriptor> getAnnotations();
}
