// LANGUAGE: +StrictJavaNullabilityAssertions -ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// FIR status: expected: <OK> but was: <Fail: SHOULD NOT throw>, issue related to T & Any
// See KT-8135
// We could generate runtime assertion on call site for 'generic<NOT_NULL_TYPE>()' below.

// FILE: box.kt
fun box(): String {
    try {
        J().test()
        return "OK"
    }
    catch (e: Throwable) {
        return "Fail: SHOULD NOT throw"
    }
}

// FILE: test.kt
fun withAssertion(j: J) = generic<String?>(j)

fun <T> generic(j: J) = j.nullT<T>()

// FILE: J.java
import org.jetbrains.annotations.NotNull;

public class J {
    @NotNull
    public <T> T nullT() {
        return null;
    }

    public void test() {
        TestKt.withAssertion(this);
    }
}
