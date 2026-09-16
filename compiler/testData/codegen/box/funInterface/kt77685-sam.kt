// DIAGNOSTICS: -IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE
// ^^^ Kotlin/JS partially forbids implementing suspend function interfaces and report this diagnostic
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
