// !LANGUAGE: +MultiPlatformProjects
// WITH_RUNTIME
// FILE: common.kt

expect class Foo() {
    fun member(a: String, b: Int = 0, c: Double? = null): String
    fun noDefaultOverrideInExpect(a: String, b: Int = 0, c: Double? = null): String
}

// FILE: jvm.kt

import kotlin.test.assertEquals

actual typealias Foo = Foo2

class Foo2 constructor() {
    fun member(a: String, b: Int = 0, c: Double? = null): String = a + "," + b + "," + c
    fun noDefaultOverrideInExpect(a: String, b: Int, c: Double?): String = a + "," + b + "," + c
}

fun box(): String {
    val foo = Foo()
    assertEquals("OK,0,null", foo.member("OK"))
    assertEquals("OK,42,null", foo.member("OK", 42))
    assertEquals("OK,42,3.14", foo.member("OK", 42, 3.14))

    assertEquals("OK,42,3.14", foo.noDefaultOverrideInExpect("OK", 42, 3.14))


    return "OK"
}
