// ISSUE: KT-66762
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: ANY:1.9
// WITH_STDLIB
import kotlin.test.*

val sb = StringBuilder()

object O {
    const val C = 1
}

fun foo() = O.also { sb.appendLine("Hello!") }

val X = foo().C

fun box(): String {
    assertEquals("Hello!\n", sb.toString(), "FAIL 1: $sb")
    assertEquals(1, X)
    assertEquals("Hello!\n", sb.toString(), "FAIL 2: $sb")
    val Y = foo().C
    assertEquals(1, Y)
    assertEquals("Hello!\nHello!\n", sb.toString(), "FAIL 3: $sb")

    return "OK"
}
