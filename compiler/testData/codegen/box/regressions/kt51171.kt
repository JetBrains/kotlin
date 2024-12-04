// TARGET_BACKEND: JVM

// FILE: Error.java

public class Error {
    static String foo() {
        return "OK";
    }
}

// FILE: test.kt

fun box() = Error.foo()
