package test;

import java.lang.annotation.*;

@Target(value=ElementType.TYPE)
public @interface AnnotationWithArguments {

    String name();

    String arg() default "default";

}
