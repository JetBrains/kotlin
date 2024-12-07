// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt

expect class Foo(a: String, b: Int = 0, c: Double? = null)

// MODULE: platform()()(common)
// FILE: platform.kt

import kotlin.test.assertEquals

actual class Foo actual constructor(a: String, b: Int, c: Double?) {
    val result: String = a + "," + b + "," + c
}

fun box(): String {
    assertEquals("OK,0,null", Foo("OK").result)
    assertEquals("OK,42,null", Foo("OK", 42).result)
    assertEquals("OK,42,3.14", Foo("OK", 42, 3.14).result)

    return "OK"
}
