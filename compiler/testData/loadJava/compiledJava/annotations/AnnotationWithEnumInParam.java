package test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public interface AnnotationWithEnumInParam {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface RetentionAnnotation {
        String value();
    }
}
