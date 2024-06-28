// ISSUE: KT-66762: under K1, there's FAIL 1: . Expected <Hello!\n>, actual <>
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM, NATIVE
// IGNORE_LIGHT_ANALYSIS
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
