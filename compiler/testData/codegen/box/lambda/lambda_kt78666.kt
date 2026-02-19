// WITH_STDLIB

import kotlin.test.*

fun <T> consume(l: () -> T): T = l()

val topLevel: String = consume { "top level property" }

fun topLevel(): String = consume { "top level function" }

class Foo {
    val classLevel: String = consume { "class level property" }

    fun classLevel(): String = consume { "class level function" }
}

fun box(): String {
    assertEquals("top level property", topLevel)
    assertEquals("top level function", topLevel())
    assertEquals("class level property", Foo().classLevel)
    assertEquals("class level function", Foo().classLevel())
    return "OK"
}
