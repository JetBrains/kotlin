// LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect fun foo(j: Int, i: Int = -1)

// MODULE: main()()(common)
// FILE: J.java

public class J {
    public static String test() {
        JvmKt.foo(-1);
        JvmKt.foo(5, 5);
        return "OK";
    }
}

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
