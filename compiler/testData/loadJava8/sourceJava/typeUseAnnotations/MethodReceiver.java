// JAVAC_EXPECTED_FILE

package test;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@interface A {
    String value() default "";
}

/*
 * Note that a receiver type doesn't get into signatures used by the Kotlin compiler
 * So in this test, annotated types shouldn't be reflected in the signatures dump
 */

public class MethodReceiver<T> {
    public void f1(MethodReceiver<@A T> this) { }

    class MethodReceiver3<T, K, L> {
        public void f1(@A MethodReceiver3<@A T, K, @A L> this) { }
    }
}