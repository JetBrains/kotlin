// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: Test.java

public class Test {

    protected String data = "O";

    protected Test() {

    }

    protected static String testStatic() {
        return "K";
    }

}

// FILE: test.kt

public inline fun test(): String {
    val p = object : Test() {}
    return p.data + Test.testStatic();
}


fun box(): String {
    return test()
}
