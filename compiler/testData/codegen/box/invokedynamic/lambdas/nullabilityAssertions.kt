// TARGET_BACKEND: JVM
// JVM_TARGET: 1.8
// LAMBDAS: INDY
// WITH_RUNTIME
// FILE: nullabilityAssertions.kt
fun box(): String {
    val fn: (String) -> String = { it }
    try {
        J.test(fn)
    } catch (e: NullPointerException) {
        return "OK"
    }

    return "Should throw NullPointerException"
}

// FILE: J.java
import kotlin.jvm.functions.Function1;

public class J {
    public static void test(Function1<String, String> fn) {
        fn.invoke(null);
    }
}
