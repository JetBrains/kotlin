package test;

import java.lang.annotation.*;
public class TypeParameterAnnotations {
    @Target(ElementType.TYPE_PARAMETER)
    public @interface A {
        String value() default "";
    }

    // Currently annotations on type parameters and arguments are not loaded from compiled code because of IDEA-153093
    // Once it will be fixed check if KT-11454 is ready to be resolved
    public interface G<@A T> {
        <@A("abc") R> void foo(R r);
    }
}
