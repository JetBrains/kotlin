package foo
import kotlin.test.*

// FUNCTION_CALLED_TIMES: abs count=1
// ^ This single call is in the standard library, not here

inline fun abs(a: Int): Int {
    if (a < 0) {
        return a * -1
    } else {
        return a
    }
}

val r1 = abs(1)
val r2 = abs(-2)
val r3 = abs(3)
val r4 = abs(-4)

fun box(): String {
    assertEquals(1, r1)
    assertEquals(2, r2)
    assertEquals(3, r3)
    assertEquals(4, r4)

    return "OK"
}
