import kotlin.test.assertEquals

inline fun <R> foo(x : R, block : (R) -> R) : R {
    return block(x)
}

fun box() : String {
    val result = foo(1) { x -> x + 1 }
    assertEquals(2, result)

    return "OK"
}
