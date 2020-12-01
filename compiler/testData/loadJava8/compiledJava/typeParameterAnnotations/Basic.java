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

    public interface G1<T, E extends T, @A X> {
        <R, @A("abc") _A extends R> void foo(R r);
    }

    <R, @A("abc") _A extends R, @A("abc") K> void foo(R r) {

    }
}
