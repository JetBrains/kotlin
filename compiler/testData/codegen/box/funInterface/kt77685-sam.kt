// WITH_STDLIB

// FILE: lib.kt

fun interface Foo: suspend () -> Unit

// FILE: main.kt
import kotlin.test.*

val x = suspend {}
val foo = Foo(x)

fun box(): String {
    assertEquals(foo, foo) // Circumvent DCE.

    return "OK"
}
