// LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_K2: JVM_IR, JS_IR
// FIR status: expect/actual in the same module (ACTUAL_WITHOUT_EXPECT)
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: common.kt

expect class Foo(a: String = "", b: Int = 42)

// FILE: J.java

public class J {
    public static void test() {
        new Foo();
        new Foo("", 42);
    }
}

// FILE: jvm.kt

import kotlin.test.assertEquals

actual class Foo actual constructor(a: String, b: Int) {
    init {
        assertEquals("", a)
        assertEquals(42, b)
    }
}

fun box(): String {
    J.test()
    return "OK"
}
