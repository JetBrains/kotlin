// !LANGUAGE: +StrictJavaNullabilityAssertions +ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated +DefinitelyNonNullableTypes
// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM

// FILE: box.kt
fun box(): String {
    try {
        J().test()
        return "Fail: SHOULD throw exception"
    }
    catch (e: Throwable) {
        return "OK"
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
