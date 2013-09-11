package test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

public interface ArrayOfStringInParam {

    public @interface MyAnnotation {
        String[] value();
    }

    @MyAnnotation({"a", "b", "c"})
    public class A {

    }
}
