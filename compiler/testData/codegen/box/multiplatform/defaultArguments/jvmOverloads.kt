// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: J.java

public class J {
    public static String test() {
        JvmKt.foo(-1);
        JvmKt.foo(5, 5);
        return "OK";
    }
}

// FILE: common.kt

expect fun foo(j: Int, i: Int = -1)

// FILE: jvm.kt

import kotlin.test.assertEquals

@JvmOverloads
actual fun foo(j: Int, i: Int) {
    assertEquals(j, i)
}

fun box(): String {
    foo(-1)
    foo(5, 5)
    return J.test();
}
