// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM

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
