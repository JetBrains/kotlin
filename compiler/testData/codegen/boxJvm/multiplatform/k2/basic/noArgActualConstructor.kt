// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect class Foo(a: String = "", b: Int = 42)

// MODULE: jvm()()(common)
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
