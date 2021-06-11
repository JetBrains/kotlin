// TARGET_BACKEND: JVM
// FILE: test.kt

fun test(): String {
    return A()
        // Large comment that would cause the message for
        // the non-null assertion to be very large if
        // it is included verbatim in the message.
        .foo()
}

fun box(): String {
    try {
        test()
    } catch(e: Throwable) {
        return if (e.message!!.length <= " must not be null".length + 50) {
            "OK"
        } else {
            "FAIL1"
        }
    }
    return "FAIL2"
}

// FILE: A.java

public class A {
    public String foo() {
        return null;
    }
}
