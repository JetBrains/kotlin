// WITH_STDLIB
import kotlin.test.*

object A {
    val a = 42
    val b = A.a
}

fun box(): String {
    assertEquals(42, A.b)

    return "OK"
}
