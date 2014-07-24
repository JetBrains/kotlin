
import kotlin.test.assertEquals

inline fun <R, T> foo(x : R, y : R, block : (R) -> T) : T {
    val a = x is Number
    val b = x is Object

    val b1 = x as Object

    if (a && b) {
        return block(x)
    } else {
        return block(y)
    }
}

fun box() : String {
    assertEquals(1, foo(1, 2) { x -> x as Int })
    assertEquals("def", foo("abc", "def") { x -> x as String })

    return "OK"
}
