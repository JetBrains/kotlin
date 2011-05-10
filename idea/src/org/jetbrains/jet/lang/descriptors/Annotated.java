package org.jetbrains.jet.lang.descriptors;

import java.util.List;

/**
 * @author abreslav
 */
public interface Annotated {
    List<Annotation> getAnnotations();
}
