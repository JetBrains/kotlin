// WITH_REFLECT
// TARGET_BACKEND: JVM
// FILE: Anno.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Anno {
    Class<?> value() default void.class;
}

// FILE: test.kt

import kotlin.test.assertEquals

class C {
    @Anno
    fun f1() {}

    @Anno(Void::class)
    fun f2() {}
}

fun box(): String {
    assertEquals("[@Anno(value=void)]", C::f1.annotations.toString())
    assertEquals("[@Anno(value=class java.lang.Void)]", C::f2.annotations.toString())
    return "OK"
}
