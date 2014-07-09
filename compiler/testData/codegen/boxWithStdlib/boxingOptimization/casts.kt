
import kotlin.test.assertEquals

inline fun <R, T> foo(x : R?, block : (R?) -> T) : T {
    return block(x)
}

fun box() : String {
    assertEquals(1L, foo(1) { x -> x!!.toLong() })
    assertEquals(1.toShort(), foo(1) { x -> x!!.toShort() })
    assertEquals(1.toByte(), foo(1L) { x -> x!!.toByte() })
    assertEquals(1.toShort(), foo(1L) { x -> x!!.toShort() })
    assertEquals('a'.toDouble(), foo('a') { x -> x!!.toDouble() })
    assertEquals(1.0.toByte(), foo(1.0) { x -> x!!.toByte() })

    return "OK"
}
