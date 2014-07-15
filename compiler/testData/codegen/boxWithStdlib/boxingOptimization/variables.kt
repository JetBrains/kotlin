
import kotlin.test.assertEquals

inline fun <R, T> foo(x : R, block : (R) -> T) : T {
    var y = x
    var z = y
    z = x
    return block(z)
}

fun box() : String {
    assertEquals(1, foo(1) { x -> x })
    assertEquals(1f, foo(1f) { x -> x })
    assertEquals(1L, foo(1L) { x -> x })
    assertEquals(1.toDouble(), foo(1.toDouble()) { x -> x })
    assertEquals(1.toShort(), foo(1.toShort()) { x -> x })
    assertEquals(1.toByte(), foo(1.toByte()) { x -> x })
    assertEquals('a', foo('a') { x -> x })
    assertEquals(true, foo(true) { x -> x })

    return "OK"
}
