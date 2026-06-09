// TARGET_BACKEND: JVM
// WITH_STDLIB

// FILE: test.kt
@file:OptIn(ExperimentalVersionOverloading::class)

fun foo(s: String, @IntroducedAt("2") b: Int = 0): String = s

fun box(): String {
    return Test().test("OK")
}

// FILE: Test.java

public class Test {
    public String test(String s) {
        return TestKt.foo(s);
    }
}
