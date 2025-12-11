// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.2
// ^^^ KT-77685 is fixed in 2.2.10-RC
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
