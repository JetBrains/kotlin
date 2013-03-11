package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public interface AnnotationWithArrayOfEnumInParam {

    @Target({ElementType.FIELD, ElementType.CONSTRUCTOR})
    public @interface targetAnnotation {
        String value();
    }
}
