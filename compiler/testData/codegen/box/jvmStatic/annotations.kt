// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// FILE: Test.java

import java.lang.annotation.Annotation;

class Test {

    public static String test1() throws NoSuchMethodException {
        Annotation[] test1s = A.class.getMethod("test1").getAnnotations();
        for (Annotation test : test1s) {
            String name = test.toString();
            if (name.contains("testAnnotation")) {
                return "OK";
            }
        }
        return "fail";
    }

    public static String test2() throws NoSuchMethodException {
        Annotation[] test2s = B.class.getMethod("test1").getAnnotations();
        for (Annotation test : test2s) {
            String name = test.toString();
            if (name.contains("testAnnotation")) {
                return "OK";
            }
        }
        return "fail";
    }

}

// FILE: test.kt

@Retention(AnnotationRetention.RUNTIME)
annotation class testAnnotation

class A {

    companion object {
        val b: String = "OK"

        @JvmStatic @testAnnotation fun test1() = b
    }
}

object B {
    val b: String = "OK"

    @JvmStatic @testAnnotation fun test1() = b
}

fun box(): String {
    if (Test.test1() != "OK") return "fail 1"

    if (Test.test2() != "OK") return "fail 2"

    return "OK"
}
