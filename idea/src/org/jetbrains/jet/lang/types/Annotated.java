package org.jetbrains.jet.lang.types;

import java.util.List;

/**
 * @author abreslav
 */
public interface Annotated {
    List<Annotation> getAnnotations();
}
