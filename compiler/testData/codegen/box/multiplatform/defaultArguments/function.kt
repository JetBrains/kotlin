// !LANGUAGE: +MultiPlatformProjects
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: common.kt

expect fun topLevel(a: String, b: Int = 0, c: Double? = null): String

expect class Foo() {
    fun member(a: String, b: Int = 0, c: Double? = null): String
}

// FILE: jvm.kt

import kotlin.test.assertEquals

actual fun topLevel(a: String, b: Int, c: Double?): String = a + "," + b + "," + c

actual class Foo actual constructor() {
    actual fun member(a: String, b: Int, c: Double?): String = a + "," + b + "," + c
}

fun box(): String {
    assertEquals("OK,0,null", topLevel("OK"))
    assertEquals("OK,42,null", topLevel("OK", 42))
    assertEquals("OK,42,3.14", topLevel("OK", 42, 3.14))

    val foo = Foo()
    assertEquals("OK,0,null", foo.member("OK"))
    assertEquals("OK,42,null", foo.member("OK", 42))
    assertEquals("OK,42,3.14", foo.member("OK", 42, 3.14))

    return "OK"
}
