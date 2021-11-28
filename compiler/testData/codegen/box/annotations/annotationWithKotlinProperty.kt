// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: JavaClass.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class JavaClass {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Foo {
        int value();
    }

    @Foo(KotlinClass.FOO_INT)
    public String test() throws NoSuchMethodException {
        return KotlinClass.FOO_STRING +
               JavaClass.class.getMethod("test").getAnnotation(Foo.class).value();
    }
}

// FILE: kotlinClass.kt

class KotlinClass {
    companion object {
        const val FOO_INT: Int = 10
        @JvmField val FOO_STRING: String = "OK"
    }
}

fun box(): String {
    val test = JavaClass().test()
    return if (test == "OK10") "OK" else "fail : $test"
}
