// !LANGUAGE: +MultiPlatformProjects
// WITH_RUNTIME
// FILE: common.kt

open class A() {
    fun member(a: String, b: Int = 0, c: Double? = null): String = a + "," + b + "," + c
}

expect class B() : A

// FILE: jvm.kt

import kotlin.test.assertEquals

actual class B actual constructor() : A()

fun box(): String {
    val b = B()
    assertEquals("OK,0,null", b.member("OK"))
    assertEquals("OK,42,null", b.member("OK", 42))
    assertEquals("OK,42,3.14", b.member("OK", 42, 3.14))

    return "OK"
}
