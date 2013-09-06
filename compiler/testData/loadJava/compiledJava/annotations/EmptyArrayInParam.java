package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public interface EmptyArrayInParam {

    public @interface MyAnnotation {
        String[] value();
    }

    @MyAnnotation({})
    public class A {

    }
}
