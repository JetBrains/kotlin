// FILE: test/JavaClass.java

package test;

public class JavaClass {
    protected static int field;

    protected static String method() {
        return "";
    }
}

// FILE: test.kt

package test

fun test() {
    JavaClass.field
    JavaClass.method()
}
