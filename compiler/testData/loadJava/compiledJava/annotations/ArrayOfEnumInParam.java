package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public interface ArrayOfEnumInParam {

    @Target({ElementType.FIELD, ElementType.CONSTRUCTOR})
    public @interface targetAnnotation {
        String value();
    }
}
