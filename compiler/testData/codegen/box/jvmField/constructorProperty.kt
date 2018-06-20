// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
