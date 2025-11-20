// WITH_STDLIB

// FILE: lib.kt

fun interface Foo: suspend () -> Unit

// FILE: main.kt
import kotlin.test.*

val foo = Foo {}

fun box(): String {
    assertEquals(foo, foo) // Circumvent DCE.

    return "OK"
}
