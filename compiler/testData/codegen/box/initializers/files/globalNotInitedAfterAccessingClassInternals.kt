// WITH_STDLIB

// FILE: lib.kt
var z = false

// FILE: lib2.kt
import kotlin.test.*

val x = foo()

private fun foo(): Int {
    z = true
    return 42
}

class C {
    fun bar() = 117
}

// FILE: main.kt
import kotlin.test.*

fun box(): String {
    C().bar()
    assertFalse(z)

    return "OK"
}
