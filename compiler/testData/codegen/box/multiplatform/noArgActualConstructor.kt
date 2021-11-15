// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
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
