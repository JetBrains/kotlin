// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_RUNTIME
// FILE: Test.java

public class Test {
    public static String invokeMethodWithPublicField() {
        C c = new C("OK");
        return c.foo;
    }
}

// FILE: simple.kt

class C(@JvmField val foo: String) {

}

fun box(): String {
    return Test.invokeMethodWithPublicField()
}
