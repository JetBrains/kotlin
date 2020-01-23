// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FILE: JavaClass.java

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class JavaClass {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Foo {
        int value();
    }

    @Foo(KotlinInterface.FOO_INT)
    public String test() throws NoSuchMethodException {
        return KotlinInterface.FOO_STRING +
               JavaClass.class.getMethod("test").getAnnotation(Foo.class).value();
    }
}

// FILE: KotlinInterface.kt

interface KotlinInterface {
    companion object {
        const val FOO_INT: Int = 10
        const  val FOO_STRING: String = "OK"
    }
}

fun box(): String {
    val test = JavaClass().test()
    return if (test == "OK10") "OK" else "fail : $test"
}
