// TARGET_BACKEND: JVM

// WITH_STDLIB
// FILE: Test.java

public class Test {
    public static String invokeMethodWithPublicField() {
        C c = new C();
        return c.foo;
    }
}

// FILE: simple.kt

class C {
    @JvmField public val foo: String = "OK"
}

fun box(): String {
    return Test.invokeMethodWithPublicField()
}
