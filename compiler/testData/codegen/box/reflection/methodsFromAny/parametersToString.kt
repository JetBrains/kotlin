// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.test.*

fun Int.foo(s: String) {}

class A {
    fun bar() {}
}

fun baz(name: String) {}

fun box(): String {
    assertEquals(
            listOf("extension receiver parameter of ${Int::foo}", "parameter #1 s of ${Int::foo}"),
            Int::foo.parameters.map(Any::toString)
    )

    assertEquals(
            listOf("instance parameter of ${A::bar}"),
            A::bar.parameters.map(Any::toString)
    )

    assertEquals(
            listOf("parameter #0 name of ${::baz}"),
            ::baz.parameters.map(Any::toString)
    )

    return "OK"
}
