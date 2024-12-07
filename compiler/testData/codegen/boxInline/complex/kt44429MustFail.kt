// TARGET_BACKEND: JVM
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
package test

// Following examples expected to fail
inline fun <T : Int> takeTFail(t: T) {}
inline fun <T : Int, U : T> takeUSuperInt(u: U) {}

// FILE: 2.kt
import test.*

inline fun <reified T> assertThrows(block: () -> Unit) {
    try {
        block.invoke()
    } catch (t: Throwable) {
        if (t is T) return
        throw t
    }
    throw AssertionError("Exception was expected")
}

fun box(): String {
    val f = { null } as () -> Int

    assertThrows<NullPointerException> { takeTFail(f()) }
    assertThrows<NullPointerException> { takeUSuperInt(f()) }
    return "OK"
}
