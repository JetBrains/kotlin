// JAVAC_EXPECTED_FILE
package test;

import java.lang.annotation.*;
public class Basic {
    @Target(ElementType.TYPE_PARAMETER)
    public @interface A {
        String value() default "";
    }

    public interface G<@A T> {
        <@A("abc") R> void foo(R r);
    }
}
