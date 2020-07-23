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

import kotlin.test.assertTrue

class C {
    @Anno
    fun f1() {}

    @Anno(Void::class)
    fun f2() {}
}

fun box(): String {
    assertTrue("\\[@Anno\\(value=void(\\.class)?\\)\\]".toRegex().matches(C::f1.annotations.toString()))
    assertTrue("\\[@Anno\\(value=(class )?java.lang.Void(\\.class)?\\)\\]".toRegex().matches(C::f2.annotations.toString()))
    return "OK"
}
